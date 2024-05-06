package com.team1.moim.domain.chat.service;

import com.team1.moim.domain.chat.dto.request.ChatRequest;
import com.team1.moim.domain.chat.dto.response.ChatResponse;
import com.team1.moim.domain.chat.entity.Chat;
import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.chat.exception.RoomNotFoundException;
import com.team1.moim.domain.chat.repository.ChatRepository;
import com.team1.moim.domain.chat.repository.RoomRepository;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.stream.Collectors;
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
    private final MemberRepository memberRepository;

    @Transactional
    public ChatResponse save(ChatRequest chatRequest) {
        Member member = memberRepository.findByNickname(chatRequest.sender()).orElseThrow(MemberNotFoundException::new);
        Room room = roomRepository.findById(chatRequest.room()).orElseThrow(RoomNotFoundException::new);
        Chat chat = Chat.builder()
                .content(chatRequest.content())
                .member(member)
                .room(room)
                .type(chatRequest.type())
                .build();
        chatRepository.save(chat);
        return ChatResponse.from(chat);
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getMessages(Long roomId) {
        List<Chat> chats = chatRepository.findAllByRoomId(roomId);
        return chats.stream()
                .map(ChatResponse::from)
                .collect(Collectors.toList());
    }

}
