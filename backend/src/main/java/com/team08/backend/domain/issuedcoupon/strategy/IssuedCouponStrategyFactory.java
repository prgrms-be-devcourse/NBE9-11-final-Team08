package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.issuedcoupon.exception.InvalidCouponTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IssuedCouponStrategyFactory {

    private final Map<CouponType, IssuedCouponStrategy> strategyMap;

    public IssuedCouponStrategyFactory(List<IssuedCouponStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(IssuedCouponStrategy::getSupportedType, Function.identity()));
    }

    // 타입에 맞는 발급 전략 반환
    public IssuedCouponStrategy getStrategy(CouponType type) {
        IssuedCouponStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new InvalidCouponTypeException();
        }
        return strategy;
    }
}
