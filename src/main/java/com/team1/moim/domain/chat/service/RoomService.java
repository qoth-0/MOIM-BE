package com.team1.moim.domain.chat.service;

import com.team1.moim.domain.chat.dto.response.RoomDetailResponse;
import com.team1.moim.domain.chat.entity.MemberRoom;
import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.chat.repository.MemberRoomRepository;
import com.team1.moim.domain.chat.repository.RoomRepository;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final MemberRoomRepository memberRoomRepository;

//    public RoomDetailResponse create(
//            RoomRequest roomRequest,
//            List<MemberRoomRequest> memberRoomRequests) throws JsonProcessingException {
//
//        Member host = findMemberByEmail();
//
//        // 참여자 정보
//        if (memberRoomRequests == null || memberRoomRequests.isEmpty()) {
//            throw new NoChatParticipantsException();
//        }
//
//        // 참여자 리스트에 호스트가 포함되어 있는지 검사
//        for (MemberRoomRequest request : memberRoomRequests) {
//            if (request.getMemberEmail().equals(host.getEmail())) {
//                throw new HostChatIncludedException();
//            }
//        }
//
//        Room newRoom = roomRequest.toEntity(host, memberRoomRequests);
//
//    }

    // HostEmail을 포함하는 RoomDetailResponse 조립
    public RoomDetailResponse getRoomDetail(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        MemberRoom hostMemberRoom = memberRoomRepository.findHostByRoomId(roomId);
        String hostEmail = hostMemberRoom.getMember().getEmail();
        return RoomDetailResponse.builder()
                .id(room.getId())
                .title(room.getTitle())
                .deleteDateTime(room.getDeleteDate())
                .hostEmail(hostEmail)
                .build();
    }

    // 이메일로 회원 찾기
    private Member findMemberByEmail() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);
    }
}
