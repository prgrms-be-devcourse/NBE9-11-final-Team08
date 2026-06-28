package com.team08.backend.domain.order.expiration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.order.expiration.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PendingOrderExpirationScheduler {

    private final PendingOrderExpirationService pendingOrderExpirationService;

    @Scheduled(fixedDelayString = "${app.order.expiration.fixed-delay:60000}")
    public void expirePendingOrders() {
        for (Long orderId : pendingOrderExpirationService.findExpirationCandidateIds()) {
            try {
                pendingOrderExpirationService.expireOrder(orderId);
            } catch (RuntimeException exception) {
                log.error("미결제 주문 만료 처리에 실패했습니다. orderId={}", orderId, exception);
            }
        }
    }
}
