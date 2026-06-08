package com.team08.backend.support;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyMember;
import com.team08.backend.domain.study.entity.StudyMemberRole;
import com.team08.backend.domain.study.entity.StudyMemberStatus;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.entity.StudyVisibility;
import com.team08.backend.domain.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    public static User user(String email, String nickname) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", nickname);
        ReflectionTestUtils.setField(user, "role", "USER");
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return user;
    }

    public static Course course(User instructor, String title) {
        Course course = newInstance(Course.class);
        ReflectionTestUtils.setField(course, "instructor", instructor);
        ReflectionTestUtils.setField(course, "title", title);
        ReflectionTestUtils.setField(course, "price", 10000);
        ReflectionTestUtils.setField(course, "status", CourseStatus.ON_SALE);
        ReflectionTestUtils.setField(course, "viewCount", 0);
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(course, "createdAt", now);
        ReflectionTestUtils.setField(course, "updatedAt", now);
        return course;
    }

    public static Chapter chapter(Course course, String title, int orderNo) {
        Chapter chapter = newInstance(Chapter.class);
        ReflectionTestUtils.setField(chapter, "course", course);
        ReflectionTestUtils.setField(chapter, "title", title);
        ReflectionTestUtils.setField(chapter, "orderNo", orderNo);
        return chapter;
    }

    public static Lecture lecture(Chapter chapter, String title, String videoId, int durationSeconds, int orderNo) {
        Lecture lecture = newInstance(Lecture.class);
        ReflectionTestUtils.setField(lecture, "chapter", chapter);
        ReflectionTestUtils.setField(lecture, "title", title);
        ReflectionTestUtils.setField(lecture, "videoId", videoId);
        ReflectionTestUtils.setField(lecture, "durationSeconds", durationSeconds);
        ReflectionTestUtils.setField(lecture, "orderNo", orderNo);
        ReflectionTestUtils.setField(lecture, "isFreePreview", false);
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(lecture, "createdAt", now);
        ReflectionTestUtils.setField(lecture, "updatedAt", now);
        return lecture;
    }

    public static Study study(Course course, User owner, String title, LocalDate startDate, LocalDate endDate) {
        Study study = newInstance(Study.class);
        ReflectionTestUtils.setField(study, "course", course);
        ReflectionTestUtils.setField(study, "owner", owner);
        ReflectionTestUtils.setField(study, "title", title);
        ReflectionTestUtils.setField(study, "status", StudyStatus.CLOSED);
        ReflectionTestUtils.setField(study, "visibility", StudyVisibility.PUBLIC);
        ReflectionTestUtils.setField(study, "startDate", startDate);
        ReflectionTestUtils.setField(study, "endDate", endDate);
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(study, "createdAt", now);
        ReflectionTestUtils.setField(study, "updatedAt", now);
        return study;
    }

    public static StudyMember studyMember(Study study, User user, StudyMemberRole role) {
        StudyMember member = newInstance(StudyMember.class);
        ReflectionTestUtils.setField(member, "study", study);
        ReflectionTestUtils.setField(member, "user", user);
        ReflectionTestUtils.setField(member, "role", role);
        ReflectionTestUtils.setField(member, "status", StudyMemberStatus.ACTIVE);
        ReflectionTestUtils.setField(member, "joinedAt", LocalDateTime.now());
        return member;
    }
}
