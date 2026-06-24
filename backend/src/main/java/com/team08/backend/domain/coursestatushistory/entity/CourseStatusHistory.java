package com.team08.backend.domain.coursestatushistory.entity;

import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.global.common.BaseTimeEntity;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_status_histories")
@Getter
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

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private Long changedBy;

    private CourseStatusHistory(Long courseId, CourseStatus fromStatus, CourseStatus toStatus, String reason, Long changedBy) {
        validateInitialState(courseId, changedBy);
        this.courseId = courseId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    public static CourseStatusHistory of(Long courseId, CourseStatus fromStatus, CourseStatus toStatus, Long changedBy) {
        return new CourseStatusHistory(courseId, fromStatus, toStatus, null, changedBy);
    }

    public static CourseStatusHistory of(Long courseId, CourseStatus fromStatus, CourseStatus toStatus, Long changedBy, String reason) {
        return new CourseStatusHistory(courseId, fromStatus, toStatus, reason, changedBy);
    }

    private void validateInitialState(Long courseId, Long changedBy) {
        if (courseId == null || changedBy == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}