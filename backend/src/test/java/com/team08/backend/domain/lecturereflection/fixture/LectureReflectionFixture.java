package com.team08.backend.domain.lecturereflection.fixture;
import com.team08.backend.domain.lecturereflection.entity.LectureReflection;
public class LectureReflectionFixture {

    public static LectureReflection reflection() {
        return LectureReflection.create(1L, 10L, "회고 내용");
    }

    public static LectureReflection reflection(
            Long userId,
            Long lectureId,
            String content
    ) {
        return LectureReflection.create(userId, lectureId, content);
    }
}