package com.team08.backend.domain.learningevent.service;

import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.dto.RecordLearningEventRequest;
import com.team08.backend.domain.learningevent.dto.CourseStatsProjection;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LectureExitedEvent;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningEventService {

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

        //1. 이벤트 적재
        LearningEvent event = LearningEvent.create(
                userId,
                request.courseId(),
                request.chapterId(),
                request.lectureId(),
                request.eventType(),
                request.positionSeconds(),
                request.eventTime(),
                eventKey
        );

        LearningEvent saved = learningEventRepository.save(event);

        //2. 이벤트 따라 작업 분기 (publisher)
            // 퇴장은 단일 출처·비가역이라 이벤트 적재가 1급(반드시 커밋).
            // lecture_progresses 마지막 위치 flush 는 하트비트로 다중화된 best-effort 보정이라
            // 이벤트 커밋 후(AFTER_COMMIT) 별도 트랜잭션에서 처리해 이벤트를 롤백시키지 않는다.
        if (saved.getEventType() == LearningEventType.LECTURE_EXIT) {
            eventPublisher.publishEvent(new LectureExitedEvent(
                    saved.getUserId(),
                    saved.getLectureId(),
                    saved.getPositionSeconds(),
                    saved.getEventTime()
            ));
        }

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
