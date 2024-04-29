package com.team1.moim.domain.chat.dto.response;

import com.team1.moim.domain.chat.entity.Chat;
import com.team1.moim.domain.chat.entity.MessageType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ChatResponse(
        Long chatId,
        Long userId,
        String username,
        MessageType type,
        LocalDateTime createdAt,
        String content
) {
    public static ChatResponse from(Chat chat) {
        return ChatResponse.builder()
                .chatId(chat.getId())
                .userId(chat.getMember().getId())
                .username(chat.getMember().getNickname())
                .type(chat.getType())
                .createdAt(chat.getCreatedAt())
                .content(chat.getContent())
                .build();
    }
}
