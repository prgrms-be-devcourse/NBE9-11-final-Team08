package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.event.AdminCourseRejectedEvent;
import com.team08.backend.domain.course.event.CourseClosedEvent;
import com.team08.backend.domain.course.event.CourseDeletedEvent;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.domain.coursestatushistory.repository.CourseStatusHistoryRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.service.CourseStudyManager;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseStatusHistoryRepository courseStatusHistoryRepository;
    private final CourseViewCountManager courseViewCountManager;
    private final CourseStudyManager courseStudyManager;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaEncodingService mediaEncodingService;
    private final LectureRepository lectureRepository;

    @Transactional
    public Long createCourse(Long instructorId, CourseCreateRequest request) {
        Course course = request.toEntity(instructorId);
        return courseRepository.save(course).getId();
    }

    @Transactional
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findWithChaptersAndLecturesAsc(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        // TODO: 대규모 트래픽 발생 시 RDB Write 부하가 우려되므로 차후 Redis를 활용한 쓰기 지연(Write-Behind) 방식으로 고도화 필요
        try {
            courseViewCountManager.increaseViewCountRequiresNew(courseId);
        } catch (Exception e) {
            log.error("Failed to increase course view count for courseId: {}", courseId, e);
        }

        return CourseDetailResponse.from(course);
    }

    public Page<CourseCardResponse> getCourses(CourseSortType sortType, Pageable pageable) {
        Pageable pagedWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortType.getSort());
        return courseRepository.findAllByStatus(CourseStatus.ON_SALE, pagedWithSort)
                .map(CourseCardResponse::from);
    }

    @Transactional
    public void updateCourseGeneralInfo(Long courseId, Long instructorId, CourseUpdateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        course.validateOwner(instructorId);

        course.updateGeneralInfo(request);
    }

    @Transactional
    public void requestCourseReview(Long courseId, Long instructorId) {
        Course course = courseRepository.findWithChaptersAndLecturesAsc(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        course.validateOwner(instructorId);

        CourseStatusHistory history = course.requestReview(instructorId);

        courseStatusHistoryRepository.save(history);
    }

    @Transactional
    public void cancelCourseReview(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        course.validateOwner(instructorId);

        CourseStatusHistory history = course.cancelReview(instructorId);

        courseStatusHistoryRepository.save(history);
    }

    @Transactional
    public void approveCourseReview(Long courseId, Long adminId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        CourseStatusHistory history = course.approve(adminId);
        courseStatusHistoryRepository.save(history);

        CourseStudyCreateCommand command = new CourseStudyCreateCommand(
                course.getInstructorId(),
                course.getId(),
                course.getTitle(),
                course.getDescription()
        );
        courseStudyManager.createForCourse(command);
    }

    @Transactional
    public void rejectCourseReview(Long courseId, Long adminId, String reason) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        CourseStatusHistory history = course.reject(adminId, reason);
        courseStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new AdminCourseRejectedEvent(courseId));
    }

    @Transactional
    public void closeCourse(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        CourseStatusHistory history = course.close(instructorId);

        courseStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new CourseClosedEvent(courseId));

        // TODO: 일반 사용자의 신규 장바구니 담기 및 주문서 생성 차단 로직 연계 필요 (차후 장바구니/주문 도메인에서 CourseStatus.SUSPENDED 체크로 방어)
    }

    @Transactional
    public void suspendCourseByAdmin(Long courseId, Long adminId, String reason) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        CourseStatusHistory history = course.suspendByAdmin(adminId, reason);
        courseStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new CourseClosedEvent(courseId));

        // TODO: 일반 사용자의 신규 장바구니 담기 및 주문서 생성 차단 로직 연계 필요 (차후 장바구니/주문 도메인에서 CourseStatus.SUSPENDED 체크로 방어)
    }

    @Transactional
    public void deleteCourseByAdmin(Long courseId, Long adminId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        validateActiveEnrollments(courseId);

        CourseStatusHistory history = course.delete(adminId);
        courseStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new CourseDeletedEvent(courseId));
    }

    @Transactional
    public void deleteCourseByInstructor(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        course.validateOwner(instructorId);
        validateActiveEnrollments(courseId);

        CourseStatusHistory history = course.delete(instructorId);
        courseStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new CourseDeletedEvent(courseId));
    }

    private void validateActiveEnrollments(Long courseId) {
        if (enrollmentRepository.existsByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)) {
            throw new CustomException(ErrorCode.COURSE_HAS_ACTIVE_ENROLLMENTS);
        }
    }

    @Transactional
    public void uploadAndEncodeLectureVideo(Long instructorId, Long lectureId, MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("video/")) {
            throw new CustomException(ErrorCode.INVALID_VIDEO_FORMAT);
        }

        Course course = courseRepository.findByLectureId(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        course.validateOwner(instructorId);

        String targetDirName = randomUUID().toString();
        File tempSourceFile = new File(System.getProperty("java.io.tmpdir"), targetDirName + ".mp4");

        try {
            file.transferTo(tempSourceFile);
        } catch (java.io.IOException e) {
            throw new CustomException(ErrorCode.VIDEO_UPLOAD_FAILED);
        }

        mediaEncodingService.encodeToHls(tempSourceFile, targetDirName, lectureId);
    }

    @Component
    @RequiredArgsConstructor
    public static class CourseViewCountManager {

        private final CourseRepository courseRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void increaseViewCountRequiresNew(Long courseId) {
            courseRepository.increaseViewCountAtomic(courseId);
        }
    }
}