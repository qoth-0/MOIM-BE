package com.team1.moim.global.config.security.oauth2.handler;

import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.entity.Role;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.repository.MemberRepository;
import com.team1.moim.global.config.security.jwt.JwtProvider;
import com.team1.moim.global.config.security.oauth2.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        log.info("OAuth2 Login 성공");

        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            Member findMember = memberRepository.findByNickname(oAuth2User.getNickname())
                    .orElseThrow(MemberNotFoundException::new);
            String accessToken;
            String refreshToken;

            if (oAuth2User.getRole() == Role.GUEST){
                log.info("GUEST 권한이면 토큰 생성 후 USER 권한으로 바꿔줌");
                accessToken = jwtProvider.createAccessToken(oAuth2User.getNickname(), Role.GUEST.name());
                refreshToken = jwtProvider.createRefreshToken();
                findMember.authorizeUser();
            } else {
                log.info("USER 권한의 경우");
                accessToken = jwtProvider.createAccessToken(oAuth2User.getNickname(), oAuth2User.getRole().name());
                refreshToken = jwtProvider.createRefreshToken();
            }
            response.addHeader(jwtProvider.getAccessHeader(), "Bearer " + accessToken);
            response.addHeader(jwtProvider.getRefreshHeader(), "Bearer " + refreshToken);
            jwtProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);

            findMember.updateRepresentativeData(
                    oAuth2User.getEmail(),
                    oAuth2User.getProfileImage(),
                    oAuth2User.getLoginType());
            findMember.updateRefreshToken(refreshToken);
            memberRepository.saveAndFlush(findMember);

            log.info("Redirect Strategy 시작");

            String redirectUrl = makeRedirectUrl(accessToken, refreshToken);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (Exception e){
            log.error("OAuth2 Login 실패: {}", e.getMessage());
        }
    }

    private String makeRedirectUrl(String accessToken, String refreshToken){
        return UriComponentsBuilder.fromUriString("http://localhost:8081/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();
    }
}
