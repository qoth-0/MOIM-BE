package com.team1.moim.global.config.security.oauth2;

import com.team1.moim.domain.member.entity.LoginType;
import com.team1.moim.domain.member.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final String nickname;
    private final String email;
    private final String profileImage;
    private final LoginType loginType;
    private final Role role;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes, String nameAttributeKey,
                            String nickname, String email, String profileImage, LoginType loginType, Role role){
        super(authorities, attributes, nameAttributeKey);
        this.nickname = nickname;
        this.email = email;
        this.profileImage = profileImage;
        this.loginType = loginType;
        this.role = role;
    }
}
