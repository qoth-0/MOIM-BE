package com.team1.moim.domain.chat.dto.request;

import com.team1.moim.domain.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    /**
     * 채팅 동작을 확인하기 위한 임시 DTO
     */
    private MessageType type;
    private String content;
    private String sender;
    private int roomId;
}
