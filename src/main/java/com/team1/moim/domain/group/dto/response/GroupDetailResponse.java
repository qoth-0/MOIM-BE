package com.team1.moim.domain.group.dto.response;

import com.team1.moim.domain.group.entity.Group;
import com.team1.moim.domain.group.entity.GroupType;
import com.team1.moim.domain.member.entity.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupDetailResponse {
    private Long id;
    private String hostNickname;
    private String isConfirmed;
    private String title;
    private String place;
    private LocalDate expectStartDate;
    private LocalDate expectEndDate;
    private LocalTime expectStartTime;
    private LocalTime expectEndTime;
    private LocalDateTime voteDeadline;
    private String contents;
    private String filePath;
    private int participants;
    private GroupType groupType;
    private int runningTime;
    private List<GroupInfoResponse> groupInfos;

    public static GroupDetailResponse from(Group group) {

        List<GroupInfoResponse> groupInfos = group.getGroupInfos().stream()
                .map(GroupInfoResponse::from)
                .collect(Collectors.toList());

        return GroupDetailResponse.builder()
                .id(group.getId())
                .hostNickname(group.getMember().getNickname())
                .isConfirmed(group.getIsConfirmed())
                .title(group.getTitle())
                .place(group.getPlace())
                .expectStartDate(group.getExpectStartDate())
                .expectEndDate(group.getExpectEndDate())
                .expectStartTime(group.getExpectStartTime())
                .expectEndTime(group.getExpectEndTime())
                .voteDeadline(group.getVoteDeadline())
                .contents(group.getContents())
                .filePath(group.getFilePath())
                .participants(group.getParticipants())
                .groupType(group.getGroupType())
                .runningTime(group.getRunningTime())
                .groupInfos(groupInfos)
                .build();
    }
}
