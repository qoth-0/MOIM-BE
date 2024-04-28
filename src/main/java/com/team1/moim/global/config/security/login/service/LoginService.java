package com.team1.moim.global.config.security.login.service;

import com.team1.moim.domain.member.entity.Account;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.AccountNotFoundException;
import com.team1.moim.domain.member.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커스텀 로그인 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService implements UserDetailsService {

    private final AccountRepository accountRepository;

    // UserDetails의 User 객체를 만들어서 반환하는 메서드
    // 반환받은 UserDetails 객체의 password를 꺼내어, 내부의 PasswordEncoder에서 password가 일치하는 지 검증 수행
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.info("1. loadUserByUsername 진입!");
        // DaoAuthenticationProvider가 설정해준 email을 가진 유저를 찾는다.

        Account findAccount = accountRepository.findByEmail(email).orElseThrow(AccountNotFoundException::new);
        log.info("2. findAccount - Email: {}", findAccount.getEmail());

        Member findMember = findAccount.getMember();
        log.info("3. findMember - email: {}, nickname: {}", findMember.getEmail(), findMember.getNickname());

        return User.builder()
                .username(findMember.getEmail())
                .password(findMember.getPassword())
                // roles의 메서드를 보면 파라미터로 들어온 role들이 "ROLE_"으로 시작하지 않으면, 예외를 발생시킴
                .roles(findMember.getRole().name())
                .build();
    }
}
