package com.team1.moim.domain.event.controller;

import com.team1.moim.domain.event.dto.request.AlarmRequest;
import com.team1.moim.domain.event.dto.request.EventRequest;
import com.team1.moim.domain.event.dto.request.RepeatRequest;
import com.team1.moim.domain.event.dto.request.ToDoListRequest;
import com.team1.moim.domain.event.dto.response.EventResponse;
import com.team1.moim.domain.event.service.EventService;
import com.team1.moim.global.dto.ApiSuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // 일정 등록
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<EventResponse>> create(HttpServletRequest servRequest,
                                                                    @Valid EventRequest request,
                                                                    @RequestPart(value = "toDoListRequests", required = false) List<ToDoListRequest> toDoListRequests,
                                                                    @RequestPart(value = "repeat", required = false) RepeatRequest repeatValue,
                                                                    @RequestPart(value = "alarmRequest", required = false) List<AlarmRequest> alarmRequest) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiSuccessResponse.of(
                        HttpStatus.OK,
                        servRequest.getServletPath(),
                        eventService.create(request, toDoListRequests, repeatValue, alarmRequest)));
    }

    // 일정 수정
    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping("/{eventId}")
    public ResponseEntity<ApiSuccessResponse<EventResponse>> update(HttpServletRequest servRequest,
                                                                    @PathVariable(name = "eventId") Long eventId,
                                                                    @Valid EventRequest request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiSuccessResponse.of(
                        HttpStatus.OK,
                        servRequest.getServletPath(),
                        eventService.update(eventId, request)));
    }

    // 일정 삭제
    @PreAuthorize("hasRole('ROLE_USER')")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiSuccessResponse<String>> delete(HttpServletRequest servRequest,
                                                             @PathVariable(name = "eventId") Long eventId) {
        eventService.delete(eventId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiSuccessResponse.of(
                        HttpStatus.OK,
                        servRequest.getServletPath(),
                        ("삭제되었습니다.")));
    }

//   반복일정의 삭제
    @PreAuthorize("hasRole('ROLE_USER')")
    @DeleteMapping("/repeat/{eventId}")
    public ResponseEntity<ApiSuccessResponse<String>> deleteRepeat(HttpServletRequest servRequest,
                                                                   @PathVariable(name = "eventId") Long eventId,
                                                                   @RequestParam("deleteType") String deleteType) {
        eventService.repeatDelete(eventId, deleteType);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiSuccessResponse.of(
                        HttpStatus.OK,
                        servRequest.getServletPath(),
                        ("삭제되었습니다.")));
    }
}
