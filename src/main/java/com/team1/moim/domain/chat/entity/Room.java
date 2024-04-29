package com.team1.moim.domain.chat.entity;

import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    // 채팅방의 이름
    @Column(nullable = false)
    private String name;

    // 채팅방 삭제일
    // 현재 날짜를 기준으로 최소한 30분은 지속되어야 한다.
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime deleteDate;

    // 채팅방 인원 수
    // 나를 포함 최소 2명이 있어야 채팅이 가능
    @Column(nullable = false)
    private int numberOfMembers = 2;

    // 참여자 목록
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberRoom> memberRoomList = new ArrayList<>();

    // 채팅방 비밀번호
    private String password;

    @Column(nullable = false)
    private String deleteYn = "N";

    @Builder
    public Room(String name,
                LocalDateTime deleteDate,
                int numberOfMembers,
                String password,
                String deleteYn) {
        this.name = name;
        this.deleteDate = deleteDate;
        this.numberOfMembers = numberOfMembers;
        this.password = password;
        this.deleteYn = deleteYn;
    }
}
