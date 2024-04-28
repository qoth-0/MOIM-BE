package com.team1.moim.domain.member.repository;

import com.team1.moim.domain.member.entity.Account;
import com.team1.moim.domain.member.entity.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLoginTypeAndSocialId(LoginType loginType, String socialId);
    Optional<Account> findByEmail(String email);
}
