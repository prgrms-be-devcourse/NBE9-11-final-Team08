package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponPolicyFactory {

    private final List<CouponPolicyCreator> creators;

    public CouponPolicy create(CouponPolicyCreateRequest request) {
        return creators.stream()
                .filter(creator -> creator.supports(request))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE))
                .create(request);
    }
}
