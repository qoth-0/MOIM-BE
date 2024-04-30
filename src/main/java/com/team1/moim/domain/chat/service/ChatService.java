package com.team1.moim.domain.chat.service;

import com.team1.moim.domain.chat.dto.request.ChatRequest;
import com.team1.moim.domain.chat.dto.response.ChatResponse;
import com.team1.moim.domain.chat.entity.Chat;
import com.team1.moim.domain.chat.repository.ChatRepository;
import com.team1.moim.domain.chat.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final RoomRepository roomRepository;

//    @Transactional
//    public ChatResponse save(Long roomId, ChatRequest chatRequest) {
//        Room room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new IllegalArgumentException(roomId + "에 해당되는 채팅룸이 없습니다."));
//        Chat chat = chatRepository.save(chatRequest.toEntity(room));
//        return ChatResponse.from(chat);
//    }
//    @Transactional
//    public ChatResponse save(ChatRequest chatRequest) {
//        Chat chat = chatRepository.save(chatRequest.toEntity(room));
//        return ChatResponse.from(chat);
//    }

    @Transactional
    public ChatResponse save(ChatRequest chatRequest) {
        Chat chat = chatRepository.save(chatRequest.toEntity());
        return ChatResponse.from(chat);
    }

}
