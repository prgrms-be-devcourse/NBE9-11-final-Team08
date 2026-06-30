package com.team08.backend.domain.enrollment.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.dto.EnrolledCourseResponse;
import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.team08.backend.domain.studyreport.repository.StudyReportRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudyRepository studyRepository;
    private final StudyReportRepository studyReportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean hasActiveEnrollment(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<EnrolledCourseResponse> getMyActiveCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findAllByUserIdAndStatusOrderByEnrolledAtDescIdDesc(userId, EnrollmentStatus.ACTIVE);
        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .distinct()
                .toList();

        List<Course> courses = new java.util.ArrayList<>();
        courseRepository.findAllById(courseIds).forEach(courses::add);
        Map<Long, Course> courseById = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        Map<Long, Study> studyByCourseId = studyRepository
                .findByCourseIdInAndStatusIn(courseIds, List.of(StudyStatus.ACTIVE, StudyStatus.READONLY))
                .stream()
                .collect(Collectors.toMap(study -> study.getCourse().getId(), Function.identity()));

        List<Long> studyIds = studyByCourseId.values().stream()
                .map(Study::getId)
                .toList();
        Map<Long, StudyReport> reportByStudyId = studyIds.isEmpty()
                ? Map.of()
                : studyReportRepository.findByUserIdAndStudyIdIn(userId, studyIds).stream()
                        .collect(Collectors.toMap(StudyReport::getStudyId, Function.identity()));

        List<Long> instructorIds = courseById.values().stream()
                .map(Course::getInstructorId)
                .distinct()
                .toList();
        List<User> instructors = new java.util.ArrayList<>();
        if (!instructorIds.isEmpty()) {
            userRepository.findAllById(instructorIds).forEach(instructors::add);
        }
        Map<Long, String> nicknameByUserId = instructors.stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return enrollments.stream()
                .map(enrollment -> toResponse(
                        enrollment,
                        courseById.get(enrollment.getCourseId()),
                        studyByCourseId.get(enrollment.getCourseId()),
                        reportByStudyId,
                        nicknameByUserId
                ))
                .filter(response -> response != null)
                .toList();
    }

    private EnrolledCourseResponse toResponse(
            Enrollment enrollment,
            Course course,
            Study study,
            Map<Long, StudyReport> reportByStudyId,
            Map<Long, String> nicknameByUserId
    ) {
        if (course == null) {
            return null;
        }

        Long studyId = study == null ? null : study.getId();
        StudyReport report = studyId == null ? null : reportByStudyId.get(studyId);
        int completedLectures = report == null || report.getCompletedLectures() == null
                ? 0 : report.getCompletedLectures();
        int totalLectures = report == null || report.getTotalLectures() == null
                ? 0 : report.getTotalLectures();
        int progressRate = totalLectures == 0
                ? 0 : (int) Math.round((double) completedLectures * 100 / totalLectures);

        return new EnrolledCourseResponse(
                enrollment.getId(),
                course.getId(),
                studyId,
                course.getTitle(),
                nicknameByUserId.getOrDefault(course.getInstructorId(), "(알 수 없음)"),
                course.getThumbnail(),
                progressRate,
                completedLectures,
                totalLectures,
                enrollment.getEnrolledAt()
        );
    }
}
