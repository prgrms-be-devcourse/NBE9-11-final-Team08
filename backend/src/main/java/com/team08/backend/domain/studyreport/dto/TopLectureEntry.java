package com.team08.backend.domain.studyreport.dto;

public record TopLectureEntry(
        Long lectureId,
        String title,
        Integer watchTimeSeconds
) {}
