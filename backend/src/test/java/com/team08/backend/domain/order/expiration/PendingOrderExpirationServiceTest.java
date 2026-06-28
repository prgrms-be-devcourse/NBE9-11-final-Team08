package com.team08.backend.domain.order.expiration;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PendingOrderExpirationServiceTest {

    private static final Long ORDER_ID = 10L;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-28T10:00:00Z"), ZONE_ID);
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);
    private static final PendingOrderExpirationProperties PROPERTIES = new PendingOrderExpirationProperties(
            true,
            60_000,
            30,
            20
    );

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private PendingOrderExpirationService expirationService;

    @BeforeEach
    void setUp() {
        expirationService = new PendingOrderExpirationService(
                orderRepository,
                paymentRepository,
                PROPERTIES,
                FIXED_CLOCK
        );
    }

    @Test
    void findsOldPendingOrdersWithExpirablePaymentStatuses() {
        LocalDateTime threshold = FIXED_NOW.minusMinutes(30);
        given(orderRepository.findExpirationCandidateIds(
                OrderStatus.PENDING_PAYMENT,
                threshold,
                List.of(PaymentStatus.READY, PaymentStatus.DECLINED),
                PageRequest.of(0, 20)
        )).willReturn(List.of(1L, 2L));

        List<Long> result = expirationService.findExpirationCandidateIds();

        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    void oldPendingOrderWithoutPaymentExpires() {
        Order order = order(OrderStatus.PENDING_PAYMENT, FIXED_NOW.minusMinutes(31));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());

        boolean expired = expirationService.expireOrder(ORDER_ID);

        assertThat(expired).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(order.getExpiredAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void readyPaymentIsCanceledWhenOrderExpires() {
        Order order = order(OrderStatus.PENDING_PAYMENT, FIXED_NOW.minusMinutes(31));
        Payment payment = payment(order, PaymentStatus.READY);
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));

        boolean expired = expirationService.expireOrder(ORDER_ID);

        assertThat(expired).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void declinedPaymentIsCanceledWhenOrderExpires() {
        Order order = order(OrderStatus.PENDING_PAYMENT, FIXED_NOW.minusMinutes(31));
        Payment payment = payment(order, PaymentStatus.DECLINED);
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));

        boolean expired = expirationService.expireOrder(ORDER_ID);

        assertThat(expired).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void processingPaymentKeepsOrderPending() {
        Order order = order(OrderStatus.PENDING_PAYMENT, FIXED_NOW.minusMinutes(31));
        Payment payment = payment(order, PaymentStatus.PROCESSING);
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));

        boolean expired = expirationService.expireOrder(ORDER_ID);

        assertThat(expired).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void unknownPaymentKeepsOrderPending() {
        Order order = order(OrderStatus.PENDING_PAYMENT, FIXED_NOW.minusMinutes(31));
        Payment payment = payment(order, PaymentStatus.UNKNOWN);
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));

        boolean expired = expirationService.expireOrder(ORDER_ID);

        assertThat(expired).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    void recentOrAlreadyCompletedOrderIsNotTouched() {
        Order recentOrder = order(OrderStatus.PENDING_PAYMENT, FIXED_NOW.minusMinutes(29));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(recentOrder));

        assertThat(expirationService.expireOrder(ORDER_ID)).isFalse();
        assertThat(recentOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verifyNoInteractions(paymentRepository);

        Order paidOrder = order(OrderStatus.PAID, FIXED_NOW.minusHours(1));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(paidOrder));

        assertThat(expirationService.expireOrder(ORDER_ID)).isFalse();
        verify(paymentRepository, never()).findByOrder_Id(ORDER_ID);
    }

    private Order order(OrderStatus status, LocalDateTime orderedAt) {
        Order order = Order.createPendingPayment(1L, "ORD-20260628190000-ABC12345", orderedAt);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    private Payment payment(Order order, PaymentStatus status) {
        LocalDateTime createdAt = FIXED_NOW.minusHours(1);
        Payment payment = Payment.createReady(order, PaymentProviderType.MOCK, createdAt);
        ReflectionTestUtils.setField(payment, "id", 100L);

        if (status == PaymentStatus.PROCESSING) {
            payment.markProcessing(createdAt);
        } else if (status == PaymentStatus.DECLINED) {
            payment.markProcessing(createdAt);
            payment.decline("payment-key", "CARD", "승인 거절", createdAt);
        } else if (status == PaymentStatus.UNKNOWN) {
            payment.markProcessing(createdAt);
            payment.markUnknown("TIMEOUT", "결제 결과 확인 필요", createdAt);
        }
        return payment;
    }
}
