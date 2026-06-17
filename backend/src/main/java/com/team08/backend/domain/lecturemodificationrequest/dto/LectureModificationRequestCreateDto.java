package com.team08.backend.domain.lecturemodificationrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LectureModificationRequestCreateDto {

    @NotNull
    private Long lectureId;

    @NotBlank
    private String description;

    public LectureModificationRequestCreateDto(Long lectureId, String description) {
        this.lectureId = lectureId;
        this.description = description;
    }
}