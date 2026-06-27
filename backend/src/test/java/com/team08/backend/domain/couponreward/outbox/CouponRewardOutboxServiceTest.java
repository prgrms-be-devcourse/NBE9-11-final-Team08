package com.team08.backend.domain.couponreward.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CouponRewardOutboxServiceTest {

    @Mock
    private CouponRewardOutboxEventRepository couponRewardOutboxEventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CouponRewardOutboxService couponRewardOutboxService;

    @BeforeEach
    void setUp() {
        couponRewardOutboxService = new CouponRewardOutboxService(
                couponRewardOutboxEventRepository,
                new ObjectMapper().findAndRegisterModules(),
                eventPublisher
        );
    }

    @Test
    @DisplayName("회원가입 outbox 이벤트를 저장하고 생성 이벤트를 발행한다")
    void createUserSignedUpEvent_savesOutboxAndPublishesCreatedEvent() {
        // given
        given(couponRewardOutboxEventRepository.existsByEventTypeAndEventKey(
                CouponRewardOutboxEvent.USER_SIGNED_UP_EVENT,
                "1"
        )).willReturn(false);
        given(couponRewardOutboxEventRepository.save(any(CouponRewardOutboxEvent.class)))
                .willAnswer(invocation -> {
                    CouponRewardOutboxEvent event = invocation.getArgument(0);
                    ReflectionTestUtils.setField(event, "id", 10L);
                    return event;
                });

        // when
        couponRewardOutboxService.createUserSignedUpEvent(1L);

        // then
        ArgumentCaptor<CouponRewardOutboxEvent> eventCaptor = ArgumentCaptor.forClass(CouponRewardOutboxEvent.class);
        then(couponRewardOutboxEventRepository).should().save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(CouponRewardOutboxEvent.USER_SIGNED_UP_EVENT);
        assertThat(eventCaptor.getValue().getEventKey()).isEqualTo("1");

        ArgumentCaptor<CouponRewardOutboxCreatedEvent> createdEventCaptor =
                ArgumentCaptor.forClass(CouponRewardOutboxCreatedEvent.class);
        then(eventPublisher).should().publishEvent(createdEventCaptor.capture());
        assertThat(createdEventCaptor.getValue().outboxEventId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("이미 같은 outbox 이벤트가 있으면 저장과 발행을 하지 않는다")
    void createAttendanceCheckedEvent_doesNotSaveWhenOutboxEventAlreadyExists() {
        // given
        LocalDate attendanceDate = LocalDate.of(2026, 6, 27);
        given(couponRewardOutboxEventRepository.existsByEventTypeAndEventKey(
                CouponRewardOutboxEvent.ATTENDANCE_CHECKED_EVENT,
                "1:" + attendanceDate
        )).willReturn(true);

        // when
        couponRewardOutboxService.createAttendanceCheckedEvent(1L, attendanceDate, 7, 15);

        // then
        then(couponRewardOutboxEventRepository).should(never()).save(any(CouponRewardOutboxEvent.class));
        then(eventPublisher).shouldHaveNoInteractions();
    }
}
