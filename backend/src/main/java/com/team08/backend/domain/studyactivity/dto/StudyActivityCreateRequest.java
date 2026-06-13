package com.team08.backend.domain.studyactivity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyActivityCreateRequest(
        @NotBlank(message = "스터디 활동 내용은 필수입니다.")
        @Size(
                min = 20,
                max = 2000,
                message = "스터디 활동 내용은 20자 이상 2000자 이하로 입력해주세요."
        )
        String content
) {
}
