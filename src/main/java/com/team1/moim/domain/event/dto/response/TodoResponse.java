package com.team1.moim.domain.event.dto.response;

import com.team1.moim.domain.event.entity.Alarm;
import com.team1.moim.domain.event.entity.AlarmType;
import com.team1.moim.domain.event.entity.ToDoList;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoResponse {
    private Long id;
    private String contents;
    private String isChecked;

    public static TodoResponse from(ToDoList toDoList){
        return TodoResponse.builder()
                .id(toDoList.getId())
                .contents(toDoList.getContents())
                .isChecked(toDoList.getIsChecked())
                .build();
    }
}
