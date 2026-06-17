package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.component.CouponPolicyValidator;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyValidatorTest {

    private final CouponPolicyValidator validator = new CouponPolicyValidator();

    @Test
    @DisplayName("수정 검증 실패: 정률 할인(PERCENT)인데 할인 값이 100을 초과하면 예외가 발생한다")
    void validate_fail_percent_over_100() {
        // given
        CouponPolicyUpdateRequest request = new CouponPolicyUpdateRequest(
                "테스트", DiscountType.PERCENT, 101, null, 0, 7, null, null, null, CouponTarget.ALL, false, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("수정 검증 실패: 정액 할인(AMOUNT)인데 최대 할인 금액(maxDiscountAmount)이 설정되어 있으면 예외가 발생한다")
    void validate_fail_amount_with_maxDiscount() {
        // given
        CouponPolicyUpdateRequest request = new CouponPolicyUpdateRequest(
                "테스트", DiscountType.AMOUNT, 5000, 1000, 0, 7, null, null, null, CouponTarget.ALL, false, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("수정 검증 실패: 카테고리 대상 쿠폰인데 카테고리 ID 목록이 비어있으면 예외가 발생한다")
    void validate_fail_category_target_without_ids() {
        // given
        CouponPolicyUpdateRequest request = new CouponPolicyUpdateRequest(
                "테스트", DiscountType.AMOUNT, 5000, null, 0, 7, null, List.of(), null, CouponTarget.CATEGORY, false, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("수정 검증 실패: 코스 대상 쿠폰인데 코스 ID 목록이 비어있으면 예외가 발생한다")
    void validate_fail_course_target_without_ids() {
        // given
        CouponPolicyUpdateRequest request = new CouponPolicyUpdateRequest(
                "테스트", DiscountType.AMOUNT, 5000, null, 0, 7, null, null, List.of(), CouponTarget.COURSE, false, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("수정 검증 실패: 발급 시작일이 종료일보다 늦으면 예외가 발생한다")
    void validate_fail_invalid_period() {
        // given
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CouponPolicyUpdateRequest request = new CouponPolicyUpdateRequest(
                "테스트", DiscountType.AMOUNT, 5000, null, 0, 7, null, null, null, CouponTarget.ALL, false, start, end
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }
}
