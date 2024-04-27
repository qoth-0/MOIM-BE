package com.team1.moim.domain.member.entity;

import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String profileImage;

    private String socialId; // 로그인 한 소셜 타입의 식별자 값 (일반 로그인은 null)

    @Enumerated(EnumType.STRING)
    private LoginType loginType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public Account(String email, String profileImage, String socialId, LoginType loginType){
        this.email = email;
        this.profileImage = profileImage;
        this.socialId = socialId;
        this.loginType = loginType;
    }

    public void attachMember(Member member) {
        this.member = member;
        member.getAccounts().add(this);
    }
}
