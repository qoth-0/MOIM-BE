package com.team1.moim.global.config.security.oauth2.service;

import com.team1.moim.domain.member.entity.Account;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.entity.LoginType;
import com.team1.moim.domain.member.repository.AccountRepository;
import com.team1.moim.domain.member.repository.MemberRepository;
import com.team1.moim.global.config.security.oauth2.CustomOAuth2User;
import com.team1.moim.global.config.security.oauth2.OAuthAttributes;
import com.team1.moim.global.config.security.oauth2.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    private static final String KAKAO = "kakao";

    // 소셜 로그인 API의 사용자 정보 제공 URI로 요청을 보내서
    // 사용자 정보를 얻은 후, 이를 통해 DefaultOAuth2User 객체를 생성 후 반환
    // 결과적으로, OAuth2User는 OAuth 서비스에서 가져온 유저 정보를 담고 있는 유저
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("CustomOAuth2UserService.loadUser() 실행 - OAuth2 로그인 요청 진입");

        // DefaultOAuth2UserService 객체를 생성하여, loadUser(userRequest)를 통해 DefaultOAuth2User 객체를 생성 후 반환
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        log.info("1");

        // registrationId 추출 후 이 Id로 SocialType 구해서 저장
        // http://localhost:8080/oauth2/authorization/kakao에서 kakao가 registrationId
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("2");
        LoginType loginType = getSocialType(registrationId);
        log.info("3 / socialType: {}", loginType);
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName(); // OAuth2 로그인 시 키(PK)가 되는 값
        log.info("4 / userNameAttributeName: {}", userNameAttributeName);

        // 소셜 로그인에서 API가 제공하는 userInfo의 Json 값(유저 정보들)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.info("5 / attributes: {}", attributes);

        // SocialType에 따라 유저 정보를 통해 OAuthAttributes 객체 생성
        OAuthAttributes extractAttributes = OAuthAttributes.of(loginType, userNameAttributeName, attributes);
        log.info("6 / extractAttributes: {}", extractAttributes);

        Member createdMember = getMember(extractAttributes, loginType);
        log.info("7 / createdMember: {}", createdMember);

        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(createdMember.getRole().name())),
                attributes,
                extractAttributes.getNameAttributeKey(),
                createdMember.getNickname(),
                createdMember.getEmail(),
                createdMember.getProfileImage(),
                createdMember.getLoginType(),
                createdMember.getRole());
    }

    private LoginType getSocialType(String registrationId) {
        if (registrationId.equals(KAKAO)){
            return LoginType.KAKAO;
        }
        return LoginType.GOOGLE;
    }

    // SocialType과 attributes에 들어있는 소셜 로그인의 식별값 id를 통해 회원을 찾아 반환
    // 만약 찾은 회원이 있다면 그대로 반환하고, 없다면 saveUser()를 호출하여 회원을 저장
    private Member getMember(OAuthAttributes attributes, LoginType loginType){

        log.info("소셜 로그인 유저의 닉네임이 DB에 이미 존재한다면 ");
        log.info("DB에 소셜 로그인 회원 있는지 확인!!");

        Account account = accountRepository.findByLoginTypeAndSocialId(loginType,
                attributes.getOAuth2UserInfo().getId()).orElse(null);

        if (account == null){
            log.info("신규 소셜 로그인 계정이므로 DB에 저장해주기!");
            return saveMember(attributes, loginType);
        }

        account.getMember().updateRepresentativeData(
                attributes.getOAuth2UserInfo().getEmail(),
                attributes.getOAuth2UserInfo().getImageUrl(),
                loginType
        );

        return memberRepository.save(account.getMember());
    }

    // 생성된 Member 객체를 DB에 저장 : socialType, socialId, email, role 값만 있는 상태
    private Member saveMember(OAuthAttributes attributes, LoginType loginType){

        OAuth2UserInfo oAuth2UserInfo = attributes.getOAuth2UserInfo();
        Account account = Account.builder()
                .email(oAuth2UserInfo.getEmail())
                .profileImage(oAuth2UserInfo.getImageUrl())
                .socialId(oAuth2UserInfo.getId())
                .loginType(loginType)
                .build();

        Optional<Member> optionalMember = memberRepository.findByNickname(oAuth2UserInfo.getNickname());
        if (optionalMember.isPresent()){
            Member findMember = optionalMember.get();
            account.attachMember(findMember);
            log.info("{}과 동일한 닉네임의 유저가 이미 있습니다!", findMember.getNickname());
            findMember.getAccounts().add(account);
            findMember.updateRepresentativeData(account.getEmail(), account.getProfileImage(), loginType);
            return memberRepository.save(findMember);
        }
        Member createdMember = attributes.toEntity(attributes.getOAuth2UserInfo());
        account.attachMember(createdMember);
        log.info("{} 소셜 로그인 회원이 DB에 저장됩니다. ", createdMember.getNickname());

        return memberRepository.save(createdMember);
    }
}
