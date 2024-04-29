package com.team1.moim.domain.chat.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomDetailResponse {
    private Long id;
    private String title;   // 채팅룸 이름
    private LocalDateTime deleteDateTime;   // 채팅룸 삭제 시간
    private String hostEmail;    // 호스트 정보
}
