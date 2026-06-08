package com.team08.backend.domain.course.dto;

import java.util.List;

public record CurriculumSaveRequest(
        List<ChapterSaveDto> chapters
) {
}