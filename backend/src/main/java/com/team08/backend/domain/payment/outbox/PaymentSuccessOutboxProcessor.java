package com.team08.backend.domain.payment.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSuccessOutboxProcessor {

    private final PaymentSuccessOutboxTransactionService transactionService;

    public void processReady() {
        for (Long eventId : transactionService.findReadyIds()) {
            try {
                transactionService.processReady(eventId);
            } catch (Exception exception) {
                String errorMessage = StringUtils.hasText(exception.getMessage())
                        ? exception.getMessage()
                        : exception.getClass().getSimpleName();
                transactionService.markFailed(eventId, errorMessage);
                log.error("결제 성공 후처리 Outbox 처리에 실패했습니다. eventId={}", eventId, exception);
            }
        }
    }
}
