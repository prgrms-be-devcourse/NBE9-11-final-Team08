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
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningEventService {

    private final LearningEventRepository learningEventRepository;
    private final CourseRepository courseRepository;
    private final LectureProgressRepository lectureProgressRepository;
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

        // 완료 이벤트는 클라이언트 주장(eventType)을 그대로 믿지 않고 서버 상태로 검증한다.
        // lecture_progresses 의 누적 시청시간이 완료 기준(progressRate≥90%)에 도달해
        // completed=true 인 경우에만 적재를 허용한다. (프론트 가드를 우회한 직접 호출로
        // learning_daily_stats.completed_count 가 부풀려져 리포트가 어긋나는 것을 막는다.)
        if (request.eventType() == LearningEventType.LECTURE_COMPLETE
                && !isLectureCompleted(userId, request.lectureId())) {
            throw new CustomException(ErrorCode.LECTURE_NOT_COMPLETED);
        }

        //1. 이벤트 적재
        //   발생 시각은 클라이언트 시계를 믿지 않고 서버 수신 시각으로 찍는다(권위값).
        //   타임존 모호성·시계 오차·조작이 원천적으로 없고, 모든 이벤트가 단일 서버 클럭으로 정렬된다.
        //   (현재 클라이언트는 이벤트를 즉시 전송하므로 수신 시각 ≈ 발생 시각)
        LearningEvent event = LearningEvent.create(
                userId,
                request.courseId(),
                request.chapterId(),
                request.lectureId(),
                request.eventType(),
                request.positionSeconds(),
                LocalDateTime.now(),
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
    private String resolveEventKey(RecordLearningEventRequest request) {
        if (request.eventKey() != null && !request.eventKey().isBlank()) {
            return request.eventKey();
        }
        // 클라이언트가 키를 제공하지 않으면 서버가 생성
        return UUID.randomUUID().toString();
    }

    // 완료 이벤트 검증용: 해당 유저·강의의 진행 행이 실제 완료 상태인지 확인한다.
    private boolean isLectureCompleted(Long userId, Long lectureId) {
        if (lectureId == null) {
            return false;
        }
        return lectureProgressRepository.findByUserIdAndLectureId(userId, lectureId)
                .map(LectureProgress::getCompleted)
                .orElse(false);
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
