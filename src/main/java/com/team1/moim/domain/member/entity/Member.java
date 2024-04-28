package com.team1.moim.domain.member.entity;

import com.team1.moim.domain.event.entity.Event;
import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // 대표 이메일

    // 소셜 로그인 유저의 경우 비밀번호가 필요 없으므로, nullable
    private String password;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String profileImage; // 대표 프로필 이미지

    @Enumerated(EnumType.STRING)
    private LoginType loginType; // 대표 로그인 타입

    private String refreshToken;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private String deleteYn = "N";

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<>();

    @Builder
    public Member(String email, String password, String nickname, String profileImage, LoginType loginType, Role role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.loginType = loginType;
        this.role = role;
    }

    public void withdraw() {
        this.deleteYn = "Y";
    }

    public void authorizeUser(){
        this.role = Role.USER;
    }

    public void updateRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }

    public void updateRepresentativeData(String email, String profileImage, LoginType loginType){
        this.email = email;
        this.profileImage = profileImage;
        this.loginType = loginType;
    }

    public void updateMember(String nickname, String profileImage){
        this.nickname = nickname;
        this.profileImage = profileImage;
    }
}
