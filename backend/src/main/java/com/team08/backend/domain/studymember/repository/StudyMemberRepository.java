package com.team08.backend.domain.studymember.repository;

import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    @Query("""
        SELECT sm
        FROM StudyMember sm
        JOIN FETCH sm.user
        WHERE sm.study.id = :studyId
            AND sm.status = :status
        ORDER BY sm.role ASC, sm.joinedAt ASC
    """)
    List<StudyMember> findActiveMembersWithUser(
            @Param("studyId") Long studyId,
            @Param("status") StudyMemberStatus status
    );

    boolean existsByStudyIdAndUserIdAndStatus(
            Long studyId,
            Long userId,
            StudyMemberStatus status
    );

    Optional<StudyMember> findByStudyIdAndUserIdAndStatus(
            Long studyId,
            Long userId,
            StudyMemberStatus status
    );

    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);
}
