package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicycategory.entity.CouponPolicyCategory;
import com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CouponPolicyUpdaterTest {

    private final CouponPolicyUpdater updater = new CouponPolicyUpdater();

    @Test
    @DisplayName("성공: 새로운 카테고리 ID 목록이 주어지면 기존 목록을 업데이트한다 (추가/삭제)")
    void updateTargets_category_success() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.CATEGORY, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, List.of(1L, 2L), null
        );

        List<Long> newCategoryIds = List.of(2L, 3L); // 1 삭제, 3 추가

        // when
        updater.updateTargets(policy, newCategoryIds, null);

        // then
        List<Long> resultIds = policy.getTargetCategories().stream()
                .map(CouponPolicyCategory::getCategoryId)
                .toList();
        assertThat(resultIds).hasSize(2);
        assertThat(resultIds).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("성공: 새로운 코스 ID 목록이 주어지면 기존 목록을 업데이트한다 (추가/삭제)")
    void updateTargets_course_success() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, List.of(10L, 20L)
        );

        List<Long> newCourseIds = List.of(20L, 30L); // 10 삭제, 30 추가

        // when
        updater.updateTargets(policy, null, newCourseIds);

        // then
        List<Long> resultIds = policy.getTargetCourses().stream()
                .map(CouponPolicyCourse::getCourseId)
                .toList();
        assertThat(resultIds).hasSize(2);
        assertThat(resultIds).containsExactlyInAnyOrder(20L, 30L);
    }

    @Test
    @DisplayName("성공: 빈 목록이 주어지면 모든 기존 타겟을 삭제한다")
    void updateTargets_clear_all() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "테스트", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, List.of(10L, 20L)
        );

        // when
        updater.updateTargets(policy, null, List.of());

        // then
        assertThat(policy.getTargetCourses()).isEmpty();
    }
}
