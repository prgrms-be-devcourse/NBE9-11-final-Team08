package com.team08.backend.domain.order.expiration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingOrderExpirationSchedulerTest {

    @Mock
    private PendingOrderExpirationService expirationService;

    @Test
    void oneFailureDoesNotStopRemainingOrderExpiration() {
        PendingOrderExpirationScheduler scheduler = new PendingOrderExpirationScheduler(expirationService);
        given(expirationService.findExpirationCandidateIds()).willReturn(List.of(1L, 2L));
        given(expirationService.expireOrder(1L)).willThrow(new IllegalStateException("만료 실패"));

        scheduler.expirePendingOrders();

        verify(expirationService).expireOrder(1L);
        verify(expirationService).expireOrder(2L);
    }
}
