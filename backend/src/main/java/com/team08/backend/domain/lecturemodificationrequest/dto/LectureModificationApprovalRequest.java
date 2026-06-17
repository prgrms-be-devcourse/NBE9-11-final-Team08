package com.team08.backend.domain.lecturemodificationrequest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LectureModificationApprovalRequest {
    private String rejectedReason;
}