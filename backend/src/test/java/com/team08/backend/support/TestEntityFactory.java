package com.team08.backend.support;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestEntityFactory {

    private TestEntityFactory() {
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트 엔티티 생성 실패: " + clazz.getSimpleName(), e);
        }
    }

    public static User user(Long userId) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "id", userId);

        return user;
    }

    public static Course course(Long courseId) {
        Course course = newInstance(Course.class);
        ReflectionTestUtils.setField(course, "id", courseId);

        return course;
    }
}