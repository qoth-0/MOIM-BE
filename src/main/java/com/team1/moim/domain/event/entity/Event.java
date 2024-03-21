package com.team1.moim.domain.event.entity;

import com.team1.moim.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    회원ID
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id", nullable = false)
//    private Member member;

//    제목
    @Column(nullable = false)
    private String title;

//    내용
    @Column(nullable = false)
    private String memo;

//    시작일자
    @Column(nullable = false)
    private LocalDateTime startDate;

//    종료일자
    @Column(nullable = false)
    private LocalDateTime endDate;

//    장소
    @Column
    private String place;

//    사분면
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Matrix matrix;

//    첨부파일
    @Column
    private String file;

//    삭제여부
    @Column(nullable = false)
    private String deleteYn = "N";

//    반복여부
    @Column(nullable = false)
    private String repeatYn = "N";

//    알림여부
    @Column(nullable = false)
    private String alarmYn = "N";

    @Builder
    public Event(String title, String memo, LocalDateTime startDate, LocalDateTime endDate, String place, Matrix matrix, String file) {
        this.title = title;
        this.memo = memo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.place = place;
        this.matrix = matrix;
        this.file = file;
    }

}
