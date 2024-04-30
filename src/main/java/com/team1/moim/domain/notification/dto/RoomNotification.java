package com.team1.moim.domain.notification.dto;

import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.notification.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class RoomNotification {
    private Long alarmId;
    private Long roomId; // 채팅방 ID
    private String hostName; // 호스트명
    private String message; // 참여자에게 보여지는 메시지
    private String roomTitle; // 채팅방 명
    private String deleteDate; // 채팅방 삭제일
    private String sendTime;
    private NotificationType notificationType;
    private String readYn = "N";

    @Builder
    public RoomNotification(Long alarmId, Long roomId, String hostName, String message, String roomTitle, String deleteDate, String sendTime, NotificationType notificationType) {
        this.alarmId = alarmId;
        this.roomId = roomId;
        this.hostName = hostName;
        this.message = message;
        this.roomTitle = roomTitle;
        this.deleteDate = deleteDate;
        this.sendTime = sendTime;
        this.notificationType = notificationType;
    }
    public static RoomNotification from(Room room,
                                        String message, NotificationType notificationType, LocalDateTime sendTime) {

        return RoomNotification.builder()
                .roomId(room.getId())
                .hostName(room.getMember().getNickname())
                .message(message)
                .roomTitle(room.getTitle())
                .deleteDate(room.getDeleteDate().toString())
                .sendTime(sendTime.toString())
                .notificationType(notificationType)
                .build();
    }
}
