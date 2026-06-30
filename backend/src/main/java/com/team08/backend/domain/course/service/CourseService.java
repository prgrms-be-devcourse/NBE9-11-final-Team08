package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.event.CourseClosedEvent;
import com.team08.backend.domain.course.event.CourseDeletedEvent;
import com.team08.backend.domain.media.event.CourseThumbnailEvent;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.domain.coursestatushistory.repository.CourseStatusHistoryRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.service.CourseThumbnailService;
import com.team08.backend.domain.media.service.MediaEncodingService;
import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.service.CourseStudyManager;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.FileUrlFormatter;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseStatusHistoryRepository courseStatusHistoryRepository;
    private final CourseViewCountManager courseViewCountManager;
    private final CourseViewCountRedisManager courseViewCountRedisManager;
    private final CourseDetailCacheManager courseDetailCacheManager;
    private final CourseStudyManager courseStudyManager;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaEncodingService mediaEncodingService;
    private final LectureRepository lectureRepository;
    private final CourseThumbnailService courseThumbnailService;
    private final FileUrlFormatter fileUrlFormatter;

    @Transactional
    public Long createCourse(Long instructorId, CourseCreateRequest request, MultipartFile thumbnailFile) {
        Course course = request.toEntity(instructorId);
        Course savedCourse = courseRepository.save(course);

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            String newS3Key = courseThumbnailService.uploadThumbnail(savedCourse.getId(), thumbnailFile);
            savedCourse.updateThumbnail(newS3Key);
            eventPublisher.publishEvent(new CourseThumbnailEvent(savedCourse.getId(), null, newS3Key));
        }

        return savedCourse.getId();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CourseDetailResponse getCourseDetail(Long courseId) {
        CourseDetailResponse response = getCourseDetailInternal(courseId);
        if (response.status() != CourseStatus.ON_SALE) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }
        int delta = incrementViewCount(courseId);
        return response.withViewCount(response.viewCount() + delta);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CourseDetailResponse getCourseDetailForAdmin(Long courseId) {
        CourseDetailResponse response = getCourseDetailInternal(courseId);
        return response.withStatusReason(getLatestStatusReason(courseId, response.status()));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CourseDetailResponse getCourseDetailForInstructor(Long courseId, Long instructorId) {
        CourseDetailResponse response = getCourseDetailInternal(courseId);
        if (!response.instructorId().equals(instructorId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_COURSE_OWNER);
        }
        return response.withStatusReason(getLatestStatusReason(courseId, response.status()));
    }

    private int incrementViewCount(Long courseId) {
        try {
            courseViewCountRedisManager.increaseViewCount(courseId);
            return courseViewCountRedisManager.getViewCountDelta(courseId);
        } catch (Exception e) {
            log.error("Failed to process Redis view count caching for courseId: {}", courseId, e);
            courseViewCountManager.increaseViewCountRequiresNew(courseId);
            return 1;
        }
    }

    private CourseDetailResponse getCourseDetailInternal(Long courseId) {
        // 1. Redis 조회수 증가 처리 (Write-Behind)
 
        // 2. Cache-Aside 패턴으로 DTO 캐시 조회
        CourseDetailResponse cachedResponse = courseDetailCacheManager.getCache(courseId);
        if (cachedResponse != null) {
            // 캐시 히트 시 실시간 조회수만 복제하여 RDB 조회 없이 즉시 반환
            return cachedResponse;
        }
 
        // 3. Cache Miss: RDB 조회 수행
        Course course = courseRepository.findWithChaptersAsc(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        // 두 번째 fetch 로 각 챕터의 lectures 를 초기화한다(MultipleBagFetchException 회피).
        courseRepository.findChaptersWithLecturesAsc(courseId);
 
        // 4. 원본 DTO 생성 및 Redis 캐시 기록
        CourseDetailResponse originalResponse = CourseDetailResponse.from(course, fileUrlFormatter);
        courseDetailCacheManager.setCache(courseId, originalResponse);
 
        // 5. 실시간 조회수 합산 반환
        return originalResponse;
    }

    public Page<CourseCardResponse> getCourses(CourseSortType sortType, Pageable pageable) {
        Pageable pagedWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortType.getSort());
        return courseRepository.findAllByStatus(CourseStatus.ON_SALE, pagedWithSort)
                .map(course -> CourseCardResponse.from(course, fileUrlFormatter));
    }

    public Page<CourseCardResponse> getCoursesByInstructor(Long instructorId, CourseStatus status, Pageable pageable) {
        return courseRepository.findAllByInstructorIdAndStatus(instructorId, status, pageable)
                .map(course -> CourseCardResponse.from(
                        course,
                        fileUrlFormatter,
                        getLatestStatusReason(course.getId(), course.getStatus())
                ));
    }

    public Page<CourseCardResponse> getCoursesForAdmin(CourseStatus status, Pageable pageable) {
        if (status == null) {
            return courseRepository.findAll(pageable)
                    .map(course -> CourseCardResponse.from(
                            course,
                            fileUrlFormatter,
                            getLatestStatusReason(course.getId(), course.getStatus())
                    ));
        }
        return courseRepository.findAllByStatus(status, pageable)
                .map(course -> CourseCardResponse.from(
                        course,
                        fileUrlFormatter,
                        getLatestStatusReason(course.getId(), course.getStatus())
                ));
    }

    private String getLatestStatusReason(Long courseId, CourseStatus status) {
        if (status != CourseStatus.SUSPENDED && status != CourseStatus.DRAFT) {
            return null;
        }
        return courseStatusHistoryRepository.findTopByCourseIdAndToStatusAndReasonIsNotNullOrderByCreatedAtDesc(courseId, status)
                .map(CourseStatusHistory::getReason)
                .orElse(null);
    }

    @Transactional
    public void updateCourseGeneralInfo(Long courseId, Long instructorId, CourseUpdateRequest request, MultipartFile thumbnailFile) {
        Course course = courseRepository.findWithChaptersAsc(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        courseRepository.findChaptersWithLecturesAsc(courseId);

        course.validateOwner(instructorId);

        String oldThumbnail = course.getThumbnail();

        course.updateGeneralInfo(request);

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            String newS3Key = courseThumbnailService.uploadThumbnail(course.getId(), thumbnailFile);
            course.updateThumbnail(newS3Key);
            eventPublisher.publishEvent(new CourseThumbnailEvent(course.getId(), oldThumbnail, newS3Key));
        }
        
        // 변경 완료 후 캐시 무효화 처리
        courseDetailCacheManager.evictCache(courseId);
    }

    @Transactional
    public void requestCourseReview(Long courseId, Long instructorId) {
        Course course = courseRepository.findWithChaptersAsc(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        // 두 번째 fetch 로 각 챕터의 lectures 를 초기화한다(MultipleBagFetchException 회피).
        courseRepository.findChaptersWithLecturesAsc(courseId);

        course.validateOwner(instructorId);

        CourseStatusHistory history = course.requestReview(instructorId);

        courseStatusHistoryRepository.save(history);
        courseDetailCacheManager.evictCache(courseId);
    }

    @Transactional
    public void cancelCourseReview(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        course.validateOwner(instructorId);

        CourseStatusHistory history = course.cancelReview(instructorId);

        courseStatusHistoryRepository.save(history);
        courseDetailCacheManager.evictCache(courseId);
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
        courseDetailCacheManager.evictCache(courseId);
    }

    @Transactional
    public void rejectCourseReview(Long courseId, Long adminId, String reason) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        CourseStatusHistory history = course.reject(adminId, reason);
        courseStatusHistoryRepository.save(history);

        courseDetailCacheManager.evictCache(courseId);
    }

    @Transactional
    public void closeCourse(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        CourseStatusHistory history = course.close(instructorId);

        courseStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new CourseClosedEvent(courseId));
        courseDetailCacheManager.evictCache(courseId);

        // TODO: 일반 사용자의 신규 장바구니 담기 및 주문서 생성 차단 로직 연계 필요 (차후 장바구니/주문 도메인에서 CourseStatus.SUSPENDED 체크로 방어)
    }

    @Transactional
    public void suspendCourseByAdmin(Long courseId, Long adminId, String reason) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        CourseStatusHistory history = course.suspendByAdmin(adminId, reason);
        courseStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new CourseClosedEvent(courseId));
        courseDetailCacheManager.evictCache(courseId);

        // TODO: 일반 사용자의 신규 장바구니 담기 및 주문서 생성 차단 로직 연계 필요 (차후 장바구니/주문 도메인에서 CourseStatus.SUSPENDED 체크로 방어)
    }

    @Transactional
    public void deleteCourseByAdmin(Long courseId, Long adminId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        validateActiveEnrollments(courseId);

        courseThumbnailService.deleteThumbnail(course.getThumbnail());

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

        courseThumbnailService.deleteThumbnail(course.getThumbnail());

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

        File tempFile = null;
        try {
            Path tempPath = Files.createTempFile("lecture-temp-upload-", ".mp4");
            tempFile = tempPath.toFile();
            file.transferTo(tempFile);
        } catch (IOException e) {
            log.error("Failed to write multipart file to temp file", e);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            throw new CustomException(ErrorCode.VIDEO_UPLOAD_FAILED);
        }

        mediaEncodingService.encodeToHls(tempFile, targetDirName, lectureId);
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
