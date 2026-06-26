package com.team08.backend.domain.learningevent.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsProjection;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.dto.RecordLearningEventRequest;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LearningEventServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    @Mock
    private LearningEventRepository learningEventRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LearningEventService learningEventService;


    // ──── 이벤트 기록 ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("강의 입장 이벤트 기록 성공")
    void recordEvent_lectureEnter_success() {
        Long userId = 1L;
        RecordLearningEventRequest request = lectureEnterRequest(10L, "client-key-001");
        LearningEvent saved = savedEvent(1L, userId, request);

        given(learningEventRepository.existsByUniqueEventKey("client-key-001")).willReturn(false);
        given(learningEventRepository.save(any())).willReturn(saved);

        LearningEventResponse response = learningEventService.recordEvent(userId, request);

        assertThat(response.eventType()).isEqualTo(LearningEventType.LECTURE_ENTER);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.lectureId()).isEqualTo(10L);

        ArgumentCaptor<LearningEventRecorded> eventCaptor = ArgumentCaptor.forClass(LearningEventRecorded.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().learningEventId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(LearningEventType.LECTURE_ENTER);
    }

    @Test
    @DisplayName("eventKey 미제공 시 서버가 UUID 자동 생성")
    void recordEvent_noEventKey_serverGeneratesKey() {
        Long userId = 1L;
        RecordLearningEventRequest request = lectureEnterRequest(10L, null);
        LearningEvent saved = savedEvent(1L, userId, request);

        given(learningEventRepository.existsByUniqueEventKey(anyString())).willReturn(false);
        given(learningEventRepository.save(any())).willReturn(saved);

        LearningEventResponse response = learningEventService.recordEvent(userId, request);

        assertThat(response).isNotNull();
        verify(learningEventRepository).save(any());
    }


    // ──── 이벤트 시각 검증 (skew → 발행 여부 / KST 변환) ─────────────────────────

    @Test
    @DisplayName("입장 이벤트 - 허용 오차를 크게 벗어나면 적재만 하고 도메인 이벤트는 발행하지 않는다")
    void recordEvent_lectureEnter_largeSkew_recordedButNotPublished() {
        Long userId = 1L;
        OffsetDateTime farPast = OffsetDateTime.now(KST_OFFSET).minusDays(1); // 5분 초과
        RecordLearningEventRequest request = new RecordLearningEventRequest(
                1L, 2L, 10L, LearningEventType.LECTURE_ENTER, null, farPast, "enter-skew");

        given(learningEventRepository.existsByUniqueEventKey("enter-skew")).willReturn(false);
        given(learningEventRepository.save(any())).willReturn(savedEvent(1L, userId, request));

        learningEventService.recordEvent(userId, request);

        // 적재는 하되 시각은 보정하지 않고 클라이언트 원본(KST 변환)을 그대로 저장한다.
        ArgumentCaptor<LearningEvent> captor = ArgumentCaptor.forClass(LearningEvent.class);
        verify(learningEventRepository).save(captor.capture());
        LocalDateTime expectedKst = farPast.atZoneSameInstant(KST).toLocalDateTime();
        assertThat(captor.getValue().getEventTime()).isEqualTo(expectedKst);
        // 도메인 이벤트는 발행하지 않는다(후속 반응 차단).
        verify(eventPublisher, never()).publishEvent(any(LearningEventRecorded.class));
    }

    @Test
    @DisplayName("퇴장 이벤트 - 허용 오차를 크게 벗어나면 적재만 하고 도메인 이벤트는 발행하지 않는다")
    void recordEvent_lectureExit_largeSkew_recordedButNotPublished() {
        Long userId = 1L;
        OffsetDateTime farFuture = OffsetDateTime.now(KST_OFFSET).plusDays(1); // 미래로 조작
        RecordLearningEventRequest request = new RecordLearningEventRequest(
                1L, 2L, 10L, LearningEventType.LECTURE_EXIT, 300, farFuture, "exit-skew");

        given(learningEventRepository.existsByUniqueEventKey("exit-skew")).willReturn(false);
        given(learningEventRepository.save(any())).willReturn(savedEvent(1L, userId, request));

        learningEventService.recordEvent(userId, request);

        ArgumentCaptor<LearningEvent> captor = ArgumentCaptor.forClass(LearningEvent.class);
        verify(learningEventRepository).save(captor.capture());
        LocalDateTime expectedKst = farFuture.atZoneSameInstant(KST).toLocalDateTime();
        assertThat(captor.getValue().getEventTime()).isEqualTo(expectedKst);
        verify(eventPublisher, never()).publishEvent(any(LearningEventRecorded.class));
    }

    @Test
    @DisplayName("입장 이벤트 - 허용 오차 이내면 KST 변환 후 그대로 적재하고 발행한다")
    void recordEvent_lectureEnter_withinSkew_keepsClientTimeAndPublishes() {
        Long userId = 1L;
        OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC); // 현재 시각, UTC 오프셋
        RecordLearningEventRequest request = new RecordLearningEventRequest(
                1L, 2L, 10L, LearningEventType.LECTURE_ENTER, null, nowUtc, "enter-ok");

        given(learningEventRepository.existsByUniqueEventKey("enter-ok")).willReturn(false);
        given(learningEventRepository.save(any())).willReturn(savedEvent(1L, userId, request));

        learningEventService.recordEvent(userId, request);

        ArgumentCaptor<LearningEvent> captor = ArgumentCaptor.forClass(LearningEvent.class);
        verify(learningEventRepository).save(captor.capture());
        // UTC 입력이 KST(+9h) 로 변환된 값이 그대로 저장된다.
        LocalDateTime expectedKst = nowUtc.atZoneSameInstant(KST).toLocalDateTime();
        assertThat(captor.getValue().getEventTime()).isEqualTo(expectedKst);
        // 신뢰 범위 안이므로 도메인 이벤트도 발행된다.
        verify(eventPublisher).publishEvent(any(LearningEventRecorded.class));
    }

    @Test
    @DisplayName("입장/퇴장 외 이벤트 - 오차가 커도 KST 변환만 하고 항상 발행한다")
    void recordEvent_videoStart_largeSkew_alwaysPublished() {
        Long userId = 2L;
        OffsetDateTime farPastUtc = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        RecordLearningEventRequest request = new RecordLearningEventRequest(
                1L, 2L, 10L, LearningEventType.VIDEO_START, 120, farPastUtc, "video-skew");

        given(learningEventRepository.existsByUniqueEventKey("video-skew")).willReturn(false);
        given(learningEventRepository.save(any())).willReturn(savedEvent(2L, userId, request));

        learningEventService.recordEvent(userId, request);

        ArgumentCaptor<LearningEvent> captor = ArgumentCaptor.forClass(LearningEvent.class);
        verify(learningEventRepository).save(captor.capture());
        // VIDEO_START 는 신뢰성 판정 대상이 아니므로 하루 전 시각이 보존되고 발행된다(KST 변환만).
        LocalDateTime expectedKst = farPastUtc.atZoneSameInstant(KST).toLocalDateTime();
        assertThat(captor.getValue().getEventTime()).isEqualTo(expectedKst);
        verify(eventPublisher).publishEvent(any(LearningEventRecorded.class));
    }


    // ──── 중복 이벤트 방지 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("동일한 eventKey 재전송 시 중복 이벤트 예외 발생")
    void recordEvent_duplicateKey_throwsException() {
        RecordLearningEventRequest request = lectureEnterRequest(10L, "dup-key");

        given(learningEventRepository.existsByUniqueEventKey("dup-key")).willReturn(true);

        assertThatThrownBy(() -> learningEventService.recordEvent(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_LEARNING_EVENT);

        verify(learningEventRepository, never()).save(any());
    }

    // ──── 사용자별 활동 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("본인 활동 조회 성공")
    void getUserActivities_self_success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        LearningEvent event = savedEvent(1L, userId, lectureEnterRequest(10L, "k1"));
        given(learningEventRepository.findByUserId(userId, pageable))
                .willReturn(new PageImpl<>(List.of(event)));

        Page<LearningEventResponse> result =
                learningEventService.getUserActivities(userId, userId, "ROLE_USER", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("관리자가 다른 사용자 활동 조회 성공")
    void getUserActivities_admin_canViewOthers() {
        Long adminId = 99L;
        Long targetUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        given(learningEventRepository.findByUserId(targetUserId, pageable))
                .willReturn(Page.empty());

        Page<LearningEventResponse> result =
                learningEventService.getUserActivities(adminId, targetUserId, "ROLE_ADMIN", pageable);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("타인 활동 조회 시 권한 없음 예외 발생")
    void getUserActivities_otherUser_accessDenied() {
        assertThatThrownBy(() ->
                learningEventService.getUserActivities(1L, 2L, "ROLE_USER", PageRequest.of(0, 10)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);

        verify(learningEventRepository, never()).findByUserId(anyLong(), any());
    }

    // ──── 강의별 통계 집계 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자가 강의별 통계 조회 성공")
    void getCourseStats_admin_success() {
        Long courseId = 10L;

        given(learningEventRepository.getStatsByCourseId(courseId))
                .willReturn(new CourseStatsProjection(50L, 36000L, 30L));

        CourseStatsResponse stats =
                learningEventService.getCourseStats(99L, courseId, "ROLE_ADMIN");

        assertThat(stats.courseId()).isEqualTo(courseId);
        assertThat(stats.enterCount()).isEqualTo(50L);
        assertThat(stats.watchTimeSeconds()).isEqualTo(36000L);
        assertThat(stats.completionCount()).isEqualTo(30L);
        // 단일 쿼리로 집계됐는지 확인
        verify(learningEventRepository).getStatsByCourseId(courseId);
    }

    @Test
    @DisplayName("강좌 소유 판매자가 강의별 통계 조회 성공")
    void getCourseStats_courseOwner_success() {
        Long sellerId = 5L;
        Long courseId = 10L;
        Course course = mockCourse(courseId, sellerId);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(learningEventRepository.getStatsByCourseId(courseId))
                .willReturn(new CourseStatsProjection(5L, 1000L, 3L));

        CourseStatsResponse stats =
                learningEventService.getCourseStats(sellerId, courseId, "ROLE_SELLER");

        assertThat(stats.courseId()).isEqualTo(courseId);
    }

    @Test
    @DisplayName("강좌 소유자가 아닌 판매자는 통계 조회 불가")
    void getCourseStats_nonOwnerSeller_accessDenied() {
        Long otherId = 9L;
        Long courseId = 10L;
        Course course = mockCourse(courseId, 5L);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() ->
                learningEventService.getCourseStats(otherId, courseId, "ROLE_SELLER"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("일반 유저는 강의별 통계 조회 불가")
    void getCourseStats_regularUser_accessDenied() {
        Long courseId = 10L;
        Course course = mockCourse(courseId, 5L);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() ->
                learningEventService.getCourseStats(1L, courseId, "ROLE_USER"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
    }

    // ──── 챕터별 통계 집계 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자가 챕터별 통계 조회 성공")
    void getChapterStats_admin_success() {
        Long chapterId = 20L;

        given(learningEventRepository.countByChapterIdAndEventType(chapterId, LearningEventType.LECTURE_ENTER)).willReturn(40L);
        given(learningEventRepository.countByChapterIdAndEventType(chapterId, LearningEventType.LECTURE_COMPLETE)).willReturn(20L);
        given(learningEventRepository.avgWatchTimeSecondsByChapterId(chapterId)).willReturn(600.0);

        ChapterStatsResponse stats =
                learningEventService.getChapterStats(99L, chapterId, "ROLE_ADMIN");

        assertThat(stats.chapterId()).isEqualTo(chapterId);
        assertThat(stats.enterCount()).isEqualTo(40L);
        assertThat(stats.completionCount()).isEqualTo(20L);
        assertThat(stats.avgWatchTimeSeconds()).isEqualTo(600L);
    }

    @Test
    @DisplayName("판매자가 챕터별 통계 조회 성공")
    void getChapterStats_seller_success() {
        Long chapterId = 20L;

        given(learningEventRepository.countByChapterIdAndEventType(eq(chapterId), any())).willReturn(5L);
        given(learningEventRepository.avgWatchTimeSecondsByChapterId(chapterId)).willReturn(300.0);

        ChapterStatsResponse stats =
                learningEventService.getChapterStats(5L, chapterId, "ROLE_SELLER");

        assertThat(stats.chapterId()).isEqualTo(chapterId);
    }

    @Test
    @DisplayName("일반 유저는 챕터별 통계 조회 불가")
    void getChapterStats_regularUser_accessDenied() {
        assertThatThrownBy(() ->
                learningEventService.getChapterStats(1L, 20L, "ROLE_USER"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);

        verify(learningEventRepository, never()).countByChapterIdAndEventType(anyLong(), any());
    }

    // ──── 관리자 전체 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자 전체 이벤트 조회 성공")
    void getAllEvents_admin_success() {
        Pageable pageable = PageRequest.of(0, 50);
        given(learningEventRepository.findAll(pageable)).willReturn(Page.empty());

        Page<LearningEventResponse> result =
                learningEventService.getAllEvents("ROLE_ADMIN", pageable);

        assertThat(result).isEmpty();
        verify(learningEventRepository).findAll(pageable);
    }

    @Test
    @DisplayName("비관리자는 전체 이벤트 조회 불가")
    void getAllEvents_nonAdmin_accessDenied() {
        assertThatThrownBy(() ->
                learningEventService.getAllEvents("ROLE_SELLER", PageRequest.of(0, 50)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);

        verify(learningEventRepository, never()).findAll(any(Pageable.class));
    }

    // ──── 판매자 강좌 필터링 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("판매자가 자신의 강좌 이벤트 조회 성공")
    void getSellerEvents_seller_success() {
        Long sellerId = 5L;
        Pageable pageable = PageRequest.of(0, 20);
        List<Long> courseIds = List.of(10L, 11L);

        given(courseRepository.findIdsByInstructorId(sellerId)).willReturn(courseIds);
        given(learningEventRepository.findByCourseIdIn(courseIds, pageable))
                .willReturn(Page.empty());

        Page<LearningEventResponse> result =
                learningEventService.getSellerEvents(sellerId, "ROLE_SELLER", pageable);

        assertThat(result).isEmpty();
        verify(learningEventRepository).findByCourseIdIn(courseIds, pageable);
    }

    @Test
    @DisplayName("판매자 강좌가 없으면 빈 페이지 반환")
    void getSellerEvents_noCourses_returnsEmpty() {
        Long sellerId = 5L;
        Pageable pageable = PageRequest.of(0, 20);

        given(courseRepository.findIdsByInstructorId(sellerId)).willReturn(List.of());

        Page<LearningEventResponse> result =
                learningEventService.getSellerEvents(sellerId, "ROLE_SELLER", pageable);

        assertThat(result).isEmpty();
        verify(learningEventRepository, never()).findByCourseIdIn(any(), any());
    }

    @Test
    @DisplayName("일반 유저는 판매자 이벤트 조회 불가")
    void getSellerEvents_regularUser_accessDenied() {
        assertThatThrownBy(() ->
                learningEventService.getSellerEvents(1L, "ROLE_USER", PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);

        verify(courseRepository, never()).findIdsByInstructorId(anyLong());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixtures
    // ─────────────────────────────────────────────────────────────────────────

    private RecordLearningEventRequest lectureEnterRequest(Long lectureId, String eventKey) {
        return new RecordLearningEventRequest(
                1L, 2L, lectureId,
                LearningEventType.LECTURE_ENTER,
                null,
                OffsetDateTime.now(KST_OFFSET), // 허용 오차 이내 → 정상 발행 경로
                eventKey
        );
    }

    private LearningEvent savedEvent(Long id, Long userId, RecordLearningEventRequest req) {
        LearningEvent event = LearningEvent.create(
                userId,
                req.courseId(),
                req.chapterId(),
                req.lectureId(),
                req.eventType(),
                req.positionSeconds(),
                req.eventTime().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime(),
                req.eventKey() != null ? req.eventKey() : "generated-key"
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private Course mockCourse(Long courseId, Long instructorId) {
        // 리팩토링 포인트: 빌더 및 public 생성자 제거 대응으로 createDraft 정적 팩토리 메서드 도입
        Course course = Course.createDraft(
                instructorId,
                1L,
                "테스트 강좌",
                "설명",
                "thumbnail.jpg",
                50000
        );
        // 테스트 시나리오 충족을 위해 status 상태 필드를 리플렉션으로 강제 전이 제어
        ReflectionTestUtils.setField(course, "status", CourseStatus.ON_SALE);
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }
}
