package com.team08.backend.domain.studymember.repository;

import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    Optional<StudyMember> findByStudyIdAndUserIdAndStatus(
            Long studyId,
            Long userId,
            StudyMemberStatus status
    );
}
