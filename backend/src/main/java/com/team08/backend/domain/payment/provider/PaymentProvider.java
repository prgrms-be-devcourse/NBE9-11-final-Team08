package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.entity.PaymentProviderType;

public interface PaymentProvider {

    PaymentProviderType type();

    PaymentProviderConfirmResponse confirm(PaymentProviderConfirmRequest request);
}
