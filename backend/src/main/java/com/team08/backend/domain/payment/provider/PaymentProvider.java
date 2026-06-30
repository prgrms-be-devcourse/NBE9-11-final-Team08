package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.entity.PaymentProviderType;

import java.util.Optional;

public interface PaymentProvider {

    PaymentProviderType type();

    PaymentProviderConfirmResponse confirm(PaymentProviderConfirmRequest request);

    Optional<PaymentProviderLookupResponse> lookup(PaymentProviderLookupRequest request);
}
