package com.team08.backend.domain.course.access;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAccessContextResolver {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseAccessContext fromCourseId(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        return build(course, userId);
    }

    public CourseAccessContext fromChapterId(Long chapterId, Long userId) {
        Chapter chapter = chapterRepository.findByIdWithCourse(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));

        return build(chapter.getCourse(), userId);
    }

    public CourseAccessContext fromLectureId(Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        return build(lecture.getChapter().getCourse(), userId);
    }

    private CourseAccessContext build(Course course, Long userId) {
        Long courseId = course.getId();
        boolean hasActiveEnrollment = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, EnrollmentStatus.ACTIVE);
        boolean hasFreePreview = lectureRepository.existsByChapterCourseIdAndIsFreePreviewTrue(courseId);

        boolean isAdmin = determineAdminStatus();

        return new CourseAccessContext(
                userId,
                course.getStatus(),
                hasActiveEnrollment,
                course.getInstructorId().equals(userId),
                hasFreePreview,
                isAdmin
        );
    }

    private boolean determineAdminStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUserPrincipal principal) {
            return "ROLE_ADMIN".equals(principal.user().role());
        }
        return false;
    }
}
