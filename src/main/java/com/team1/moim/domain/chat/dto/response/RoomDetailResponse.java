package com.team1.moim.domain.chat.dto.response;

import java.time.LocalDateTime;

import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.group.dto.response.GroupDetailResponse;
import com.team1.moim.domain.group.entity.Group;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomDetailResponse {
    private Long id;
    private String title; // 채팅룸 이름
    private String memo;
    private LocalDateTime deleteDateTime; // 채팅룸 삭제 시간
    private String hostEmail; // 호스트 정보
    private int participants; // 참여자 수

    public static RoomDetailResponse from(Room room) {
        return RoomDetailResponse.builder()
                .id(room.getId())
                .hostEmail(room.getMember().getEmail())
                .title(room.getTitle())
                .memo(room.getMemo())
                .deleteDateTime(room.getDeleteDate())
                .participants(room.getParticipants())
                .build();
    }
}
