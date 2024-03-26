package com.team1.moim.domain.event.entity;

import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ToDoList extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //    EventID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    //    내용
    @Column(nullable = false)
    private String contents;

    //    완료여부
    @Column(nullable = false)
    private String isChecked = "N";

    @Builder
    public ToDoList(Event event, String contents, String isChecked) {
        this.event = event;
        this.contents = contents;
        this.isChecked = isChecked;
    }

}
