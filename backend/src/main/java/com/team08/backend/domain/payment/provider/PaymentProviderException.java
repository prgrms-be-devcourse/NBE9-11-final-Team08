package com.team08.backend.domain.payment.provider;

import lombok.Getter;

@Getter
public class PaymentProviderException extends RuntimeException {

    private final PaymentProviderFailureType failureType;
    private final String failureCode;
    private final String failureMessage;

    private PaymentProviderException(PaymentProviderFailureType failureType, String failureCode, String failureMessage) {
        super(failureMessage);
        this.failureType = failureType;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public static PaymentProviderException declined(String failureCode, String failureMessage) {
        return new PaymentProviderException(PaymentProviderFailureType.DECLINED, failureCode, failureMessage);
    }

    public static PaymentProviderException timeout(String failureCode, String failureMessage) {
        return new PaymentProviderException(PaymentProviderFailureType.TIMEOUT, failureCode, failureMessage);
    }

    public static PaymentProviderException unknown(String failureCode, String failureMessage) {
        return new PaymentProviderException(PaymentProviderFailureType.UNKNOWN, failureCode, failureMessage);
    }
}
