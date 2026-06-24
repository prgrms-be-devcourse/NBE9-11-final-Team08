package com.team08.backend.domain.study.access;

import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;

public record StudyAccessContext(
        Long studyId,
        Long userId,
        StudyStatus studyStatus,
        boolean activeMember,
        StudyMemberRole memberRole
) {
    public boolean isOwner() {
        return memberRole == StudyMemberRole.OWNER;
    }

    public boolean isActiveMember() {
        return activeMember;
    }

    public boolean isReadableStudy() {
        return studyStatus == StudyStatus.ACTIVE
                || studyStatus == StudyStatus.READONLY;
    }

    public boolean isWritableStudy() {
        return studyStatus == StudyStatus.ACTIVE;
    }
}
