package com.team08.backend.domain.course.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
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
import com.team08.backend.domain.media.event.CourseThumbnailEvent;
import com.team08.backend.domain.course.fixture.CourseFixture;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.course.service.CourseService.CourseViewCountManager;
import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.domain.coursestatushistory.repository.CourseStatusHistoryRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.service.CourseThumbnailService;
import com.team08.backend.domain.media.service.MediaEncodingService;
import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.service.CourseStudyManager;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.FileUrlFormatter;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.util.ReflectionUtils.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseStatusHistoryRepository courseStatusHistoryRepository;

    @Mock
    private CourseViewCountManager courseViewCountManager;

    @Mock
    private CourseStudyManager courseStudyManager;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MediaEncodingService mediaEncodingService;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private CourseThumbnailService courseThumbnailService;

    @Mock
    private FileUrlFormatter fileUrlFormatter;

    @InjectMocks
    private CourseService courseService;

    @Test
    void 강좌를_성공적으로_생성하고_ID를_반환한다() {
        Long instructorId = 1L;
        CourseCreateRequest request = new CourseCreateRequest(
                "테스트 강좌",
                "강좌 설명입니다.",
                10L,
                15000,
                "thumbnail/path.png"
        );
        MultipartFile mockFile = new MockMultipartFile("thumbnail", "test.png", "image/png", "content".getBytes());

        Course savedCourse = CourseFixture.course(100L, instructorId, request);

        given(courseRepository.save(any(Course.class))).willReturn(savedCourse);
        given(courseThumbnailService.uploadThumbnail(eq(100L), any(MultipartFile.class))).willReturn("courses/thumbnails/100/uuid.png");

        Long courseId = courseService.createCourse(instructorId, request, mockFile);

        ArgumentCaptor<CourseThumbnailEvent> eventCaptor = ArgumentCaptor.forClass(CourseThumbnailEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        CourseThumbnailEvent publishedEvent = eventCaptor.getValue();

        assertThat(courseId).isEqualTo(100L);
        assertThat(publishedEvent.courseId()).isEqualTo(100L);
        assertThat(publishedEvent.oldThumbnail()).isNull();
        assertThat(publishedEvent.newS3Key()).isEqualTo("courses/thumbnails/100/uuid.png");
        verify(courseRepository).save(any(Course.class));
        verify(courseThumbnailService).uploadThumbnail(eq(100L), any(MultipartFile.class));
    }

    @Test
    void 강좌를_상세_조회하면_조회수가_1_증가하고_커리큘럼과_함께_반환한다() {
        Long courseId = 100L;
        Course course = TestEntityFactory.course(courseId);

        Chapter chapter = ChapterFixture.chapter(1L, "첫 번째 챕터", 1, course);
        Lecture freeLecture = LectureFixture.lecture(10L, "무료 맛보기 강의", "videos/free.m3u8", 600, 1, chapter);

        course.addChapter(chapter);
        chapter.addLecture(freeLecture);

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));

        CourseDetailResponse response = courseService.getCourseDetail(courseId);

        assertThat(response.id()).isEqualTo(courseId);
        assertThat(response.chapters()).hasSize(1);
        assertThat(response.chapters().get(0).lectures()).hasSize(1);
        verify(courseRepository).findWithChaptersAsc(courseId);
        verify(courseViewCountManager).increaseViewCountRequiresNew(courseId);
    }

    @Test
    void 무료_미리보기가_아닌_강의는_상세_조회_시_영상_주소가_노출되지_않는다() {
        Long courseId = 100L;
        Course course = TestEntityFactory.course(courseId);
        Chapter chapter = ChapterFixture.chapter(1L, "첫 번째 챕터", 1, course);

        Lecture paidLecture = Lecture.createWithStream(
                "videos/paid.m3u8",
                UUID.randomUUID().toString(),
                "유료 본 강의",
                "요약",
                1200,
                1,
                false,
                chapter
        );

        course.addChapter(chapter);
        chapter.addLecture(paidLecture);

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));

        CourseDetailResponse response = courseService.getCourseDetail(courseId);

        assertThat(response.chapters().get(0).lectures().get(0).m3u8Path()).isNull();
        verify(courseRepository).findWithChaptersAsc(courseId);
        verify(courseViewCountManager).increaseViewCountRequiresNew(courseId);
    }

    @Test
    void 존재하지_않는_강좌_ID로_상세_조회_시_예외가_발생한다() {
        Long invalidCourseId = 999L;

        given(courseRepository.findWithChaptersAsc(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseDetail(invalidCourseId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(courseRepository).findWithChaptersAsc(invalidCourseId);
        verify(courseViewCountManager, never()).increaseViewCountRequiresNew(invalidCourseId);
    }

    @Test
    void 강좌_목록을_조회하면_판매_중인_강좌만_지정된_정렬_조건과_2순위_최신순_조건으로_반환한다() {
        Course course1 = TestEntityFactory.course(1L);
        Course course2 = TestEntityFactory.course(2L);
        Page<Course> pagedCourses = new PageImpl<>(List.of(course1, course2));

        Pageable inputPageable = PageRequest.of(0, 10);
        Pageable expectedPageable = PageRequest.of(0, 10, CourseSortType.VIEW_DESC.getSort());
        given(courseRepository.findAllByStatus(eq(CourseStatus.ON_SALE), eq(expectedPageable))).willReturn(pagedCourses);

        Page<CourseCardResponse> response = courseService.getCourses(CourseSortType.VIEW_DESC, inputPageable);

        assertThat(response.getContent()).hasSize(2);
        verify(courseRepository).findAllByStatus(CourseStatus.ON_SALE, expectedPageable);
    }

    @Test
    void 강사_ID로_강좌_목록을_조회하면_해당_강사의_모든_상태의_강좌를_반환한다() {
        Long instructorId = 1L;
        Course course1 = TestEntityFactory.course(1L);
        Course course2 = TestEntityFactory.course(2L);
        Page<Course> pagedCourses = new PageImpl<>(List.of(course1, course2));

        Pageable pageable = PageRequest.of(0, 10);
        given(courseRepository.findAllByInstructorIdAndStatus(instructorId, null, pageable)).willReturn(pagedCourses);

        Page<CourseCardResponse> response = courseService.getCoursesByInstructor(instructorId, null, pageable);

        assertThat(response.getContent()).hasSize(2);
        verify(courseRepository).findAllByInstructorIdAndStatus(instructorId, null, pageable);
    }

    @Test
    void 강사_ID와_특정_상태_조건으로_강좌_목록을_조회하면_필터링된_강좌_목록만_반환한다() {
        Long instructorId = 1L;
        CourseStatus statusCondition = CourseStatus.DRAFT;
        Course draftCourse = TestEntityFactory.course(1L);
        Page<Course> pagedCourses = new PageImpl<>(List.of(draftCourse));

        Pageable pageable = PageRequest.of(0, 10);
        given(courseRepository.findAllByInstructorIdAndStatus(instructorId, statusCondition, pageable)).willReturn(pagedCourses);

        Page<CourseCardResponse> response = courseService.getCoursesByInstructor(instructorId, statusCondition, pageable);

        assertThat(response.getContent()).hasSize(1);
        verify(courseRepository).findAllByInstructorIdAndStatus(instructorId, statusCondition, pageable);
    }

    @Test
    void 강좌가_목록_조회_시_지정된_정렬_조건의_값이_동일하면_2순위인_최신순으로_정렬_조건이_체이닝된다() {
        Course course1 = TestEntityFactory.course(1L);
        Course course2 = TestEntityFactory.course(2L);
        Page<Course> pagedCourses = new PageImpl<>(List.of(course1, course2));

        given(courseRepository.findAllByStatus(eq(CourseStatus.ON_SALE), any(Pageable.class))).willReturn(pagedCourses);

        Pageable inputPageable = PageRequest.of(0, 10);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        courseService.getCourses(CourseSortType.VIEW_DESC, inputPageable);

        verify(courseRepository).findAllByStatus(eq(CourseStatus.ON_SALE), pageableCaptor.capture());
        Sort capturedSort = pageableCaptor.getValue().getSort();

        Sort.Order primaryOrder = capturedSort.getOrderFor("viewCount");
        Sort.Order secondaryOrder = capturedSort.getOrderFor("createdAt");

        assertThat(primaryOrder).isNotNull();
        assertThat(primaryOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(secondaryOrder).isNotNull();
        assertThat(secondaryOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void 존재하지_않는_강좌_ID로_수정_요청_시_예외가_발생한다() {
        Long invalidCourseId = 999L;
        Long instructorId = 1L;
        CourseUpdateRequest request = new CourseUpdateRequest("제목", "설명", 2L, 20000, "thumb.png", List.of());
        MultipartFile mockFile = new MockMultipartFile("thumbnail", "test.png", "image/png", "content".getBytes());

        given(courseRepository.findWithChaptersAsc(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourseGeneralInfo(invalidCourseId, instructorId, request, mockFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    void 소유자가_아닌_사용자가_강좌_수정_요청_시_예외가_발생한다() {
        Long courseId = 100L;
        Long hackerId = 999L;
        Course course = Course.createDraft(
                1L,
                2L,
                "원래 제목",
                "원래 설명",
                "old.png",
                10000
        );
        CourseUpdateRequest request = new CourseUpdateRequest("변경 제목", "변경 설명", 3L, 20000, "new.png", List.of());
        MultipartFile mockFile = new MockMultipartFile("thumbnail", "test.png", "image/png", "content".getBytes());

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));
        given(courseRepository.findChaptersWithLecturesAsc(courseId)).willReturn(List.of());

        assertThatThrownBy(() -> courseService.updateCourseGeneralInfo(courseId, hackerId, request, mockFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_COURSE_OWNER.getMessage())
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_COURSE_OWNER);
    }

    @Test
    void 정상_소유자가_요청하면_강좌_정보와_하위_커리큘럼이_모두_연쇄_동기화되어_수정되고_트랜잭션_중_S3_업로드_및_물리_정합성_이벤트가_발행된다() {
        Long courseId = 100L;
        Long instructorId = 1L;

        Course course = Course.createDraft(
                instructorId,
                2L,
                "원래 제목",
                "원래 설명",
                "old.png",
                10000
        );

        Field courseIdField = findField(Course.class, "id");
        makeAccessible(courseIdField);
        setField(courseIdField, course, courseId);

        Chapter chapter = Chapter.create("원래 챕터", 1, course);
        Field chapterIdField = findField(Chapter.class, "id");
        makeAccessible(chapterIdField);
        setField(chapterIdField, chapter, 10L);

        Lecture lecture = Lecture.createWithStream("vid.m3u8", UUID.randomUUID().toString(), "원래 강의", "", 300, 1, false, chapter);
        Field lectureIdField = findField(Lecture.class, "id");
        makeAccessible(lectureIdField);
        setField(lectureIdField, lecture, 20L);

        chapter.addLecture(lecture);
        course.addChapter(chapter);

        CourseUpdateRequest.LectureUpdateRequest lectureUpdate = new CourseUpdateRequest.LectureUpdateRequest(20L, "수정 강의", "요약", 400, 1, true);
        CourseUpdateRequest.LectureUpdateRequest lectureNew = new CourseUpdateRequest.LectureUpdateRequest(null, "신규 강의", "", 500, 2, false);
        CourseUpdateRequest.ChapterUpdateRequest chapterUpdate = new CourseUpdateRequest.ChapterUpdateRequest(10L, "수정 챕터", 1, List.of(lectureUpdate, lectureNew));
        CourseUpdateRequest request = new CourseUpdateRequest("수정 제목", "수정 설명", 5L, 50000, "new.png", List.of(chapterUpdate));
        MultipartFile mockFile = new MockMultipartFile("thumbnail", "test.png", "image/png", "content".getBytes());

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));
        given(courseRepository.findChaptersWithLecturesAsc(courseId)).willReturn(List.of(chapter));
        given(courseThumbnailService.uploadThumbnail(eq(courseId), any(MultipartFile.class))).willReturn("courses/thumbnails/100/new-uuid.png");

        courseService.updateCourseGeneralInfo(courseId, instructorId, request, mockFile);

        ArgumentCaptor<CourseThumbnailEvent> eventCaptor = ArgumentCaptor.forClass(CourseThumbnailEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        CourseThumbnailEvent publishedEvent = eventCaptor.getValue();

        assertThat(course.getTitle()).isEqualTo("수정 제목");
        assertThat(course.getThumbnail()).isEqualTo("courses/thumbnails/100/new-uuid.png");
        assertThat(publishedEvent.courseId()).isEqualTo(courseId);
        assertThat(publishedEvent.oldThumbnail()).isEqualTo("old.png");
        assertThat(publishedEvent.newS3Key()).isEqualTo("courses/thumbnails/100/new-uuid.png");
        verify(courseThumbnailService).uploadThumbnail(eq(courseId), any(MultipartFile.class));
    }

    @Test
    void 존재하지_않는_강좌_ID로_심사_요청_시_예외가_발생한다() {
        Long invalidCourseId = 999L;
        Long instructorId = 1L;

        given(courseRepository.findWithChaptersAsc(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.requestCourseReview(invalidCourseId, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    void 소유자가_아닌_사용자가_심사_요청_시_예외가_발생한다() {
        Long courseId = 100L;
        Long hackerId = 999L;
        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.requestCourseReview(courseId, hackerId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_COURSE_OWNER.getMessage());
    }

    @Test
    void DRAFT_상태가_아닌_강좌_심사_요청_시_예외가_발생한다() {
        Long courseId = 100L;
        Long instructorId = 1L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.requestCourseReview(courseId, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_COURSE_STATUS_TRANSITION.getMessage());
    }

    @Test
    void 커리큘럼_조건_미달_시_심사_요청하면_예외가_발생한다() {
        Long courseId = 100L;
        Long instructorId = 1L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.requestCourseReview(courseId, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_CURRICULUM_EMPTY.getMessage());
    }

    @Test
    void 정상_조건을_충족하면_심사_요청_상태로_전이되고_이력이_남는다() {
        Long courseId = 100L;
        Long instructorId = 1L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Chapter chapter = Chapter.create("챕터", 1, course);
        Lecture lecture = Lecture.createWithStream("path.m3u8", UUID.randomUUID().toString(), "강의", "", 300, 1, false, chapter);
        chapter.addLecture(lecture);
        course.addChapter(chapter);

        given(courseRepository.findWithChaptersAsc(courseId)).willReturn(Optional.of(course));

        courseService.requestCourseReview(courseId, instructorId);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.IN_REVIEW);
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
    }

    @Test
    void 존재하지_않는_강좌_ID로_심사_취소_시_예외가_발생한다() {
        Long invalidCourseId = 999L;
        Long instructorId = 1L;

        given(courseRepository.findById(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.cancelCourseReview(invalidCourseId, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    void 소유자가_아닌_사용자가_심사_취소_시_예외가_발생한다() {
        Long courseId = 100L;
        Long hackerId = 999L;
        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.IN_REVIEW);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.cancelCourseReview(courseId, hackerId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_COURSE_OWNER.getMessage());
    }

    @Test
    void IN_REVIEW_상태가_아닌_강좌_심사_취소_시_예외가_발생한다() {
        Long courseId = 100L;
        Long instructorId = 1L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.cancelCourseReview(courseId, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_COURSE_STATUS_TRANSITION.getMessage());
    }

    @Test
    void 정상_조건을_충족하면_심사_취소_상태로_전이되고_이력이_남는다() {
        Long courseId = 100L;
        Long instructorId = 1L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.IN_REVIEW);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        courseService.cancelCourseReview(courseId, instructorId);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
    }

    @Test
    void 심사_중이_아닌_강좌를_승인_요청_시_예외가_발생한다() {
        Long courseId = 100L;
        Long adminId = 999L;
        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.approveCourseReview(courseId, adminId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_COURSE_STATUS_TRANSITION.getMessage());
    }

    @Test
    void 정상_조건을_충족하면_심사_승인_상태로_전이되고_이력이_남는다() {
        Long courseId = 100L;
        Long adminId = 999L;
        Course course = Course.createDraft(1L, 0L, "테스트 제목", "테스트 설명", "thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.IN_REVIEW);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(courseStudyManager.createForCourse(any(CourseStudyCreateCommand.class))).willReturn(1L);

        courseService.approveCourseReview(courseId, adminId);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.ON_SALE);
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
        verify(courseStudyManager).createForCourse(any(CourseStudyCreateCommand.class));
    }

    @Test
    void 심사_중이_아닌_강좌를_반려_요청_시_예외가_발생한다() {
        Long courseId = 100L;
        Long adminId = 999L;
        String reason = "콘텐츠 부적절";
        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.rejectCourseReview(courseId, adminId, reason))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_COURSE_STATUS_TRANSITION.getMessage());
    }

    @Test
    void 강좌_심사_반려_시_사유가_누락되면_예외가_발생한다() {
        Long courseId = 100L;
        Long adminId = 999L;
        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.IN_REVIEW);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.rejectCourseReview(courseId, adminId, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.REJECT_REASON_REQUIRED.getMessage());
    }

    @Test
    void 정상_조건을_충족하면_심사_반려_상태로_전이되고_이력_저장_및_반려_이벤트가_발행된다() {
        Long courseId = 100L;
        Long adminId = 999L;
        String reason = "콘텐츠 부적절";
        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.IN_REVIEW);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        courseService.rejectCourseReview(courseId, adminId, reason);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.SUSPENDED);
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
        verify(eventPublisher).publishEvent(any(AdminCourseRejectedEvent.class));
    }

    @Test
    void 존재하지_않는_강좌_ID로_판매_중지_요청_시_예외가_발생한다() {
        Long invalidCourseId = 999L;
        Long instructorId = 1L;

        given(courseRepository.findById(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.closeCourse(invalidCourseId, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    void 소유자가_아닌_사용자가_강좌_판매_중지_요청_시_예외가_발생한다() {
        Long courseId = 100L;
        Long hackerId = 999L;
        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.closeCourse(courseId, hackerId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_COURSE_OWNER.getMessage());
    }

    @Test
    void 정상_조건을_충족하면_강좌가_판매_중지_상태로_전이되고_강좌_폐쇄_이벤트가_발행된다() {
        Long courseId = 100L;
        Long instructorId = 1L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        courseService.closeCourse(courseId, instructorId);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.SUSPENDED);
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
        verify(eventPublisher).publishEvent(any(CourseClosedEvent.class));
    }

    @Test
    void 존재하지_않는_강좌_ID로_관리자가_강제_판매_중지_요청_시_예외가_발생한다() {
        Long invalidCourseId = 999L;
        Long adminId = 1L;
        String reason = "운영 정책 위반";

        given(courseRepository.findById(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.suspendCourseByAdmin(invalidCourseId, adminId, reason))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    void Administration_이_강제_판매_중지_요청_시_중지_사유가_누락되면_예외가_발생한다() {
        Long courseId = 100L;
        Long adminId = 1L;
        Course course = Course.createDraft(10L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.suspendCourseByAdmin(courseId, adminId, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.REJECT_REASON_REQUIRED.getMessage());
    }

    @Test
    void 정상_조건을_충족하면_관리자에_의해_강좌가_강제_판매_중지_상태로_전이되고_공통_폐쇄_이벤트가_발행된다() {
        Long courseId = 100L;
        Long adminId = 1L;
        String reason = "운영 정책 위반";
        Course course = Course.createDraft(10L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        courseService.suspendCourseByAdmin(courseId, adminId, reason);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.SUSPENDED);
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
        verify(eventPublisher).publishEvent(any(CourseClosedEvent.class));
    }

    @Test
    void 관리자가_삭제_요청_시_활성_수강생이_존재하면_예외가_발생한다() {
        Long courseId = 100L;
        Long adminId = 1L;
        Course course = Course.createDraft(10L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> courseService.deleteCourseByAdmin(courseId, adminId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_HAS_ACTIVE_ENROLLMENTS.getMessage());

        verify(courseStatusHistoryRepository, never()).save(any(CourseStatusHistory.class));
        verify(eventPublisher, never()).publishEvent(any(CourseDeletedEvent.class));
    }

    @Test
    void admin이_정상적으로_강좌를_삭제하면_DELETED_상태로_전이되고_기존_썸네일이_S3에서_삭제된다() {
        Long courseId = 100L;
        Long adminId = 1L;
        Course course = Course.createDraft(10L, 0L, "제목", "설명", "courses/thumbnails/100/thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)).willReturn(false);

        courseService.deleteCourseByAdmin(courseId, adminId);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DELETED);
        verify(courseThumbnailService).deleteThumbnail("courses/thumbnails/100/thumb.jpg");
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
        verify(eventPublisher).publishEvent(any(CourseDeletedEvent.class));
    }

    @Test
    void 판매자가_삭제_요청_시_소유자가_아닌_예외가_발생한다() {
        Long courseId = 100L;
        Long hackerId = 999L;
        Course course = Course.createDraft(10L, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.deleteCourseByInstructor(courseId, hackerId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_COURSE_OWNER.getMessage());

        verify(enrollmentRepository, never()).existsByCourseIdAndStatus(any(Long.class), any(EnrollmentStatus.class));
    }

    @Test
    void 판매자가_삭제_요청_시_활성_수강생이_존재하면_예외가_발생한다() {
        Long courseId = 100L;
        Long instructorId = 10L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);
        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> courseService.deleteCourseByInstructor(courseId, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_HAS_ACTIVE_ENROLLMENTS.getMessage());

        verify(courseStatusHistoryRepository, never()).save(any(CourseStatusHistory.class));
        verify(eventPublisher, never()).publishEvent(any(CourseDeletedEvent.class));
    }

    @Test
    void 판매자가_정상적으로_강좌를_삭제하면_DELETED_상태로_전이되고_기존_썸네일이_S3에서_삭제된다() {
        Long courseId = 100L;
        Long instructorId = 10L;
        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "courses/thumbnails/100/thumb.jpg", 0);
        Field idField = findField(Course.class, "id");
        makeAccessible(idField);
        setField(idField, course, courseId);

        Field statusField = findField(Course.class, "status");
        makeAccessible(statusField);
        setField(statusField, course, CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)).willReturn(false);

        courseService.deleteCourseByInstructor(courseId, instructorId);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DELETED);
        verify(courseThumbnailService).deleteThumbnail("courses/thumbnails/100/thumb.jpg");
        verify(courseStatusHistoryRepository).save(any(CourseStatusHistory.class));
        verify(eventPublisher).publishEvent(any(CourseDeletedEvent.class));
    }

    @Test
    void 올바르지_않은_비디오_포맷_업로드_요청_시_예외가_발생한다() {
        Long instructorId = 1L;
        Long lectureId = 10L;
        MultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "invalid content".getBytes()
        );

        assertThatThrownBy(() -> courseService.uploadAndEncodeLectureVideo(instructorId, lectureId, mockFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_VIDEO_FORMAT.getMessage());

        verify(courseRepository, never()).findByLectureId(any(Long.class));
        verify(mediaEncodingService, never()).encodeToHls(any(java.io.File.class), any(String.class), any(Long.class));
    }

    @Test
    void 존재하지_않는_강의_ID로_비디오_업로드_요청_시_예외가_발생한다() {
        Long instructorId = 1L;
        Long invalidLectureId = 999L;
        MultipartFile mockFile = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "video data".getBytes()
        );

        given(courseRepository.findByLectureId(invalidLectureId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.uploadAndEncodeLectureVideo(instructorId, invalidLectureId, mockFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(mediaEncodingService, never()).encodeToHls(any(java.io.File.class), any(String.class), any(Long.class));
    }

    @Test
    void 강좌_소유자가_아닌_사용자가_비디오_업로드_요청_시_예외가_발생한다() {
        Long hackerId = 999L;
        Long lectureId = 10L;
        MultipartFile mockFile = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "video data".getBytes()
        );

        Course course = Course.createDraft(1L, 0L, "제목", "설명", "thumb.jpg", 0);

        given(courseRepository.findByLectureId(lectureId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.uploadAndEncodeLectureVideo(hackerId, lectureId, mockFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_COURSE_OWNER.getMessage());

        verify(mediaEncodingService, never()).encodeToHls(any(java.io.File.class), any(String.class), any(Long.class));
    }

    @Test
    void 소유권_검증을_통과하면_HLS_인코딩_서비스를_성공적으로_트리거한다() {
        Long instructorId = 1L;
        Long lectureId = 10L;
        MultipartFile mockFile = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "video data".getBytes()
        );

        Course course = Course.createDraft(instructorId, 0L, "제목", "설명", "thumb.jpg", 0);

        given(courseRepository.findByLectureId(lectureId)).willReturn(Optional.of(course));

        courseService.uploadAndEncodeLectureVideo(instructorId, lectureId, mockFile);

        verify(courseRepository).findByLectureId(lectureId);

        ArgumentCaptor<java.io.File> fileCaptor = ArgumentCaptor.forClass(java.io.File.class);
        verify(mediaEncodingService).encodeToHls(fileCaptor.capture(), any(String.class), eq(lectureId));

        java.io.File tempFile = fileCaptor.getValue();
        assertThat(tempFile).exists();
        tempFile.delete();
    }

    @Test
    void 유효하지_않은_비디오_확장자_업로드_요청_시_예외가_발생한다() {
        Long instructorId = 1L;
        Long lectureId = 10L;
        MultipartFile mockFile = new MockMultipartFile(
                "file", "hacker.txt", "text/plain", "invalid_content".getBytes()
        );

        assertThatThrownBy(() -> courseService.uploadAndEncodeLectureVideo(instructorId, lectureId, mockFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_VIDEO_FORMAT.getMessage());

        verify(mediaEncodingService, never()).encodeToHls(any(java.io.File.class), any(String.class), any(Long.class));
    }
}