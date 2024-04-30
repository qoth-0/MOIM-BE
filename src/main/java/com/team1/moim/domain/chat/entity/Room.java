package com.team1.moim.domain.chat.entity;

import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 호스트 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 채팅방의 이름
    @Column(nullable = false)
    private String title;

    // 채팅방 삭제일
    // 현재 날짜를 기준으로 최소한 30분은 지속되어야 한다.
    @Column(nullable = false)
    private LocalDateTime deleteDate;

    // 참여자 목록
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberRoom> memberRoomList = new ArrayList<>();

    // 참여자수
    private int participants;

    @Column(nullable = false)
    private String deleteYn = "N";

    @Builder
    public Room(Member member, String title, LocalDateTime deleteDate, int participants) {
        this.member = member;
        this.title = title;
        this.deleteDate = deleteDate;
        this.participants = participants;
    }
}
