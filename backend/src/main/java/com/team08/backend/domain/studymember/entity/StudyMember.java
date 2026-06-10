package com.team08.backend.domain.studymember.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_members", uniqueConstraints = @UniqueConstraint(columnNames = {"study_id", "user_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long studyId;
    @Column(nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StudyMemberRole role;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StudyMemberStatus status;
}
