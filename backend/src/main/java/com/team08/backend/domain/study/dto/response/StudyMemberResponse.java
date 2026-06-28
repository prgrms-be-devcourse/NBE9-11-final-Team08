package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;

import java.time.LocalDateTime;

public record StudyMemberResponse(
        Long userId,
        String nickname,
        String profileImage,
        StudyMemberRole role,
        LocalDateTime joinedAt
) {
    public static StudyMemberResponse from(StudyMember member) {
        return new StudyMemberResponse(
                member.getUser().getId(),
                member.getUser().getNickname(),
                member.getUser().getProfileImage(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
