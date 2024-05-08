package com.team1.moim.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team1.moim.domain.chat.dto.request.MemberRoomRequest;
import com.team1.moim.domain.chat.dto.request.RoomRequest;
import com.team1.moim.domain.chat.dto.response.RoomDetailResponse;
import com.team1.moim.domain.chat.dto.response.RoomListResponse;
import com.team1.moim.domain.chat.entity.MemberRoom;
import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.chat.exception.IsBeforeNowException;
import com.team1.moim.domain.chat.repository.MemberRoomRepository;
import com.team1.moim.domain.chat.repository.RoomRepository;
import com.team1.moim.domain.group.exception.HostIncludedException;
import com.team1.moim.domain.group.exception.ParticipantRequiredException;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.repository.MemberRepository;
import com.team1.moim.domain.notification.NotificationType;
import com.team1.moim.domain.notification.dto.RoomNotification;
import com.team1.moim.global.config.sse.service.SseService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final MemberRoomRepository memberRoomRepository;
    private final SseService sseService;


    // 이메일로 회원 찾기
    private Member findMemberByEmail() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);
    }

    // 채팅방 생성
    @Transactional
    public RoomDetailResponse create(RoomRequest roomRequest, List<MemberRoomRequest> memberRoomRequests) throws JsonProcessingException {

        Member host = findMemberByEmail();

        // 참여자 정보
        if (memberRoomRequests == null || memberRoomRequests.isEmpty()) {
            throw new ParticipantRequiredException();
        }

        if(LocalDateTime.parse(roomRequest.getDeleteDate()).isBefore(LocalDateTime.now())) {
            throw new IsBeforeNowException();
        }

        Room room = roomRequest.toEntity(host, memberRoomRequests);

        // 참여자 리스트에 호스트가 포함되어 있는지 검사
        for (MemberRoomRequest request : memberRoomRequests) {
            if (request.getMemberEmail().equals(host.getEmail())) {
                throw new HostIncludedException();
            }
            log.info("참여자 존재여부 확인");
            Member participant = memberRepository.findByEmail(request.getMemberEmail())
                    .orElseThrow(MemberNotFoundException::new);
            log.info("참여자 이메일: {}", participant.getEmail());
            MemberRoom memberRoom = request.toEntity(participant);
            memberRoom.attachRoom(room);
        }
        roomRepository.save(room);
        log.info("저장된 room" + room.getDeleteDate());

        // 채팅방 생성 완료와 동시에 참여자들에게 알림 전송
        String hostname = host.getNickname();
        String roomTitle = room.getTitle();
        String message = String.format("%s님이 \"%s\" 채팅방에 초대했습니다.", hostname, roomTitle);
        log.info("메시지 내용 확인: " + message);

        List<MemberRoom> roomMembers = memberRoomRepository.findByRoom(room);
        for (MemberRoom roomMember : roomMembers) {
            String participantEmail = roomMember.getMember().getEmail();
            log.info("참여자 이메일 주소: " + participantEmail);
            sseService.sendRoomNotification(participantEmail,
                    RoomNotification.from(room, message, NotificationType.ROOM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
        }

        return RoomDetailResponse.from(room);

    }

    public List<RoomListResponse> findAllRoom(int pageNum) {
        // 1페이지당 나오는 갯수
        int size = 7;
        Member member = findMemberByEmail();
        List<Room> rooms = new ArrayList<>();
        List<MemberRoom> memberRooms = new ArrayList<>();
        memberRooms = memberRoomRepository.findByMember(member); // 자기가 게스트인 그룹의 인포
        rooms = roomRepository.findByMemberAndDeleteYn(member, "N"); // 자기가 호스트인 그룹

        for (MemberRoom memberRoom : memberRooms) {
            if(memberRoom.getRoom().getDeleteYn().equals("N")){
                rooms.add(memberRoom.getRoom());
            }
        }
        // 자기가 속한 모든 채팅방 = rooms
        List<RoomListResponse> roomListResponses = new ArrayList<>();

        //RoomListResponse 로 변환
        for (Room room : rooms) {
            List<MemberRoom> memberRoom = memberRoomRepository.findByRoom(room);
            List<String[]> tempMemeberRoom = new ArrayList<>();
            for (MemberRoom memberRoom1 : memberRoom) {
                tempMemeberRoom.add(new String[]{memberRoom1.getMember().getEmail(),
                        memberRoom1.getMember().getNickname(),
                        String.valueOf(memberRoom1.getLeaveDate()),
                        String.valueOf(memberRoom1.getId())});

            }

            roomListResponses.add(RoomListResponse.from(room, tempMemeberRoom));
        }
        // 정렬 id에 따라 내림 차순
        Collections.sort(roomListResponses, (a, b) -> b.getId().compareTo(a.getId()));

        // 페이징 처리
        int totalItems = roomListResponses.size();
        int fromIndex = (pageNum - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        if (fromIndex >= totalItems) {
            return new ArrayList<>(); // 요청된 페이지 번호가 가지고 있는 아이템 수보다 많은 경우 빈 리스트 반환
        } else if (fromIndex < 0) {
            throw new IllegalArgumentException("Page number should be positive.");
        }

        return new ArrayList<>(roomListResponses.subList(fromIndex, toIndex));
    }

    @Transactional
    @Scheduled(cron = "0 0/1 * * * *")
    public void scheduleChatingDelete() throws JsonProcessingException {
        // 삭제 되지 않은 모든 모임을 조회
        List<Room> rooms = roomRepository.findByDeleteYn("N");

        for(Room room: rooms){
            if(room.getDeleteDate().isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul")))){
                String roomTitle = room.getTitle();
                String message = String.format("\"%s\" 채팅방이 종료 되었습니다." ,roomTitle);
                log.info("메시지 내용 확인: " + message);

                // 모든 게스트에게 알람
                List<MemberRoom> roomMembers = memberRoomRepository.findByRoom(room);
                for (MemberRoom roomMember : roomMembers) {
                    String participantEmail = roomMember.getMember().getEmail();
                    log.info("참여자 이메일 주소: " + participantEmail);
                    sseService.sendRoomNotification(participantEmail,
                            RoomNotification.from(room, message, NotificationType.ROOM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                }
                // 호스트에게도 알람
                sseService.sendRoomNotification(room.getMember().getEmail(),
                        RoomNotification.from(room, message, NotificationType.ROOM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                room.delete();
            }
            // 채팅 마감시간 10분전 알림 보내기
            if(room.getDeleteDate().minusMinutes(10).isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul"))) && room.getDeleteDate().minusMinutes(9).isAfter(LocalDateTime.now(ZoneId.of("Asia/Seoul")))){
                // 채팅방 마감 전 채팅에 참여한 모든 사람들에게

                String roomTitle = room.getTitle();
                String message = String.format("\"%s\" 채팅방 종료 10분 전 입니다." , roomTitle);
                log.info("메시지 내용 확인: " + message);

                // 모든 게스트에게 알람
                List<MemberRoom> roomMembers = memberRoomRepository.findByRoom(room);
                for (MemberRoom roomMember : roomMembers) {
                    String participantEmail = roomMember.getMember().getEmail();
                    log.info("참여자 이메일 주소: " + participantEmail);
                    sseService.sendRoomNotification(participantEmail,
                            RoomNotification.from(room, message, NotificationType.ROOM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
                }
                // 호스트에게도 알람
                sseService.sendRoomNotification(room.getMember().getEmail(),
                        RoomNotification.from(room, message, NotificationType.ROOM, LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
            }
        }



    }
}