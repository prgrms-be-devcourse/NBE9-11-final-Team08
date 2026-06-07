package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.StudyMember;
import com.team08.backend.domain.study.entity.StudyMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

    boolean existsByStudyIdAndUserIdAndStatus(Long studyId, Long userId, StudyMemberStatus status);
}
