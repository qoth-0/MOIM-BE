package com.team1.moim.domain.notification.dto;

import com.team1.moim.domain.notification.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class NotificationResponseNew {
    private NotificationType notificationType;
    // event 또는 gruop의 chatId
    private Long id;
    private Long redisId;
    private String nickname;
    private String message;
    private String sendTime;
    private String readYn;

    @Builder
    public NotificationResponseNew(Long id, Long redisId,  String nickname, String message, String sendTime, NotificationType notificationType, String readYn) {
        this.id = id;
        this.redisId = redisId;
        this.nickname = nickname;
        this.message = message;
        this.sendTime = sendTime;
        this.notificationType = notificationType;
        this.readYn = readYn;
    }

    public static NotificationResponseNew fromEvent(EventNotification eventNotification, Long redisId){
        return NotificationResponseNew.builder()
                .id(eventNotification.getEventId())
                .redisId(redisId)
                .nickname(eventNotification.getNickname())
                .message(eventNotification.getMessage())
                .sendTime(eventNotification.getSendTime())
                .notificationType(eventNotification.getNotificationType())
                .readYn(eventNotification.getReadYn())
                .build();
    }

    public static NotificationResponseNew fromGroup(GroupNotification groupNotification, Long redisId){
        return NotificationResponseNew.builder()
                .id(groupNotification.getGroupId())
                .redisId(redisId)
                .nickname(groupNotification.getHostName())
                .message(groupNotification.getMessage())
                .sendTime(groupNotification.getSendTime())
                .notificationType(groupNotification.getNotificationType())
                .readYn(groupNotification.getReadYn())
                .build();
    }

    public static NotificationResponseNew fromRoom(RoomNotification roomNotification, Long redisId) {
        return NotificationResponseNew.builder()
                .id(roomNotification.getRoomId())
                .redisId(redisId)
                .nickname(roomNotification.getHostName())
                .message(roomNotification.getMessage())
                .sendTime(roomNotification.getSendTime())
                .notificationType(roomNotification.getNotificationType())
                .readYn(roomNotification.getReadYn())
                .build();
    }

    public void read(String readYn) {
        this.readYn = readYn;
    }


}
