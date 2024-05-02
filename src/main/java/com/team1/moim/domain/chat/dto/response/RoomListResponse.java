package com.team1.moim.domain.chat.dto.response;

import com.team1.moim.domain.chat.entity.Room;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RoomListResponse {
    private Long id;
    private String title; // 채팅룸 이름
    private LocalDateTime deleteDateTime; // 채팅룸 삭제 시간
    private String hostEmail; // 호스트 정보
    private String hostNickName;
    private int participants; // 참여자 수
    // memberRooms 정보
//    private String guestEmail;
//    private String guestNickname;
//    private boolean guestIsFirstEnter;
//    private LocalDateTime guestLeaveDate;
//    private Long memberRoomId
    private List<String[]> memberRooms;


    public static RoomListResponse from(Room room, List<String[]> memberRooms) {
        return RoomListResponse.builder()
                .id(room.getId())
                .hostEmail(room.getMember().getEmail())
                .hostNickName(room.getMember().getNickname())
                .title(room.getTitle())
                .deleteDateTime(room.getDeleteDate())
                .participants(room.getParticipants())
                .memberRooms(memberRooms)
                .build();
    }
}
