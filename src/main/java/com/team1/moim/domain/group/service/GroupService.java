package com.team1.moim.domain.group.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team1.moim.domain.event.dto.response.AvailableResponse;
import com.team1.moim.domain.event.entity.Event;
import com.team1.moim.domain.event.repository.EventRepository;
import com.team1.moim.domain.group.dto.request.GroupAlarmRequest;
import com.team1.moim.domain.group.dto.request.GroupInfoRequest;
import com.team1.moim.domain.group.dto.request.GroupRequest;
import com.team1.moim.domain.group.dto.response.*;
import com.team1.moim.domain.group.entity.*;
import com.team1.moim.domain.group.exception.*;
import com.team1.moim.domain.group.repository.GroupAlarmRepository;
import com.team1.moim.domain.group.repository.GroupInfoRepository;
import com.team1.moim.domain.group.repository.GroupRepository;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.GroupInfoNotFoundException;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.repository.MemberRepository;
import com.team1.moim.domain.notification.NotificationType;
import com.team1.moim.global.config.redis.RedisService;
import com.team1.moim.global.config.s3.S3Service;
import com.team1.moim.domain.notification.dto.GroupNotification;
import com.team1.moim.global.config.sse.service.SseService;

import java.time.ZoneId;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupAlarmRepository groupAlarmRepository;
    private final GroupInfoRepository groupInfoRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final S3Service s3Service;
    private final SseService sseService;
    private final RedisService redisService;

    // 모임 생성하기
    @Transactional
    public GroupDetailResponse create(
            GroupRequest groupRequest,
            List<GroupInfoRequest> groupInfoRequests,
            List<GroupAlarmRequest> groupAlarmRequests) throws JsonProcessingException {

        Member host = findMemberByEmail();

        // 참여자 정보
        if (groupInfoRequests == null || groupInfoRequests.isEmpty()) {
            throw new ParticipantRequiredException();
        }

        // 참여자 리스트에 호스트가 포함되어 있는지 검사
        for (GroupInfoRequest request : groupInfoRequests) {
            if (request.getMemberEmail().equals(host.getEmail())) {
                throw new HostIncludedException();
            }
        }

        Group newGroup = groupRequest.toEntity(host, groupInfoRequests, GroupType.GROUP_CREATE);
        for (GroupInfoRequest groupInfoRequest : groupInfoRequests) {
            log.info("참여자 존재여부 확인");
            Member participant = memberRepository.findByEmail(groupInfoRequest.getMemberEmail())
                    .orElseThrow(MemberNotFoundException::new);
            log.info("참여자 이메일: {}", participant.getEmail());
            GroupInfo groupInfo = groupInfoRequest.toEntity(participant);
            groupInfo.attachGroup(newGroup);
        }

        // 첨부파일
        String filePath = null;
        if (groupRequest.getFilePath() != null) {
            log.info("S3에 이미지 업로드: {}", filePath);
            filePath = s3Service.uploadFile("groups", groupRequest.getFilePath());
        }
        newGroup.setFilePath(filePath);

        // Deadline 임박에 대한 알림 추가(여러 개의 알림 등록 가능)
        if (groupAlarmRequests != null) {
            log.info("모임 알람 설정 진입");
            for (GroupAlarmRequest groupAlarmRequest : groupAlarmRequests) {
                GroupAlarmTimeType groupAlarmTimeType;
                if (groupAlarmRequest.getAlarmTimeType().equals("MIN")) {
                    log.info("모임 알람 분 설정");
                    groupAlarmTimeType = GroupAlarmTimeType.MIN;
                } else if (groupAlarmRequest.getAlarmTimeType().equals("HOUR")) {
                    groupAlarmTimeType = GroupAlarmTimeType.HOUR;
                } else {
                    groupAlarmTimeType = GroupAlarmTimeType.DAY;
                }
                GroupAlarm newGroupAlarm = groupAlarmRequest.toEntity(groupAlarmTimeType);
                newGroupAlarm.attachGroup(newGroup);
            }
        }

        groupRepository.save(newGroup);

        // 모임 생성 완료와 동시에 참여자들에게 알림 전송
        String hostname = host.getNickname();
        String groupTitle = newGroup.getTitle();
        String message = String.format("%s님이 \"%s\" 모임에 초대했습니다. 참여하시겠습니까?", hostname, groupTitle);
        log.info("메시지 내용 확인: " + message);

        List<GroupInfo> groupInfos = groupInfoRepository.findByGroup(newGroup);
        for (GroupInfo groupInfo : groupInfos) {
            String participantEmail = groupInfo.getMember().getEmail();
            log.info("참여자 이메일 주소: " + participantEmail);
            sseService.sendGroupNotification(participantEmail,
                    GroupNotification.from(newGroup, message, NotificationType.GROUP_CREATE, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
        }

        return GroupDetailResponse.from(newGroup);
    }

    // 그룹 알림 전송 스케줄러
    // 모임 참여 결정에 대한 마감 시간 알림을 제공한다.
    @Transactional
    @Scheduled(cron = "0 0/1 * * * *")
    public void scheduleGroupAlarm() throws JsonProcessingException {
        // 확정되지 않은 그룹 리스트를 검색
        List<Group> groups = groupRepository.findByIsConfirmed("N");
        for (Group group : groups) {

            // 마감 시간이 도달한 그룹 확정 및 알림 전송
            if(group.getVoteDeadline().isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul")))) {

                List<GroupInfo> participants =
                        groupInfoRepository.findByGroupAndIsAgreed(group, "Y");

                String message;
                String groupTitle = group.getTitle();

                // 참여자가 없을 경우 호스트에게만 취소 알림
                if (participants.isEmpty()) {
                    message = groupTitle + " 모임이 참여자가 없어 취소되었습니다.";

                    // Group 확정 및 삭제 처리
                    group.confirm();
                    group.delete();
                    group.updateGroupType(GroupType.GROUP_CANCEL);

                    groupRepository.save(group);

                    // 호스트 알림 발송
                    sseService.sendGroupNotification(group.getMember().getEmail(),
                            GroupNotification.from(group, message, NotificationType.GROUP_CANCEL, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));

                } else {
                    // 참여자가 있을 경우

                    // 모임 일정 자동 추천 로직 실행 후 추천 일정 리스트 가져오기
                    List<LocalDateTime> recommendEvents = recommendGroupSchedule(group);
                    log.info("추천 일정: " + recommendEvents);
                    if (redisService.getAvailableList(String.valueOf(group.getId())).isEmpty()) {
                        recommendEvents.forEach(recommendEvent -> {
                            try {
                                redisService.setAvailableList(group.getId().toString(), recommendEvent);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    log.info("추천일정 redis 저장완료");

                    // 모일 수 있는 시간이 없다면, 모두에게 모일 수 없다는 알림 발송
                    if (recommendEvents.isEmpty()) {
                        message = groupTitle + " 모임이 가능한 일정이 없어 취소되었습니다.";

                        // Group 확정 및 삭제 처리
                        group.confirm();
                        group.delete();
                        group.updateGroupType(GroupType.GROUP_CANCEL);

                        groupRepository.save(group);

                        // 호스트도 알림 발송
                        sseService.sendGroupNotification(group.getMember().getEmail(),
                                GroupNotification.from(group, message, NotificationType.GROUP_CANCEL, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));

                        for (GroupInfo agreedParticipant : participants) {
                            sseService.sendGroupNotification(agreedParticipant.getMember().getEmail(),
                                    GroupNotification.from(group, message, NotificationType.GROUP_CANCEL, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                        }

                        // 모일 수 있는 일정이 1개라면 자동으로 모임을 확정 짓고 모두에게 알림 전송
                    } else if (recommendEvents.size() == 1) {
                        message = groupTitle + " 모임이 확정 되었습니다. 일정을 확인해보세요!";

                        // Group 확정 처리
                        group.confirm();
                        group.setConfirmedDateTime(recommendEvents.get(0));
                        groupRepository.save(group);
                        group.updateGroupType(GroupType.GROUP_CONFIRM);

                        // 호스트도 알림 발송
                        sseService.sendGroupNotification(group.getMember().getEmail(),
                                GroupNotification.from(group, message, NotificationType.GROUP_CONFIRM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));

                        for (GroupInfo agreedParticipant : participants) {
                            sseService.sendGroupNotification(agreedParticipant.getMember().getEmail(),
                                    GroupNotification.from(group, message, NotificationType.GROUP_CONFIRM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                        }
                        // 추천 일정이 여러개라면 모임 확정 알림을 호스트 에게만 전송
                    } else {
                        message = groupTitle + " 모임을 확정해주세요.";
                        group.updateGroupType(GroupType.GROUP_CHOICE);
                        // 호스트한테만 알림 발송
                        sseService.sendGroupNotification(group.getMember().getEmail(),
                                GroupNotification.from(group, message, NotificationType.GROUP_CHOICE, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                    }
                }
            }

            // 모임 알림 중에서 스케줄러가 필요한 알림은 데드라인 마감 알림 밖에 없다.
            // 그 중에서 아직 알림을 보내지 않은 것을 선택한다.
            List<GroupAlarm> groupAlarms = groupAlarmRepository
                    .findByGroupAndSendYn(group, "N");

            for (GroupAlarm groupAlarm : groupAlarms) {
                if (groupAlarm.getGroupAlarmTimeType() == GroupAlarmTimeType.DAY
                        && groupAlarm.getGroup().getVoteDeadline().minusDays(
                                groupAlarm.getDeadlineAlarm()).isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul")))){
                    sendAlarmForParticipants(groupAlarm);
                    return;
                }

                if (groupAlarm.getGroupAlarmTimeType() == GroupAlarmTimeType.HOUR
                        && groupAlarm.getGroup().getVoteDeadline().minusHours(
                                groupAlarm.getDeadlineAlarm()).isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul")))) {
                    sendAlarmForParticipants(groupAlarm);

                    return;
                }

                if (groupAlarm.getGroupAlarmTimeType() == GroupAlarmTimeType.MIN
                        && groupAlarm.getGroup().getVoteDeadline().minusMinutes(
                                groupAlarm.getDeadlineAlarm()).isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul")))) {
                    sendAlarmForParticipants(groupAlarm);

                    return;
                }
            }
        }
    }

    // 모임 삭제
    @Transactional
    public void delete(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(GroupNotFoundException::new);
        group.delete();
        group.updateGroupType(GroupType.GROUP_CANCEL);
        group.confirm();
        for (GroupInfo groupInfo : groupInfoRepository.findByGroup(group)) {
            groupInfo.delete();
        }
    }

    // 모임 조회(일정 확정 전)
    @Transactional
    public FindPendingGroupResponse findPendingGroup(Long id) {
        Group pendingGroup = groupRepository.findByIsConfirmedAndIsDeletedAndId("N", "N", id)
                .orElseThrow(GroupNotFoundException::new);
        return FindPendingGroupResponse.from(pendingGroup);
    }

    // 모임 조회(일정 확정 후)
    @Transactional
    public FindConfirmedGroupResponse findConfirmedGroup(Long id) {
        Group confirmedGroup = groupRepository.findByIsConfirmedAndIsDeletedAndId("Y", "N", id)
                .orElseThrow(GroupNotFoundException::new);
        return FindConfirmedGroupResponse.from(confirmedGroup);
    }

    // 전체 모임 조회하기
    @Transactional
    public List<ListGroupResponse> findGroups(int pageNum) {
//       1페이지당 나오는 갯수
        int size = 6;
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        List<GroupInfo> guestGroupInfo = groupInfoRepository.findByMemberId(member.getId()); // 자기가 게스트인 그룹의 인포
        List<Group> groups = groupRepository.findByMemberId(member.getId()); // 자기가 호스트인 그룹

        for (GroupInfo groupInfo : guestGroupInfo){
            groups.add(groupInfo.getGroup());
        }
        // 자기가 속한 모든 그룹 = groups

        List<ListGroupResponse> groupResponse = new ArrayList<>();


        for(Group group: groups){ // 자기가 속한 모든 그룹의 정보를 추출
            List<String[]> guestEmailNicknameIsAgreed = new ArrayList<>(); // 각 그룹의 게스트 이메일, 닉네임, 동의여부
            List<GroupInfo> tempGroupInfos = groupInfoRepository.findByGroup(group);

            for(GroupInfo groupInfo : tempGroupInfos){
                guestEmailNicknameIsAgreed.add(new String[]{groupInfo.getMember().getEmail(),
                                                            groupInfo.getMember().getNickname(),
                                                            groupInfo.getIsAgreed(),
                                                            String.valueOf(groupInfo.getId()),
                                                            groupInfo.getIsAddEvent()});

            }
            groupResponse.add(ListGroupResponse.from(group, guestEmailNicknameIsAgreed));
        }
        // 정렬 (Group ID에 따라 내림차순)
        Collections.sort(groupResponse, (a, b) -> b.getId().compareTo(a.getId()));

        // 페이징 처리
        int totalItems = groupResponse.size();
        int fromIndex = (pageNum - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        if (fromIndex >= totalItems) {
            return new ArrayList<>(); // 요청된 페이지 번호가 가지고 있는 아이템 수보다 많은 경우 빈 리스트 반환
        } else if (fromIndex < 0) {
            throw new IllegalArgumentException("Page number should be positive.");
        }

        return new ArrayList<>(groupResponse.subList(fromIndex, toIndex));

//        return groupResponse;
//        Page<Group> groups = groupRepository.findAllByMemberId(member.getId(), pageable);

        // Page를 List로 변환
//        return groups.map(this::convertToListGroupResponse).getContent();
    }

    @Transactional
    public VoteResponse vote(Long groupId, Long groupInfoId, String agreeYn) throws JsonProcessingException {
        Member findParticipant = findMemberByEmail();
        Group findGroup = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        GroupInfo findGroupInfo =
                groupInfoRepository.findById(groupInfoId).orElseThrow(GroupInfoNotFoundException::new);

        // 로직 실행 전 이것 저것 검증(함수 내부에 검증 내용 주석으로 달아 놓음)
        validate(findParticipant, findGroup, findGroupInfo);

        // 수락 또는 거절 투표
        findGroupInfo.vote(agreeYn);

        // 변경 사항(동의 여부) 반영
        GroupInfo savedGroupInfo = groupInfoRepository.save(findGroupInfo);

        Group updatedGroup = savedGroupInfo.getGroup();

        if (checkVoteStatus(updatedGroup)){
            log.info("모임 참여자 전원 투표 완료!");

            // 모임 일정 자동 추천 로직 실행 후 추천 일정 리스트 가져오기
            List<LocalDateTime> recommendEvents = recommendGroupSchedule(savedGroupInfo.getGroup());
            log.info("추천 일정: " + recommendEvents);
            if (redisService.getAvailableList(groupId.toString()).isEmpty()) {
                recommendEvents.forEach(recommendEvent -> {

                    try {
                        List<AvailableResponse> existingEvents = redisService.getAvailableList(groupId.toString());

                        // 추천된 이벤트가 리스트에 이미 있는지 확인합니다
                        if (!existingEvents.contains(recommendEvent)) {
                            // 이벤트가 없을 경우 Redis에 추가합니다
                            redisService.setAvailableList(groupId.toString(), recommendEvent);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            log.info("추천일정 redis 저장완료");


            // 모임을 수락한 참여자 리스트 가져오기
            List<GroupInfo> agreedParticipants =
                    groupInfoRepository.findByGroupAndIsAgreed(savedGroupInfo.getGroup(), "Y");

            String groupTitle = savedGroupInfo.getGroup().getTitle();
            String message;

            // 모임을 모두 거절한 경우
            if(agreedParticipants.isEmpty()) {
                message = groupTitle + " 모임이 참여자가 없어 취소되었습니다.";

                // Group 확정 및 삭제 처리
                updatedGroup.confirm();
                updatedGroup.delete();
                updatedGroup.updateGroupType(GroupType.GROUP_CANCEL);

                groupRepository.save(updatedGroup);

                // 호스트 알림 발송
                sseService.sendGroupNotification(updatedGroup.getMember().getEmail(),
                        GroupNotification.from(updatedGroup, message, NotificationType.GROUP_CANCEL, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
            }


            // 모일 수 있는 시간이 없다면, 모두에게 모일 수 없다는 알림 발송
            else if (recommendEvents.isEmpty()){
                message = groupTitle + " 모임이 가능한 일정이 없어 취소되었습니다.";

                // Group 확정 및 삭제 처리
                updatedGroup.confirm();
                updatedGroup.delete();
                updatedGroup.updateGroupType(GroupType.GROUP_CANCEL);

                groupRepository.save(updatedGroup);

                // 호스트도 알림 발송
                sseService.sendGroupNotification(updatedGroup.getMember().getEmail(),
                        GroupNotification.from(updatedGroup, message, NotificationType.GROUP_CANCEL, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));

                for (GroupInfo agreedParticipant : agreedParticipants){
                    sseService.sendGroupNotification(agreedParticipant.getMember().getEmail(),
                            GroupNotification.from(updatedGroup, message, NotificationType.GROUP_CANCEL, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                }

                // 모일 수 있는 일정이 1개라면 자동으로 모임을 확정 짓고 모두에게 알림 전송
            } else if (recommendEvents.size() == 1){
                message = groupTitle + " 모임이 확정 되었습니다. 일정을 확인해보세요!";

                // Group 확정 처리
                updatedGroup.confirm();
                updatedGroup.setConfirmedDateTime(recommendEvents.get(0));
                updatedGroup.updateGroupType(GroupType.GROUP_CONFIRM);
                groupRepository.save(updatedGroup);

                // 호스트도 알림 발송
                sseService.sendGroupNotification(updatedGroup.getMember().getEmail(),
                        GroupNotification.from(updatedGroup, message, NotificationType.GROUP_CONFIRM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));

                for (GroupInfo agreedParticipant : agreedParticipants){
                    sseService.sendGroupNotification(agreedParticipant.getMember().getEmail(),
                            GroupNotification.from(updatedGroup, message, NotificationType.GROUP_CONFIRM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                }
                // 추천 일정이 여러개라면 모임 확정 알림을 호스트 에게만 전송
            } else {
                message = groupTitle + " 모임을 확정해주세요.";
                updatedGroup.updateGroupType(GroupType.GROUP_CHOICE);
                // 호스트한테만 알림 발송
                sseService.sendGroupNotification(updatedGroup.getMember().getEmail(),
                        GroupNotification.from(updatedGroup, message, NotificationType.GROUP_CHOICE, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
            }
        }

        return VoteResponse.from(savedGroupInfo, findParticipant);
    }

    private void sendAlarmForParticipants(GroupAlarm groupAlarm) throws JsonProcessingException {
        // 알림은 아직 참여 또는 거절을 선택하지 않은 상태이고, 존재하는 유저한테만 보내야 한다.
        List<GroupInfo> participants =
                groupInfoRepository.findByGroupAndIsAgreed(groupAlarm.getGroup(), "P");

        String alarmType = "";
        if(groupAlarm.getGroupAlarmTimeType() == GroupAlarmTimeType.DAY) {
            alarmType = "일";
        }if(groupAlarm.getGroupAlarmTimeType() == GroupAlarmTimeType.HOUR) {
            alarmType = "시간";
        }if(groupAlarm.getGroupAlarmTimeType() == GroupAlarmTimeType.MIN) {
            alarmType = "분";
        }

        String message = "모임 참여 결정까지 "
                + groupAlarm.getDeadlineAlarm()
                + alarmType
                + " 남았습니다.";

        for (GroupInfo participant : participants) {
            sseService.sendGroupNotification(
                    participant.getMember().getEmail(),
                    GroupNotification.from(participant.getGroup(), message, NotificationType.GROUP_DEADLINE, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
        }
        groupAlarm.sendCheck("Y");
    }

    // 모임 참여자 전원이 투표했는 지 확인하는 메서드
    private boolean checkVoteStatus(Group group) {
        List<GroupInfo> groupInfos = groupInfoRepository.findByGroup(group);
        for (GroupInfo groupInfo : groupInfos) {
            log.info("GroupInfo agreeYn 확인");
            if (groupInfo.getIsAgreed().equals("P")){
                return false;
            }
        }
        return true;
    }

    // 일정 추천 로직
    private List<LocalDateTime> recommendGroupSchedule(Group group){

        LocalDate expectStartDate = group.getExpectStartDate();
        LocalDate expectEndDate = group.getExpectEndDate();
        LocalTime expectStartTime = group.getExpectStartTime();
        LocalTime expectEndTime = group.getExpectEndTime();
        int runningTime = group.getRunningTime();


        // 각 사용자의 불가능한 슬롯을 나타내는 리스트
        List<LocalDateTime[]> allUnavailableSlots = new ArrayList<>();
        // 각 사용자의 불가능한 슬롯을 추가(시작일자, 종료일자)
//        데이터를 가져올때 start_date와 end_date 안에있는 모든 일정 데이터를 가져와 하나하나 넣어주기 밑에는 예시 데이터/ 무조건 첫번째가 일정 시작시간, 두번째가 일정 종료시간이어야 함

        // 수락을 누른 모든 게스트의 정보를 넣기
        List<GroupInfo> groupInfoList =  groupInfoRepository.findByGroupAndIsAgreed(group, "Y");

        // 모임에 수락한 Members
        List<Member> agreedMembers = new ArrayList<>();

        // 호스트 정보 넣기
        agreedMembers.add(group.getMember());

        // 모임 참여자 정보 넣기
        for (GroupInfo groupInfo : groupInfoList){
            agreedMembers.add(groupInfo.getMember());
        }

        LocalDateTime startOfMeetingRange = expectStartDate.atStartOfDay(); // 모임 시작 날짜의 00:00
        LocalDateTime endOfMeetingRange = expectEndDate.atTime(LocalTime.MAX); // 모임 종료 날짜의 23:59:59.999999999

        for (Member member: agreedMembers) {
            // 각각의 일정 리스트를 합침
            List<Event> memberEvents = eventRepository.findByMemberAndDeleteYn(member, "N");
            for (Event event : memberEvents) {
                // 개인 일정의 시작과 종료 시간
                LocalDateTime eventStart = event.getStartDateTime();
                LocalDateTime eventEnd = event.getEndDateTime();

                // 개인 일정이 모임의 날짜 범위에 걸쳐있는지 확인
                // 개인 일정의 시작 시간이 모임 범위 안에 있거나, 개인 일정의 종료 시간이 모임 범위 안에 있는 경우
                // 또는 개인 일정이 모임 범위를 포함하는 경우 (모임 시작 전에 시작하여 모임 종료 후에 끝나는 경우)
                if ((eventStart.isBefore(endOfMeetingRange) && eventStart.isAfter(startOfMeetingRange)) ||
                        (eventEnd.isAfter(startOfMeetingRange) && eventEnd.isBefore(endOfMeetingRange)) ||
                        (eventStart.isBefore(startOfMeetingRange) && eventEnd.isAfter(endOfMeetingRange))) {
                    allUnavailableSlots.add(new LocalDateTime[]{eventStart, eventEnd});
                }
            }
        }
        log.info( "모든 이벤트 "+ allUnavailableSlots.toString());
        // 불가능한 슬롯을 합치는 메소드임(시간이 겹치는거 합쳐주는거)
        List<LocalDateTime[]> fixAllUnavailableSlots = mergeOverlappingSlots(allUnavailableSlots);

        // limit가 보여주고 싶은 갯수
        return findMeetingStartTimes(
                expectStartDate,
                expectEndDate,
                expectStartTime,
                expectEndTime,
                fixAllUnavailableSlots,
                runningTime,
                3);
    }

    private List<LocalDateTime[]> mergeOverlappingSlots(List<LocalDateTime[]> slots) {
        log.info("불가능 리스트 알고리즘 시작");
//       //비어 있으면 빈 리스트로 반환
        if (slots.isEmpty()) return Collections.emptyList();

        // 빠른 시작시간으로 정렬
        slots.sort(Comparator.comparing(slot -> slot[0]));

        // 겹치는 슬롯 저장하는 리스트 merged
        List<LocalDateTime[]> merged = new ArrayList<>();
        // 저장할 첫번째 리스트를 current
        LocalDateTime[] current = slots.get(0);

        for (int i = 1; i < slots.size(); i++) {
            //
            if (current[1].isBefore(slots.get(i)[0])) {
                // No overlap
                merged.add(current);
                current = slots.get(i);
            } else {
                // Overlap, extend the current slot
                current[1] = current[1].isAfter(slots.get(i)[1]) ? current[1] : slots.get(i)[1];
            }
        }

        merged.add(current);
        for (int i = 0; i < merged.size(); i++) {
            log.info( "불가능 시간 리스트" +  Arrays.toString(merged.get(i)));
        }
        return merged;
    }

    // limit로 반환하고 싶은 추천 일정의 갯수 지정
    private List<LocalDateTime> findMeetingStartTimes(LocalDate startDate,
                                                      LocalDate endDate,
                                                      LocalTime dailyStartTime,
                                                      LocalTime dailyEndTime,
                                                      List<LocalDateTime[]> allUnavailableSlots,
                                                      int runningTime,
                                                      int limit) {
        List<LocalDateTime> availableStartTimes = new ArrayList<>();

        // 하루씩 더해가면서 도는거
        loopAll:
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime startDateTime = LocalDateTime.of(date, dailyStartTime); // 현재 날짜의 시작 시간
            LocalDateTime endDateTime = LocalDateTime.of(date, dailyEndTime); // 현재 날짜의 마지막 시간

            // 현재 날짜의 시작 시간부터 종료 시간까지, 설정된 30분을 더해감
            while (!startDateTime.plusMinutes(runningTime).isAfter(endDateTime)) {
                LocalDateTime currentStart = startDateTime;
                LocalDateTime currentFinsh = startDateTime.plusMinutes(runningTime); // 현재 시간에서 러닝타임을 더해 끝나는 시간
                availableStartTimes.add(currentStart);
                // 불가능한 슬롯이랑 겹치는지 확인하는 로직
                for (int i = 0; i < allUnavailableSlots.size(); i++) {
//                    불가능한 시간 1개씩 담는거 temp
                    LocalDateTime[] temp = allUnavailableSlots.get(i);
//                    시간,종료시간이 불가능 시간 전이나 후에 있으면
//                    비교한는 불가능 시간의 앞에있던가 뒤에 있으면 true를 반환함
                    if(!(currentFinsh.isBefore(temp[0].plusMinutes(1)) || currentStart.isAfter(temp[1].minusMinutes(1)))){
                        availableStartTimes.remove(currentStart);
                        break;
                    }

                }
                startDateTime = startDateTime.plusMinutes(30);

                if(availableStartTimes.size() == limit){ // 3개면 모든걸 끝내기
                    break loopAll;
                }
            }
        }
        log.info("availableStartTimes = " + availableStartTimes);

        return availableStartTimes;
    }


    // 이메일로 회원 찾기
    private Member findMemberByEmail() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);
    }

    private void validate(Member participant, Group group, GroupInfo groupInfo){

        // group과 groupInfo의 존재 및 일치 여부 검증
        if (!group.equals(groupInfo.getGroup())) {
            throw new GroupAndGroupInfoNotMatchException();
        }

        // GroupInfo의 Participants와 현재 로그인 투표하는 Participant 검증
        if (!groupInfo.getMember().getEmail().equals(participant.getEmail())) {
            throw new ParticipantInfoNotMatchException();
        }

        // 이미 투표했을 경우, 예외
        if (!groupInfo.getIsAgreed().equals("P")) {
            throw new AlreadyVotedException();
        }
    }

    // redis에 저장된 추천 일정 가져오기
    public List<AvailableResponse> getAvailable(String groupId) {
        List<AvailableResponse> availableList = redisService.getAvailableList(groupId);
        return availableList;
    }

//    모임 확정
    public FindConfirmedGroupResponse confirm(Long groupId, String confirmDay) throws JsonProcessingException {
        Group group = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        group.confirm();
        group.setConfirmedDateTime(LocalDateTime.parse(confirmDay));
        group.updateGroupType(GroupType.GROUP_CONFIRM);
        log.info("확정된 날짜 " + LocalDateTime.parse(confirmDay));
        // 모임을 수락한 참여자 리스트 가져오기
        List<GroupInfo> agreedParticipants =
                groupInfoRepository.findByGroupAndIsAgreed(group, "Y");

        String message = group.getTitle() + " 모임이 확정되었습니다. 일정을 확인해보세요!";

        // 알림 발송
        sseService.sendGroupNotification(group.getMember().getEmail(),
                GroupNotification.from(group, message, NotificationType.GROUP_CONFIRM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));


        for (GroupInfo agreedParticipant : agreedParticipants){
            sseService.sendGroupNotification(agreedParticipant.getMember().getEmail(),
                    GroupNotification.from(group, message, NotificationType.GROUP_CONFIRM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
        }

        return FindConfirmedGroupResponse.from(groupRepository.save(group));

    }

    public FindConfirmedGroupResponse cancel(Long groupId) throws JsonProcessingException {
        Group group = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        group.confirm();
        group.delete();
        group.updateGroupType(GroupType.GROUP_CANCEL);
        String message = group.getTitle() + " 모임이 호스트에 의해서 취소되었습니다.";

        // 모임을 수락한 참여자 리스트 가져오기
        List<GroupInfo> agreedParticipants =
                groupInfoRepository.findByGroupAndIsAgreed(group, "Y");

        // 알림 발송
        for (GroupInfo agreedParticipant : agreedParticipants){
            sseService.sendGroupNotification(agreedParticipant.getMember().getEmail(),
                    GroupNotification.from(group, message, NotificationType.GROUP_CANCEL, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
        }
        return FindConfirmedGroupResponse.from(groupRepository.save(group));
    }

    public GroupDetailResponse findGroup(Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        return GroupDetailResponse.from(group);
    }

    // 오늘 날짜의 확정된 모임리스트 // 확정된, 내가 포함된, 오늘의 그룹을 뽑아내보자
    public List<TodayGroupResponse> todayGroup()  {
        Member member = findMemberByEmail();

        List<TodayGroupResponse> todayGroupResponses = new ArrayList<>();
        List<Group> allTodayGroup = new ArrayList<>();

        List<Group> groups =  groupRepository.findByIsConfirmedAndMember("Y",member); // 자기가 호스트인 모임 확정된 모임
        List<GroupInfo> groupInfos = groupInfoRepository.findByMemberId(member.getId());
        for (GroupInfo groupInfo : groupInfos){
            if(groupInfo.getGroup().getConfirmedDateTime() != null){
                if(groupInfo.getGroup().getConfirmedDateTime().toLocalDate().equals(LocalDate.now())){
                    allTodayGroup.add(groupInfo.getGroup());
                }
            }

        }
        for(Group group : groups){
            if(group.getConfirmedDateTime() != null) {
                if (group.getConfirmedDateTime().toLocalDate().equals(LocalDate.now())) {
                    allTodayGroup.add(group);
                }
            }
        }

        for(Group group: allTodayGroup){
            todayGroupResponses.add(TodayGroupResponse.from(group));
        }
        Collections.sort(todayGroupResponses, Comparator.comparing(TodayGroupResponse::getConfirmedDateTime));
        return todayGroupResponses;

    }

    public String addEvent(Long groupId) {
        Member member = findMemberByEmail();
        Group group = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        GroupInfo groupInfo = groupInfoRepository.findByGroupAndMember(group, member);
        groupInfo.addEvent();
        groupInfoRepository.save(groupInfo);
        return "일정 등록 여부 변경";
    }
}
