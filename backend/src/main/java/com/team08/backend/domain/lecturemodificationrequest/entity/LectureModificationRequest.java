package com.team08.backend.domain.lecturemodificationrequest.entity;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecture_modification_requests")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureModificationRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false)
    private Long instructorId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String beforeM3u8Path;

    @Column(nullable = false)
    private String afterM3u8Path;

    @Column(name = "after_video_uuid", length = 36)
    private String afterVideoUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;

    private Long managedBy;

    public void reject(String reason, Long managedBy) {
        this.status = RequestStatus.REJECTED;
        this.rejectedReason = reason;
        this.managedBy = managedBy;
    }

    public void approve(Long managedBy) {
        this.status = RequestStatus.APPROVED;
        this.managedBy = managedBy;
    }

    public static LectureModificationRequest createPending(Lecture lecture, Long instructorId, String description, String afterM3u8Path, String afterVideoUuid) {
        LectureModificationRequest request = new LectureModificationRequest();
        request.lecture = lecture;
        request.instructorId = instructorId;
        request.description = description;
        request.beforeM3u8Path = lecture.getM3u8Path();
        request.afterM3u8Path = afterM3u8Path;
        request.afterVideoUuid = afterVideoUuid;
        request.status = RequestStatus.PENDING;
        return request;
    }
}