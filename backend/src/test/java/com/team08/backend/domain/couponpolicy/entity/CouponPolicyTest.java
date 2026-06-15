package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.command.CouponPolicyCreateCommand;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodEndedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodNotStartedException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyTest {

    @Test
    @DisplayName("성공: 정률 할인 계산 시 정수 연산을 사용하여 정확한 금액을 반환한다 (내림 처리)")
    void calculateDiscountAmount_percent_success() {
        // given
        CouponPolicyCreateCommand command = new CouponPolicyCreateCommand(
                "테스트", DiscountType.PERCENT, 10, 30, 100, null,
                CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, null, null
        );
        CouponPolicy policy = CouponPolicy.create(command);
        int originalPrice = 10555; // 10% 면 1055.5원 -> 1055원 기대

        // when
        int discount = policy.calculateDiscountAmount(originalPrice);

        // then
        assertThat(discount).isEqualTo(1055);
    }

    @Test
    @DisplayName("성공: 현재 시간이 발급 기간 내에 있으면 검증을 통과한다")
    void validateIssuePeriod_success() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 12, 0);
        CouponPolicyCreateCommand command = new CouponPolicyCreateCommand(
                "테스트", DiscountType.AMOUNT, 1000, 30, 100, null,
                CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, now.minusDays(1), now.plusDays(1)
        );
        CouponPolicy policy = CouponPolicy.create(command);

        // when & then
        policy.validateIssuePeriod(now);
    }

    @Test
    @DisplayName("실패: 현재 시간이 발급 시작일 이전이면 예외가 발생한다")
    void validateIssuePeriod_fail_notStarted() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 12, 0);
        CouponPolicyCreateCommand command = new CouponPolicyCreateCommand(
                "테스트", DiscountType.AMOUNT, 1000, 30, 100, null,
                CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, now.plusDays(1), now.plusDays(2)
        );
        CouponPolicy policy = CouponPolicy.create(command);

        // when & then
        assertThatThrownBy(() -> policy.validateIssuePeriod(now))
                .isInstanceOf(CouponIssuePeriodNotStartedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ISSUE_PERIOD_NOT_STARTED);
    }

    @Test
    @DisplayName("실패: 현재 시간이 발급 종료일 이후이면 예외가 발생한다")
    void validateIssuePeriod_fail_ended() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 12, 0);
        CouponPolicyCreateCommand command = new CouponPolicyCreateCommand(
                "테스트", DiscountType.AMOUNT, 1000, 30, 100, null,
                CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, now.minusDays(2), now.minusDays(1)
        );
        CouponPolicy policy = CouponPolicy.create(command);

        // when & then
        assertThatThrownBy(() -> policy.validateIssuePeriod(now))
                .isInstanceOf(CouponIssuePeriodEndedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ISSUE_PERIOD_ENDED);
    }

    @Test
    @DisplayName("실패: 쿠폰 수량이 0 이하일 때 차감을 시도하면 예외가 발생한다")
    void decreaseQuantity_fail_exhausted() {
        // given
        CouponPolicyCreateCommand command = new CouponPolicyCreateCommand(
                "테스트", DiscountType.AMOUNT, 1000, 30, 0, null,
                CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, null, null
        );
        CouponPolicy policy = CouponPolicy.create(command);

        // when & then
        assertThatThrownBy(policy::decreaseQuantity)
                .isInstanceOf(CouponExhaustedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXHAUSTED);
    }

    @Test
    @DisplayName("성공: 만료 시각 계산 시 현재 날짜 기준으로 유효 기간을 더해 해당 일의 마지막 시각을 반환한다")
    void calculateExpirationDate_success() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 12, 0);
        CouponPolicyCreateCommand command = new CouponPolicyCreateCommand(
                "테스트", DiscountType.AMOUNT, 1000, 30, 100, null,
                CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, null, null
        );
        CouponPolicy policy = CouponPolicy.create(command);

        // when
        LocalDateTime expiredAt = policy.calculateExpirationDate(now);

        // then
        assertThat(expiredAt).isEqualTo(now.toLocalDate().plusDays(30).atTime(java.time.LocalTime.MAX));
    }
}
