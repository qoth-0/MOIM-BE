package com.team1.moim.domain.member.service;

import com.team1.moim.domain.member.dto.request.UpdateRequest;
import com.team1.moim.domain.member.dto.response.MemberResponse;
import com.team1.moim.domain.member.entity.Member;
import com.team1.moim.domain.member.exception.MemberNotFoundException;
import com.team1.moim.domain.member.exception.NicknameDuplicateException;
import com.team1.moim.domain.member.repository.MemberRepository;
import com.team1.moim.global.config.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private static final String FILE_TYPE = "members";
    private final S3Service s3Service;

    private final MemberRepository memberRepository;

    @Transactional
    public String delete() {
        Member findMember = findMember();
        findMember.withdraw();

        return findMember.getEmail() + " 회원을 삭제하였습니다.";
    }

    @Transactional
    public MemberResponse view() {

        Member findMember = findMember();

        return MemberResponse.from(findMember);
    }

    private Member findMember() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);
    }

    // 멤버 검색
    public List<MemberResponse> searchMember() {
        Member myMember = findMember();
        List<Member> members = memberRepository.findAllMemberExcept(myMember);

        List<MemberResponse> memberResponses = new ArrayList<>();
        for (Member member : members) {
            memberResponses.add(MemberResponse.from(member));
        }
        return memberResponses;
    }

    public MemberResponse update(UpdateRequest updateRequest) {
        Member myMember = findMember();

        if (memberRepository.findByNickname(updateRequest.getNickname()).isPresent()) {
            throw new NicknameDuplicateException();
        }

        String imageUrl;
        if (updateRequest.getProfileImage() != null && !updateRequest.getProfileImage().isEmpty()){
            imageUrl = s3Service.uploadFile(FILE_TYPE, updateRequest.getProfileImage());
        } else {
            imageUrl = s3Service.getDefaultImage(FILE_TYPE);
        }

        myMember.updateMember(updateRequest.getNickname() , imageUrl);

        log.info("수정된 멤버 정보" + myMember);
        return MemberResponse.from(myMember);
    }

    // 유저 정보 수정
}
