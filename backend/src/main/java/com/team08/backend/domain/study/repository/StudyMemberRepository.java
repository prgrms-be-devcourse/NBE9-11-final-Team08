package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.StudyMember;
import com.team08.backend.domain.study.entity.StudyMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

    List<StudyMember> findByStudyIdAndStatusOrderByJoinedAtAsc(Long studyId, StudyMemberStatus status);

    Optional<StudyMember> findByIdAndStudyId(Long id, Long studyId);

    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);

    boolean existsByStudyIdAndUserIdAndStatus(Long studyId, Long userId, StudyMemberStatus status);
}
