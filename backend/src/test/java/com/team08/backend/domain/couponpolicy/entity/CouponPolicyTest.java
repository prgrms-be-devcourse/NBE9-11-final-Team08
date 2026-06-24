package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodEndedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodNotStartedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyTest {

    @Test
    @DisplayName("성공: COURSE 타겟 쿠폰은 지정된 코스 ID들에만 적용 가능하다")
    void isApplicableTo_course_success() {
        // given
        List<Long> targetCourseIds = List.of(100L, 200L);
        CouponPolicy policy = CouponPolicy.createPolicy(
                "특정 코스 할인", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 7, null, null, null, targetCourseIds
        );

        // when & then
        assertThat(policy.isApplicableTo(100L, 1L)).isTrue();  // 코스 100 포함됨
        assertThat(policy.isApplicableTo(200L, 1L)).isTrue();  // 코스 200 포함됨
        assertThat(policy.isApplicableTo(300L, 1L)).isFalse(); // 코스 300 미포함 (적용 불가)
    }

    @Test
    @DisplayName("성공: CATEGORY 타겟 쿠폰은 지정된 카테고리 ID들에만 적용 가능하다")
    void isApplicableTo_category_success() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "카테고리 할인", CouponTarget.CATEGORY, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 7, null, null, List.of(50L, 51L), null
        );

        // when & then
        assertThat(policy.isApplicableTo(1L, 50L)).isTrue();  // 카테고리 50 포함됨
        assertThat(policy.isApplicableTo(1L, 51L)).isTrue();  // 카테고리 51 포함됨
        assertThat(policy.isApplicableTo(1L, 60L)).isFalse(); // 카테고리 60 미포함
    }

    @Test
    @DisplayName("성공: ALL 타겟 쿠폰은 모든 코스와 카테고리에 적용 가능하다")
    void isApplicableTo_all_success() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "전체 할인", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 7, null, null, null, null
        );

        // when & then
        assertThat(policy.isApplicableTo(999L, 999L)).isTrue();
        assertThat(policy.isApplicableTo(1L, 1L)).isTrue();
    }

    @Test
    @DisplayName("성공: 정률 할인 계산 시 정수 연산을 사용하여 정확한 금액을 반환한다 (내림 처리)")
    void calculateDiscountAmount_percent_success() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.PERCENT, 10, 10000, null, 30, null, null, null, null
        );
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
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 30, now.minusDays(1), now.plusDays(1), null, null
        );

        // when & then
        policy.validateIssuePeriod(now);
    }

    @Test
    @DisplayName("실패: 현재 시간이 발급 시작일 이전이면 예외가 발생한다")
    void validateIssuePeriod_fail_notStarted() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 12, 0);
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 30, now.plusDays(1), now.plusDays(2), null, null
        );

        // when & then
        assertThatThrownBy(() -> policy.validateIssuePeriod(now))
                .isInstanceOf(CouponIssuePeriodNotStartedException.class);
    }

    @Test
    @DisplayName("실패: 현재 시간이 발급 종료일 이후이면 예외가 발생한다")
    void validateIssuePeriod_fail_ended() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 12, 0);
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 30, now.minusDays(2), now.minusDays(1), null, null
        );

        // when & then
        assertThatThrownBy(() -> policy.validateIssuePeriod(now))
                .isInstanceOf(CouponIssuePeriodEndedException.class);
    }

    @Test
    @DisplayName("실패: 쿠폰 수량이 0 이하일 때 차감을 시도하면 예외가 발생한다")
    void decreaseQuantity_fail_exhausted() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.FCFS, 1, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 30, null, null, null, null
        );
        policy.decreaseQuantity();

        // when & then
        assertThatThrownBy(policy::decreaseQuantity)
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    @DisplayName("성공: 정률 할인 계산 시 최대 할인 금액 제한이 있으면 해당 금액까지만 할인된다")
    void calculateDiscountAmount_percent_withMaxLimit() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.PERCENT, 10, 2000, null, 30, null, null, null, null
        );
        int originalPrice = 50000; // 10% 면 5000원이지만, 최대 2000원 제한

        // when
        int discount = policy.calculateDiscountAmount(originalPrice);

        // then
        assertThat(discount).isEqualTo(2000);
    }

    @Test
    @DisplayName("성공: 정액 할인 계산 시 상품 가격이 할인 금액보다 작으면 상품 가격만큼만 할인된다")
    void calculateDiscountAmount_amount_priceLowerThanDiscount() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, null, 30, null, null, null, null
        );
        int originalPrice = 3000; // 5000원 할인 쿠폰이지만 상품이 3000원

        // when
        int discount = policy.calculateDiscountAmount(originalPrice);

        // then
        assertThat(discount).isEqualTo(3000);
    }

    @Test
    @DisplayName("실패: COURSE 타겟 쿠폰에 코스 ID가 null로 전달되면 적용 불가능하다")
    void isApplicableTo_course_fail_withNull() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "코스 할인", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 7, null, null, null, List.of(100L)
        );

        // when & then
        assertThat(policy.isApplicableTo(null, 1L)).isFalse();
    }

    @Test
    @DisplayName("실패: CATEGORY 타겟 쿠폰에 카테고리 ID가 null로 전달되면 적용 불가능하다")
    void isApplicableTo_category_fail_withNull() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "카테고리 할인", CouponTarget.CATEGORY, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 7, null, null, List.of(50L), null
        );

        // when & then
        assertThat(policy.isApplicableTo(100L, null)).isFalse();
    }

    @Test
    @DisplayName("성공: 만료 시각 계산 시 현재 날짜 기준으로 유효 기간을 더해 해당 일의 마지막 시각을 반환한다")
    void calculateExpirationDate_success() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 12, 0);
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, null, 30, null, null, null, null
        );

        // when
        LocalDateTime expiredAt = policy.calculateExpirationDate(now);

        // then
        assertThat(expiredAt).isEqualTo(now.toLocalDate().plusDays(30).atTime(java.time.LocalTime.MAX));
    }
}
