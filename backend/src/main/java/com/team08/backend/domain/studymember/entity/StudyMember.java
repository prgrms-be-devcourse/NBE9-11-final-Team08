package com.team08.backend.domain.studymember.entity;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_members", uniqueConstraints = @UniqueConstraint(columnNames = {"study_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StudyMemberRole role;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StudyMemberStatus status;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private LocalDateTime kickedAt;

    private StudyMember(User user, Study study, StudyMemberRole role) {
        this.user = user;
        this.study = study;
        this.role = role;
        this.status = StudyMemberStatus.ACTIVE;
        this.joinedAt = LocalDateTime.now();
    }

    public static StudyMember owner(User user, Study study) {
        return new StudyMember(user, study, StudyMemberRole.OWNER);
    }
}
