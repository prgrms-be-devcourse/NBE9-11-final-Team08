package com.team08.backend.domain.learningevent.service;

import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.dto.RecordLearningEventRequest;
import com.team08.backend.domain.learningevent.dto.CourseStatsProjection;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningEventService {

    /** 저장 기준 타임존. 클라이언트가 보낸 오프셋 시각을 이 기준의 벽시계로 변환해 저장한다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 클라이언트 시계와 서버 수신 시각의 허용 오차.
     * 입장/퇴장 이벤트의 eventTime 이 이 범위를 벗어나면 시계 오차·조작으로 보고
     * 서버 수신 시각으로 보정(clamp)한다.
     */
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final LearningEventRepository learningEventRepository;
    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── 이벤트 기록 (중복 방지 포함) ───────────────────────────────────
    @Transactional
    public LearningEventResponse recordEvent(Long userId, RecordLearningEventRequest request) {
        String eventKey = resolveEventKey(request);

        // 클라이언트 쪽에서 네트워크 에러로 같은 이벤트를 여러번 보냈을 때
        // 중복 이벤트 방지
        if (learningEventRepository.existsByUniqueEventKey(eventKey)) {
            throw new CustomException(ErrorCode.DUPLICATE_LEARNING_EVENT);
        }

        // 입장/퇴장 이벤트는 통계·순서의 기준이 되므로 클라이언트 시계를 그대로 믿지 않고
        // 서버 수신 시각과 비교해 허용 오차를 벗어나면 보정한다.
        LocalDateTime eventTime = resolveEventTime(request.eventType(), request.eventTime());

        //1. 이벤트 적재
        LearningEvent event = LearningEvent.create(
                userId,
                request.courseId(),
                request.chapterId(),
                request.lectureId(),
                request.eventType(),
                request.positionSeconds(),
                eventTime,//보정된 시간 값
                eventKey
        );

        LearningEvent saved = learningEventRepository.save(event);

        //2. 적재된 이벤트를 단일 도메인 이벤트로 발행한다.
        //   "그래서 무엇을 할지"(progress flush, 알림 등)는 각 리스너가 자기 타입만 필터링해
        //   소유한다 — 새 반응은 리스너만 추가하면 되고 이 서비스는 수정하지 않는다(개방-폐쇄).
        eventPublisher.publishEvent(LearningEventRecorded.from(saved));

        return LearningEventResponse.from(saved);
    }

    // ── 사용자별 활동 조회 ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LearningEventResponse> getUserActivities(Long requesterId, Long targetUserId, String requesterRole, Pageable pageable) {
        // 본인 또는 관리자만 조회 가능
        if (!requesterId.equals(targetUserId) && !isAdmin(requesterRole)) {
            throw new CustomException(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
        }
        return learningEventRepository.findByUserId(targetUserId, pageable)
                .map(LearningEventResponse::from);
    }

    // ── 강의별 통계 집계 ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public CourseStatsResponse getCourseStats(Long requesterId, Long courseId, String requesterRole) {
        validateAdminOrCourseOwner(requesterId, courseId, requesterRole);

        CourseStatsProjection projection = learningEventRepository.getStatsByCourseId(courseId);
        return new CourseStatsResponse(
                courseId,
                projection.enterCount(),
                projection.watchTimeSeconds(),
                projection.completionCount()
        );
    }

    // ── 챕터별 통계 집계 ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ChapterStatsResponse getChapterStats(Long requesterId, Long chapterId, String requesterRole) {
        if (!isAdmin(requesterRole) && !isSeller(requesterRole)) {
            throw new CustomException(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
        }

        return new ChapterStatsResponse(
                chapterId,
                learningEventRepository.countByChapterIdAndEventType(chapterId, LearningEventType.LECTURE_ENTER),
                learningEventRepository.countByChapterIdAndEventType(chapterId, LearningEventType.LECTURE_COMPLETE),
                (long) learningEventRepository.avgWatchTimeSecondsByChapterId(chapterId)
        );
    }

    // ── 관리자 전체 조회 ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LearningEventResponse> getAllEvents(String requesterRole, Pageable pageable) {
        if (!isAdmin(requesterRole)) {
            throw new CustomException(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
        }
        return learningEventRepository.findAll(pageable)
                .map(LearningEventResponse::from);
    }

    // ── 판매자 강좌 필터링 ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LearningEventResponse> getSellerEvents(Long sellerId, String requesterRole, Pageable pageable) {
        if (!isSeller(requesterRole) && !isAdmin(requesterRole)) {
            throw new CustomException(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
        }

        List<Long> courseIds = courseRepository.findIdsByInstructorId(sellerId);

        if (courseIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return learningEventRepository.findByCourseIdIn(courseIds, pageable)
                .map(LearningEventResponse::from);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * 이벤트 발생 시각을 저장용 KST {@link LocalDateTime} 으로 정규화한다.
     * <p>
     * 1) 타임존: 클라이언트가 보낸 {@link OffsetDateTime}(오프셋 포함)을 KST 벽시계로 변환한다.
     *    오프셋이 명시돼 있으므로 UTC/KST 모호성이 없다.
     * 2) 신뢰성: 입장(LECTURE_ENTER)/퇴장(LECTURE_EXIT)은 통계·순서의 기준이라
     *    서버 수신 시각과의 차이가 {@link #MAX_CLOCK_SKEW} 를 넘으면(시계 오차·조작)
     *    서버 수신 시각으로 보정(clamp)한다. 범위 안이면 클라이언트 시각을 그대로 신뢰하므로
     *    (보통 수초 이내인) 네트워크 지연의 영향을 받지 않는다.
     *    그 외 이벤트 타입은 KST 변환만 하고 시각은 그대로 사용한다.
     */
    private LocalDateTime resolveEventTime(LearningEventType eventType, OffsetDateTime clientTime) {
        LocalDateTime clientKst = clientTime.atZoneSameInstant(KST).toLocalDateTime();

        if (eventType != LearningEventType.LECTURE_ENTER && eventType != LearningEventType.LECTURE_EXIT) {
            return clientKst;
        }

        // TODO:
        LocalDateTime serverNow = LocalDateTime.now(KST);
        long skewSeconds = Math.abs(Duration.between(clientKst, serverNow).getSeconds());
        if (skewSeconds > MAX_CLOCK_SKEW.getSeconds()) {
            log.warn("학습 이벤트 시각 오차가 허용치를 초과해 서버 수신 시각으로 보정: type={}, clientTime={}, serverNow={}, skewSeconds={}",
                    eventType, clientTime, serverNow, skewSeconds);
            return serverNow;
        }
        return clientKst;
    }

    private String resolveEventKey(RecordLearningEventRequest request) {
        if (request.eventKey() != null && !request.eventKey().isBlank()) {
            return request.eventKey();
        }
        // 클라이언트가 키를 제공하지 않으면 서버가 생성
        return UUID.randomUUID().toString();
    }

    private boolean isAdmin(String role) {
        return "ROLE_ADMIN".equals(role);
    }

    private boolean isSeller(String role) {
        return "ROLE_SELLER".equals(role);
    }

    private void validateAdminOrCourseOwner(Long requesterId, Long courseId, String requesterRole) {
        if (isAdmin(requesterRole)) return;

        boolean isCourseOwner = courseRepository.findById(courseId)
                .map(course -> course.getInstructorId().equals(requesterId))
                .orElse(false);

        if (!isCourseOwner) {
            throw new CustomException(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
        }
    }
}
