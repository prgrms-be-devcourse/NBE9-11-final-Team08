package com.team08.backend.domain.learningevent.service;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LearningEventServiceTest {

    @Mock
    private LearningEventRepository learningEventRepository;

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

    // ──── 완료 이벤트: 서버 전용(클라이언트 거부 + 멱등 적재) ───────────────────────

    @Test
    @DisplayName("클라이언트가 LECTURE_COMPLETE 를 직접 보내면 거부, 적재하지 않음")
    void recordEvent_clientComplete_rejected() {
        Long userId = 1L;
        RecordLearningEventRequest request = lectureCompleteRequest(10L, "complete-key");

        assertThatThrownBy(() -> learningEventService.recordEvent(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLIENT_COMPLETE_EVENT_NOT_ALLOWED);

        verify(learningEventRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("서버 완료 적재: 신규면 LECTURE_COMPLETE 저장 + 도메인 이벤트 발행")
    void recordLectureCompletion_new_persistsAndPublishes() {
        Long userId = 1L;
        String key = "LECTURE_COMPLETE:1:10";

        given(learningEventRepository.existsByUniqueEventKey(key)).willReturn(false);
        given(learningEventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        learningEventService.recordLectureCompletion(userId, 2L, 3L, 10L, LocalDateTime.now());

        ArgumentCaptor<LearningEvent> captor = ArgumentCaptor.forClass(LearningEvent.class);
        verify(learningEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(LearningEventType.LECTURE_COMPLETE);
        assertThat(captor.getValue().getUniqueEventKey()).isEqualTo(key);
        verify(eventPublisher).publishEvent(any(LearningEventRecorded.class));
    }

    @Test
    @DisplayName("서버 완료 적재: 이미 기록됐으면 멱등(저장·발행 없음)")
    void recordLectureCompletion_duplicate_idempotent() {
        given(learningEventRepository.existsByUniqueEventKey("LECTURE_COMPLETE:1:10")).willReturn(true);

        learningEventService.recordLectureCompletion(1L, 2L, 3L, 10L, LocalDateTime.now());

        verify(learningEventRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
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

    // ─────────────────────────────────────────────────────────────────────────
    // fixtures
    // ─────────────────────────────────────────────────────────────────────────

    private RecordLearningEventRequest lectureEnterRequest(Long lectureId, String eventKey) {
        return new RecordLearningEventRequest(
                1L, 2L, lectureId,
                LearningEventType.LECTURE_ENTER,
                null,
                eventKey
        );
    }

    private RecordLearningEventRequest lectureCompleteRequest(Long lectureId, String eventKey) {
        return new RecordLearningEventRequest(
                1L, 2L, lectureId,
                LearningEventType.LECTURE_COMPLETE,
                null,
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
                LocalDateTime.now(), // 서버가 수신 시각으로 적재
                req.eventKey() != null ? req.eventKey() : "generated-key"
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
