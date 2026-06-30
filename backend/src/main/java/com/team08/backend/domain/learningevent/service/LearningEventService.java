package com.team08.backend.domain.learningevent.service;

import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.dto.RecordLearningEventRequest;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningEventService {

    private final LearningEventRepository learningEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── 클라이언트 이벤트 기록 (입장/멈춤/퇴장) ───────────────────────────────
    @Transactional
    public LearningEventResponse recordEvent(Long userId, RecordLearningEventRequest request) {
        // 완료(LECTURE_COMPLETE)는 클라이언트가 직접 기록할 수 없다.
        // 완강 판별은 오직 하트비트 진행률(lecture_progresses, ≥90%)로 결정되고,
        // 완료 전이 시 서버가 LectureCompletedEvent → recordLectureCompletion 으로 연쇄 발행한다.
        // (완료 ⟺ LECTURE_COMPLETE 이벤트 존재 불변식을 서버가 구조적으로 보장)
        if (request.eventType() == LearningEventType.LECTURE_COMPLETE) {
            throw new CustomException(ErrorCode.CLIENT_COMPLETE_EVENT_NOT_ALLOWED);
        }
        return persist(
                userId,
                request.courseId(),
                request.chapterId(),
                request.lectureId(),
                request.eventType(),
                request.positionSeconds(),
                resolveEventKey(request)
        );
    }

    // ── 완료 학습이벤트 적재 (서버 전용, 완료 전이에서만 호출) ──────────────────
    // (user, lecture) 당 1회만 기록되도록 결정적 키로 멱등 처리한다.
    @Transactional
    public void recordLectureCompletion(Long userId, Long courseId, Long chapterId, Long lectureId,
                                        LocalDateTime eventTime) {
        String eventKey = completionEventKey(userId, lectureId);
        if (learningEventRepository.existsByUniqueEventKey(eventKey)) {
            return; // 이미 기록됨 — 멱등
        }
        persist(userId, courseId, chapterId, lectureId,
                LearningEventType.LECTURE_COMPLETE, null, eventKey);
    }

    private LearningEventResponse persist(Long userId, Long courseId, Long chapterId, Long lectureId,
                                          LearningEventType eventType, Integer positionSeconds, String eventKey) {
        // 클라이언트 재전송 등으로 같은 키가 이미 있으면 중복 처리
        if (learningEventRepository.existsByUniqueEventKey(eventKey)) {
            throw new CustomException(ErrorCode.DUPLICATE_LEARNING_EVENT);
        }

        //1. 이벤트 적재
        //   발생 시각은 클라이언트 시계를 믿지 않고 서버 수신 시각으로 찍는다(권위값).
        //   타임존 모호성·시계 오차·조작이 원천적으로 없고, 모든 이벤트가 단일 서버 클럭으로 정렬된다.
        LearningEvent event = LearningEvent.create(
                userId, courseId, chapterId, lectureId,
                eventType, positionSeconds, LocalDateTime.now(), eventKey
        );

        LearningEvent saved = learningEventRepository.save(event);

        //2. 적재된 이벤트를 단일 도메인 이벤트로 발행한다.
        //   "그래서 무엇을 할지"(피드, 일별 통계 등)는 각 리스너가 자기 타입만 필터링해
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

    // ── helpers ──────────────────────────────────────────────────────────────
    private String resolveEventKey(RecordLearningEventRequest request) {
        if (request.eventKey() != null && !request.eventKey().isBlank()) {
            return request.eventKey();
        }
        // 클라이언트가 키를 제공하지 않으면 서버가 생성
        return UUID.randomUUID().toString();
    }

    // 완료 학습이벤트의 결정적 키 — (user, lecture) 당 1회만 적재되도록 보장(멱등).
    private String completionEventKey(Long userId, Long lectureId) {
        return "LECTURE_COMPLETE:" + userId + ":" + lectureId;
    }

    private boolean isAdmin(String role) {
        return "ROLE_ADMIN".equals(role);
    }
}
