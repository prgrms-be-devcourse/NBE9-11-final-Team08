package com.team08.backend.domain.studymember.repository;

import com.team08.backend.domain.studymember.entity.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
}
