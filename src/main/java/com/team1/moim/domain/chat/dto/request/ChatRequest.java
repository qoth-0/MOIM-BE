package com.team1.moim.domain.chat.dto.request;

import com.team1.moim.domain.chat.entity.Chat;
import com.team1.moim.domain.chat.entity.MessageType;
import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.member.entity.Member;

public record ChatRequest (
    Member sender,
    String message,
    MessageType type
) {
    public Chat toEntity(Room room) {
        return Chat.builder()
                .member(sender)
                .content(message)
                .type(type)
                .room(room)
                .build();
    }
}
