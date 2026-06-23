package com.team08.backend.domain.payment.client;

import lombok.Getter;

@Getter
public class TossPaymentException extends RuntimeException {

    private final TossPaymentFailureType failureType;
    private final String failureCode;
    private final String failureMessage;

    public TossPaymentException(TossPaymentFailureType failureType, String failureCode, String failureMessage) {
        super(failureMessage);
        this.failureType = failureType;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public static TossPaymentException declined(String failureCode, String failureMessage) {
        return new TossPaymentException(TossPaymentFailureType.DECLINED, failureCode, failureMessage);
    }

    public static TossPaymentException timeout(String failureCode, String failureMessage) {
        return new TossPaymentException(TossPaymentFailureType.TIMEOUT, failureCode, failureMessage);
    }

    public static TossPaymentException unknown(String failureCode, String failureMessage) {
        return new TossPaymentException(TossPaymentFailureType.UNKNOWN, failureCode, failureMessage);
    }
}
