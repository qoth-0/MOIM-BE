package com.team1.moim.domain.chat.entity;

import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 전송한 메시지
    @Column(nullable = false)
    private String content;

    // 전송한 사람
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    // 메시지가 전송된 채팅룸
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    // 메시지타입
    @Enumerated(EnumType.STRING)
    private MessageType type;
}
