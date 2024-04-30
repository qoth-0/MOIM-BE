package com.team1.moim.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team1.moim.domain.chat.dto.request.MemberRoomRequest;
import com.team1.moim.domain.chat.dto.request.RoomRequest;
import com.team1.moim.domain.chat.dto.response.RoomDetailResponse;
import com.team1.moim.domain.chat.entity.MemberRoom;
import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.chat.exception.IsBeforeNowException;
import com.team1.moim.domain.chat.repository.MemberRoomRepository;
import com.team1.moim.domain.chat.repository.RoomRepository;
import com.team1.moim.domain.group.dto.request.GroupInfoRequest;
import com.team1.moim.domain.group.dto.response.GroupDetailResponse;
import com.team1.moim.domain.group.entity.Group;
import com.team1.moim.domain.group.entity.GroupInfo;
import com.team1.moim.domain.group.entity.GroupType;
import com.team1.moim.domain.group.exception.HostIncludedException;
import com.team1.moim.domain.group.exception.ParticipantRequiredException;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.repository.MemberRepository;
import com.team1.moim.domain.notification.NotificationType;
import com.team1.moim.domain.notification.dto.GroupNotification;
import com.team1.moim.domain.notification.dto.RoomNotification;
import com.team1.moim.global.config.sse.service.SseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
        String message = String.format("%s님이 \"%s\" 모임에 초대했습니다. 참여하시겠습니까?", hostname, roomTitle);
        log.info("메시지 내용 확인: " + message);

        List<MemberRoom> roomMembers = memberRoomRepository.findByRoom(room);
        for (MemberRoom roomMember : roomMembers) {
            String participantEmail = roomMember.getMember().getEmail();
            log.info("참여자 이메일 주소: " + participantEmail);
            sseService.sendRoomNotification(participantEmail,
                    RoomNotification.from(room, message, NotificationType.ROOM, LocalDateTime.now()));
        }

        return RoomDetailResponse.from(room);

    }
}
