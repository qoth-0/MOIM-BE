package com.team1.moim.domain.notification.service;

import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.exception.MemberNotMatchException;
import com.team1.moim.domain.member.repository.MemberRepository;
import com.team1.moim.domain.notification.dto.NotificationResponseNew;
import com.team1.moim.domain.notification.exception.NotificationNotFoundException;
import com.team1.moim.global.config.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final MemberRepository memberRepository;
    private final RedisService redisService;

    public List<NotificationResponseNew> getAlarms() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<NotificationResponseNew> alarms= redisService.getList(email);
        if(alarms.isEmpty()) throw new NotificationNotFoundException();
        return alarms;
    }

    public String readAlarm(Long redisId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        redisService.updateReadYn(email, redisId);
        return "알림 읽음 처리 되었습니다.";
    }
}
