package com.team1.moim.domain.group.dto.response;

import com.team1.moim.domain.group.entity.Group;
import com.team1.moim.domain.group.entity.GroupInfo;
import com.team1.moim.domain.group.entity.GroupType;
import com.team1.moim.domain.member.entity.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ListGroupResponse {
    private Long id;
    private String title;
    private int runningTime;
    private LocalDateTime voteDeadline;
    private LocalDateTime confirmedDateTime;
    private int participants;
    private String isConfirmed;
    private String isDeleted;
    private String isAddEvent;
    private String filePath;
    private LocalDate expectStartDate;
    private LocalDate expectEndDate;
    private LocalTime expectStartTime;
    private LocalTime expectEndTime;
    private String place;
    private String contents;
    private String hostEmail;
    private String hostNickname;
    private List<String[]> guestEmailNicknameIsAgreed;
    private GroupType groupType;

//cat

    public static ListGroupResponse from(Group group,List<String[]> guestEmailNicknameIsAgreed) {
        return ListGroupResponse.builder()
                .id(group.getId())
                .title(group.getTitle())
                .runningTime(group.getRunningTime())
                .voteDeadline(group.getVoteDeadline())
                .confirmedDateTime(group.getConfirmedDateTime())
                .participants(group.getParticipants())
                .isConfirmed(group.getIsConfirmed())
                .isDeleted(group.getIsDeleted())
                .isAddEvent(group.getIsAddEvent())
                .filePath(group.getFilePath())
                .expectStartDate(group.getExpectStartDate())
                .expectEndDate(group.getExpectEndDate())
                .expectStartTime(group.getExpectStartTime())
                .expectEndTime(group.getExpectEndTime())
                .place(group.getPlace())
                .contents(group.getContents())
                .hostEmail(group.getMember().getEmail())
                .hostNickname(group.getMember().getNickname())
                .guestEmailNicknameIsAgreed(guestEmailNicknameIsAgreed)
                .groupType(group.getGroupType())
                .build();
    }
}
