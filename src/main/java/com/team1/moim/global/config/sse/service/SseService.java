package com.team1.moim.global.config.sse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team1.moim.domain.notification.dto.RoomNotification;
import com.team1.moim.global.config.redis.RedisService;
import com.team1.moim.domain.notification.dto.GroupNotification;
import com.team1.moim.domain.notification.dto.EventNotification;
import com.team1.moim.global.config.sse.repository.EmitterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;

@Component
@Slf4j
public class SseService {

    private static final Long TIMEOUT = 60 * 60 * 1000L; // 1시간

    private final EmitterRepository emitterRepository;
    private final RedisService redisService;

    @Autowired
    public SseService(EmitterRepository emitterRepository, RedisService redisService) {
        this.emitterRepository = emitterRepository;
        this.redisService = redisService;
    }

    public SseEmitter add(String email) throws ServiceUnavailableException {

        //  SSE 연결을 위해서 만료 시간이 담긴 SseEmitter 객체를 만들어 반환해야 함
        SseEmitter emitter = new SseEmitter(TIMEOUT); // 만료 시간 설정
        // 현재 저장된 emitter의 수를 조회하여 자동 삭제를 확인
//        log.info("emitter size: " + emitterRepository.getEmitterSize());
        emitterRepository.save(email,emitter);

        emitter.onCompletion(()->{
            // 만일 emitter가 만료되면 삭제
            log.info("Emitter 유효 시간이 만료된 이메일: {}", email);
            emitterRepository.deleteByEmail(email);
        });

        emitter.onTimeout(()->{
            emitterRepository.get(email).complete();
        });
        try {
            // 최초 연결시 메시지를 안 보내면 503 Service Unavailable 에러 발생
            emitter.send(SseEmitter.event().name("connect").data(email + " connected!"));
        } catch (IOException e) {
            throw new ServiceUnavailableException();
        }
        return emitter;
    }

    public void sendEventAlarm(String email, EventNotification eventNotification) throws JsonProcessingException {
        try {
            SseEmitter emitter = emitterRepository.get(email);
            log.info("emitter : " + emitter);
            if(emitter != null) {
                log.info("sse 알림 전송 전");
                emitter.send(SseEmitter.event()
                        .name("sendEventAlarm")
                        .data(eventNotification));
                log.info("sse 알림 전송 후");
            }else {
                log.error(email + " SseEmitter가 존재하지 않음");
            }
            // redis 저장
            log.info("redis 저장 시작");
            redisService.setEventList(email, eventNotification);
            log.info("reids 저장 성공");
        } catch (Exception e) {
            log.error("알림 전송 중 에러");
            redisService.setEventList(email, eventNotification);
        }
    }

    public void sendGroupNotification(String memberEmail,
                                      GroupNotification groupNotification) throws JsonProcessingException {
        try {
            SseEmitter emitter = emitterRepository.get(memberEmail);
            if(emitter != null) {
                emitter.send(SseEmitter.event()
                        .name("sendToParticipant")
                        .data(groupNotification));
            }else {
                log.error(memberEmail + " SseEmitter가 존재하지 않음");
            }
            // redis 저장
            redisService.setGroupList(memberEmail, groupNotification);
        } catch (Exception e){
            log.error("알림 전송 중 에러");
            redisService.setGroupList(memberEmail, groupNotification);
        }
    }

    public void sendRoomNotification(String memberEmail,
                                      RoomNotification roomNotification) throws JsonProcessingException {
        try {
            SseEmitter emitter = emitterRepository.get(memberEmail);
            if(emitter != null) {
                emitter.send(SseEmitter.event()
                        .name("sendRoomAlarm")
                        .data(roomNotification));
            }else {
                log.error(memberEmail + " SseEmitter가 존재하지 않음");
            }
            // redis 저장
            redisService.setRoomList(memberEmail, roomNotification);
        } catch (Exception e){
            log.error("알림 전송 중 에러");
            redisService.setRoomList(memberEmail, roomNotification);
        }
    }
}
