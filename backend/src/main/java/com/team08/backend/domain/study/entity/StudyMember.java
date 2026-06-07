package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "study_members",
        indexes = @Index(name = "idx_study_members_study_user", columnList = "study_id, user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_study_members_study_user", columnNames = {"study_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyMemberStatus status;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private LocalDateTime kickedAt;

    private StudyMember(User user, Study study, StudyMemberRole role) {
        this.user = user;
        this.study = study;
        this.role = role;
        status = StudyMemberStatus.ACTIVE;
        joinedAt = LocalDateTime.now();
    }

    public static StudyMember createOwner(User owner, Study study) {
        return new StudyMember(owner, study, StudyMemberRole.OWNER);
    }

    public static StudyMember createMember(User user, Study study) {
        return new StudyMember(user, study, StudyMemberRole.MEMBER);
    }
}
