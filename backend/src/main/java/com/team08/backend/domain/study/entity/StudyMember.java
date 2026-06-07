package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.study.exception.InvalidStudyMemberStatusException;
import com.team08.backend.domain.study.exception.StudyOwnerCannotBeKickedException;
import com.team08.backend.domain.study.exception.StudyOwnerCannotLeaveException;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.*;
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

    public void kick() {
        validateActive();

        if (isOwner()) {
            throw new StudyOwnerCannotBeKickedException();
        }

        status = StudyMemberStatus.KICKED;
        kickedAt = LocalDateTime.now();
    }

    public void leave() {
        validateActive();

        if (isOwner()) {
            throw new StudyOwnerCannotLeaveException();
        }

        status = StudyMemberStatus.LEFT;
        leftAt = LocalDateTime.now();
    }

    public void rejoinAsMember() {
        if (status == StudyMemberStatus.KICKED) {
            throw new InvalidStudyMemberStatusException();
        }

        role = StudyMemberRole.MEMBER;
        status = StudyMemberStatus.ACTIVE;
        joinedAt = LocalDateTime.now();
        leftAt = null;
    }

    private void validateActive() {
        if (status != StudyMemberStatus.ACTIVE) {
            throw new InvalidStudyMemberStatusException();
        }
    }

    private boolean isOwner() {
        return role == StudyMemberRole.OWNER;
    }
}
