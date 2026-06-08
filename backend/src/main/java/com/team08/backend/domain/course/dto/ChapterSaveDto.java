package com.team08.backend.domain.course.dto;

import java.util.List;

public record ChapterSaveDto(
        Long id,
        String title,
        Integer orderNo,
        List<LectureSaveDto> lectures
) {}