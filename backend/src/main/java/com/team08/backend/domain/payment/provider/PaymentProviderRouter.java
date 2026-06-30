package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PaymentProviderRouter {

    private final Map<PaymentProviderType, PaymentProvider> providers;

    public PaymentProviderRouter(List<PaymentProvider> providers) {
        this.providers = new EnumMap<>(PaymentProviderType.class);
        providers.forEach(provider -> this.providers.put(provider.type(), provider));
    }

    public PaymentProviderConfirmResponse confirm(PaymentProviderType providerType, PaymentProviderConfirmRequest request) {
        return findProvider(providerType).confirm(request);
    }

    public Optional<PaymentProviderLookupResponse> lookup(PaymentProviderType providerType, PaymentProviderLookupRequest request) {
        return findProvider(providerType).lookup(request);
    }

    private PaymentProvider findProvider(PaymentProviderType providerType) {
        PaymentProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return provider;
    }
}
