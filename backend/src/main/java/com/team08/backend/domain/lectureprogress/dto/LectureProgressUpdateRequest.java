package com.team08.backend.domain.lectureprogress.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LectureProgressUpdateRequest(

        @NotNull
        @PositiveOrZero
        Integer positionSeconds,

        //마지막 하트비트 이후 추가적으로 영상 시청한 시간
        @NotNull
        @PositiveOrZero
        Integer watchedDeltaSeconds
) {}
