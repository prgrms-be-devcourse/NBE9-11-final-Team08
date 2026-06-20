package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.component.CouponPolicyValidator;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
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
    @DisplayName("생성 검증 실패: 정률 할인(PERCENT)인데 할인 값이 100을 초과하면 예외가 발생한다")
    void validate_fail_percent_over_100() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.PERCENT, 101, null, 0, 7, null, null, null, null
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
                "테스트", null, CouponUsageType.SINGLE_USE, false, DiscountType.AMOUNT, 5000, 1000, 0, 7, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validateForUpdate(request, CouponType.NORMAL, CouponTarget.ALL))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("생성 검증 실패: 카테고리 대상 쿠폰인데 카테고리 ID 목록이 비어있으면 예외가 발생한다")
    void validate_fail_category_target_without_ids() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.CATEGORY, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, List.of(), null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("생성 검증 실패: 코스 대상 쿠폰인데 코스 ID 목록이 비어있으면 예외가 발생한다")
    void validate_fail_course_target_without_ids() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, null, List.of()
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("생성 검증 실패: 발급 시작일이 종료일보다 늦으면 예외가 발생한다")
    void validate_fail_invalid_period() {
        // given
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, start, end, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("생성 검증 실패: 선착순(FCFS) 쿠폰인데 수량이 없거나 1 미만이면 예외가 발생한다")
    void validate_fail_fcfs_invalid_quantity() {
        // given
        CouponPolicyCreateRequest request1 = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.FCFS, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, null, null
        );
        CouponPolicyCreateRequest request2 = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.FCFS, 0, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request1))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> validator.validate(request2))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("생성 검증 실패: 일반(NORMAL) 쿠폰인데 수량이 설정되어 있으면 예외가 발생한다")
    void validate_fail_normal_with_quantity() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, 100, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("생성 검증 실패: 금액은 1 미만, 유효 기간은 음수이면 예외가 발생한다")
    void validate_fail_negative_values() {
        CouponPolicyCreateRequest zeroMaxDiscountAmount = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.PERCENT, 10, 0, null, 7, null, null, null, null
        );
        CouponPolicyCreateRequest zeroMinOrderAmount = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, null, null
        );
        CouponPolicyCreateRequest negativeValidDays = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, null, -1, null, null, null, null
        );

        assertThatThrownBy(() -> validator.validate(zeroMaxDiscountAmount))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> validator.validate(zeroMinOrderAmount))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> validator.validate(negativeValidDays))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("생성 검증 실패: 전체(ALL) 대상 쿠폰인데 카테고리나 코스 ID가 있으면 예외가 발생한다")
    void validate_fail_all_target_with_ids() {
        // given
        CouponPolicyCreateRequest request1 = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, List.of(1L), null
        );
        CouponPolicyCreateRequest request2 = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 0, 7, null, null, null, List.of(1L)
        );

        // when & then
        assertThatThrownBy(() -> validator.validate(request1))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> validator.validate(request2))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("생성 검증 성공: 모든 유효성 검사를 통과한다")
    void validate_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, null, 7, null, null, null, null
        );

        // when & then
        validator.validate(request);
    }
}
