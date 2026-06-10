package com.team08.backend.domain.lecturemodificationrequest.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_modification_requests")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureModificationRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long courseId;
    @Column(nullable = false)
    private Long lectureId;
    @Column(nullable = false)
    private Long instructorId;
    @Lob @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private String beforeM3u8Path;
    @Column(nullable = false)
    private String afterM3u8Path;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RequestStatus status;
    @Lob
    private String rejectedReason;
    private Long managedBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
