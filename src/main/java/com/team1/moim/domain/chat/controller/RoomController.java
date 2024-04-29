package com.team1.moim.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team1.moim.domain.chat.dto.request.MemberRoomRequest;
import com.team1.moim.domain.chat.dto.request.RoomRequest;
import com.team1.moim.domain.chat.dto.response.RoomDetailResponse;
import com.team1.moim.domain.chat.service.RoomService;
import com.team1.moim.domain.member.service.MemberService;
import com.team1.moim.global.dto.ApiSuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {

    private final MemberService memberService;
    private final RoomService roomService;

    // 채팅룸 생성
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/create")
    public ResponseEntity<ApiSuccessResponse<RoomDetailResponse>> createRoom(
            HttpServletRequest httpServletRequest,
            @Valid RoomRequest roomRequest,
            @RequestPart(value = "memberRoomRequest", required = false) List<MemberRoomRequest> memberRoomRequests) throws JsonProcessingException {

//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(ApiSuccessResponse.of(
//                        HttpStatus.OK,
//                        httpServletRequest.getServletPath(),
//                        roomService.create(roomRequest, memberRoomRequests)
//                ));
        return null;
    }
}
