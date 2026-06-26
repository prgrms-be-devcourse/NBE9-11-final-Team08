package com.team08.backend.domain.lecture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LectureDetailResponse {
    private Long lectureId;
    private String title;
    private String m3u8Path;
    private Integer durationSeconds;
    private Long chapterId;
    private Long courseId;
}
