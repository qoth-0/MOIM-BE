package com.team1.moim.domain.chat.dto.request;

import com.team1.moim.domain.chat.entity.Room;
import com.team1.moim.domain.member.entity.Member;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoomRequest {

    @NotEmpty(message = "채팅방 이름을 설정하세요.")
    private String title;

    @NotEmpty(message = "채팅방 삭제 시간을 설정하세요.")
    private String deleteDate;

    public Room toEntity(Member member, List<MemberRoomRequest> memberRequests) {

        return Room.builder()
                .member(member)
                .title(title)
                .deleteDate(LocalDateTime.parse(deleteDate))
                .participants(memberRequests.size())
                .build();
    }
}
