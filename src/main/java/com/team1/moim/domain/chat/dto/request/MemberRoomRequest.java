package com.team1.moim.domain.chat.dto.request;

import com.team1.moim.domain.chat.entity.MemberRoom;
import com.team1.moim.domain.member.entity.Member;
import lombok.Data;

@Data
public class MemberRoomRequest {
    private String memberEmail;

    public MemberRoom toEntity(Member member) {
        return MemberRoom.builder()
                .member(member)
                .build();
    }
}
