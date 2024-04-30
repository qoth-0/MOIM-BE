package com.team1.moim.domain.chat.controller;

import com.team1.moim.domain.chat.dto.request.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    @MessageMapping("/chat")
    @SendTo("/sub/chat")

    public ChatMessage broadcasting(ChatMessage chatMessage) {
        log.info("Chat message received : {}", chatMessage);

        return chatMessage;
    }
}
