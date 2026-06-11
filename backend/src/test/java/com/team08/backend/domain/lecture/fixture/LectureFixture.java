package com.team08.backend.domain.lecture.fixture;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.entity.Lecture;
import org.springframework.test.util.ReflectionTestUtils;

public final class LectureFixture {

    private LectureFixture() {
    }

    public static Lecture lecture(String title, String m3u8Path, int durationSeconds, int orderNo, Chapter chapter) {
        return Lecture.builder()
                .title(title)
                .m3u8Path(m3u8Path)
                .summary("강의 요약본입니다.")
                .durationSeconds(durationSeconds)
                .orderNo(orderNo)
                .isFreePreview(false)
                .chapter(chapter)
                .build();
    }

    public static Lecture lecture(Long id, String title, String m3u8Path, int durationSeconds, int orderNo, Chapter chapter) {
        Lecture lecture = lecture(title, m3u8Path, durationSeconds, orderNo, chapter);
        ReflectionTestUtils.setField(lecture, "id", id);
        return lecture;
    }
}