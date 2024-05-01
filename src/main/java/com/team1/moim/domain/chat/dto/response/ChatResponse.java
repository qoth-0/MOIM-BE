package com.team1.moim.domain.chat.dto.response;

import com.team1.moim.domain.chat.entity.Chat;
import com.team1.moim.domain.chat.entity.MessageType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ChatResponse(
        Long chatId,
        Long userId,
        Long roomId,
        String content,
        String email,
        String nickname,
        MessageType type,
        LocalDateTime createdAt
) {
    public static ChatResponse from(Chat chat) {
        return ChatResponse.builder()
                .chatId(chat.getId())
                .userId(chat.getMember().getId())
                .roomId(chat.getRoom().getId())
                .content(chat.getContent())
                .email(chat.getMember().getEmail())
                .nickname(chat.getMember().getNickname())
                .type(chat.getType())
                .createdAt(chat.getCreatedAt())
                .build();
    }
}
