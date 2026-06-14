package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CouponPolicyTest {

    @Test
    @DisplayName("성공: 정률 할인 계산 시 정수 연산을 사용하여 정확한 금액을 반환한다 (내림 처리)")
    void calculateDiscountAmount_percent_success() {
        // given
        CouponPolicy policy = CouponPolicy.create(createRequest(DiscountType.PERCENT, 10)); // 10% 할인
        int originalPrice = 10555; // 10% 면 1055.5원 -> 1055원 기대

        // when
        int discount = policy.calculateDiscountAmount(originalPrice);

        // then
        assertThat(discount).isEqualTo(1055);
    }

    @Test
    @DisplayName("성공: 정률 할인 시 최대 할인 금액 제한이 정상 적용된다")
    void calculateDiscountAmount_percent_withMaxAmount() {
        // given
    }

    private CouponPolicyCreateRequest createRequest(DiscountType type, int value) {
        return new CouponPolicyCreateRequest(
                "테스트", type, value, 30, 100, null,
                CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, null, null
        );
    }
}
