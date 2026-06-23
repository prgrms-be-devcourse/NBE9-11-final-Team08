package com.team08.backend.domain.payment.entity;

public enum PaymentStatus {
    READY,
    PROCESSING,
    SUCCESS,
    // 결제수단 한도/잔액 등 명확한 거절 상태이며 사용자는 재결제할 수 있다.
    DECLINED,
    // 승인 여부가 불명확한 상태이므로 재결제와 수강권 발급을 모두 막고 운영 확인 대상으로 둔다.
    UNKNOWN,
    CANCELED,
    REFUNDED
}
