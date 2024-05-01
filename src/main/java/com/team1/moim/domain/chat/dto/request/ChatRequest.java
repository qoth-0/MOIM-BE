package com.team1.moim.domain.chat.dto.request;

import com.team1.moim.domain.chat.entity.MessageType;

public record ChatRequest (MessageType type, String content, Long room, String sender) {
}
