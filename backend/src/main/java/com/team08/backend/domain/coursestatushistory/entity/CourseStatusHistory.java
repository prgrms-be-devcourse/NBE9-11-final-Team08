package com.team08.backend.domain.coursestatushistory.entity;

import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_status_histories")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseStatusHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    private CourseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private CourseStatus toStatus;

    @Lob
    private String reason;

    @Column(nullable = false)
    private Long changedBy;

    public CourseStatusHistory(Long id, Long courseId, CourseStatus fromStatus, CourseStatus toStatus, Long changedBy) {
        this.id = id;
        this.courseId = courseId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = null;
        this.changedBy = changedBy;
    }
}
