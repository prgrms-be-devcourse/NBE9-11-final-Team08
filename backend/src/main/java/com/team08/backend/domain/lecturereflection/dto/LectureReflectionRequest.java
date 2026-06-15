package com.team08.backend.domain.lecturereflection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record LectureReflectionRequest(
        @NotBlank(message = "회고 내용은 필수입니다.")
        @Size(max = 5000, message = "회고 내용은 5000자 이하로 입력해주세요.")
        String content

) {

}