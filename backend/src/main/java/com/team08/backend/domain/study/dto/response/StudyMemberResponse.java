package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.study.entity.StudyMember;
import com.team08.backend.domain.study.entity.StudyMemberRole;
import com.team08.backend.domain.study.entity.StudyMemberStatus;

import java.time.LocalDateTime;

public record StudyMemberResponse(
        Long memberId,
        Long studyId,
        Long userId,
        String nickname,
        StudyMemberRole role,
        StudyMemberStatus status,
        LocalDateTime joinedAt,
        LocalDateTime leftAt,
        LocalDateTime kickedAt
) {
    public static StudyMemberResponse from(StudyMember member) {
        return new StudyMemberResponse(
                member.getId(),
                member.getStudy().getId(),
                member.getUser().getId(),
                member.getUser().getNickname(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getLeftAt(),
                member.getKickedAt()
        );
    }
}
