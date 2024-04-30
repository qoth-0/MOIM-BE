package com.team1.moim.domain.chat.entity;

import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Room과 Member 테이블의 중간 테이블
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    // 채팅룸에 최초 입장하면 false로 변경해서 "xx님이 입장했습니다"라는 메시지를 한 번 더 출력하지 않도록 한다.
    @Builder.Default
    @Column(nullable = false)
    private boolean isFirstEnter = true;

    // 멤버가 호스트인지 알려주는 정보
//    @Builder.Default
//    @Column(nullable = false)
//    private boolean isHost = false;

    // 멤버가 채팅방을 떠난 시각
    private LocalDateTime leaveDate;

    public void attachRoom(Room room) {
        this.room = room;
        room.getMemberRoomList().add(this);
    }
}
