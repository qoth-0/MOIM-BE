package com.team1.moim.domain.member.repository;

import com.team1.moim.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByRefreshToken(String refreshToken);

    @Query("SELECT m FROM Member m WHERE m <> :member AND m.deleteYn = 'N'")
    List<Member> findAllMemberExcept(@Param("member") Member member);

}
