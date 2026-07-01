package com.team08.backend.domain.learningevent.analytics.service;

import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsProjection;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 어드민/판매자용 대용량 조회·집계 전용 서비스.
 * <p>
 * 서비스 트래픽의 핫 경로(이벤트 적재 = LearningEventService#recordEvent)와 패키지 단위로
 * 분리해, 무거운 운영 조회가 적재 경로와 같은 트랜잭션·컴포넌트 경계를 공유하지 않게 한다.
 * analytics 패키지로 갈라 두면 이후 물리 분리(별도 인스턴스/읽기 전용 복제본·별도 커넥션
 * 풀로의 분리)는 패키지 단위 component-scan 슬라이스 또는 모듈 추출만으로 끝난다.
 * <p>
 * 모든 메서드는 읽기 전용이며 적재 경로와 도메인 이벤트를 발행하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class LearningEventAnalyticsService {

    private final LearningEventRepository learningEventRepository;
    private final CourseRepository courseRepository;

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
