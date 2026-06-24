package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyFactoryTest {

    private final CouponPolicyValidator validator = new CouponPolicyValidator();
    private final CouponPolicyFactory factory = new CouponPolicyFactory(List.of(
            new AllTargetCouponCreator(validator),
            new CategoryTargetCouponCreator(validator),
            new CourseTargetCouponCreator(validator)
    ));

    @Test
    @DisplayName("성공: ALL 타겟 쿠폰 요청 시 AllTargetCouponCreator가 엔티티를 생성한다")
    void create_all_target_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "전체 쿠폰", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 20000, 30, null, null, null, null
        );

        // when
        CouponPolicy policy = factory.create(request);

        // then
        assertThat(policy.getCouponTarget()).isEqualTo(CouponTarget.ALL);
        assertThat(policy.getTargetCategories()).isEmpty();
        assertThat(policy.getTargetCourses()).isEmpty();
    }

    @Test
    @DisplayName("성공: CATEGORY 타겟 쿠폰 요청 시 CategoryTargetCouponCreator가 엔티티를 생성한다")
    void create_category_target_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "카테고리 쿠폰", CouponTarget.CATEGORY, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 20000, 30, null, null, List.of(1L, 2L), null
        );

        // when
        CouponPolicy policy = factory.create(request);

        // then
        assertThat(policy.getCouponTarget()).isEqualTo(CouponTarget.CATEGORY);
        assertThat(policy.getTargetCategories()).hasSize(2);
    }

    @Test
    @DisplayName("성공: COURSE 타겟 쿠폰 요청 시 CourseTargetCouponCreator가 엔티티를 생성한다")
    void create_course_target_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "코스 쿠폰", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 20000, 30, null, null, null, List.of(10L, 20L)
        );

        // when
        CouponPolicy policy = factory.create(request);

        // then
        assertThat(policy.getCouponTarget()).isEqualTo(CouponTarget.COURSE);
        assertThat(policy.getTargetCourses()).hasSize(2);
    }

    @Test
    @DisplayName("실패: ALL 타겟인데 카테고리나 코스가 포함되면 예외가 발생한다")
    void create_all_target_fail_with_ids() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "전체 실패", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 20000, 30, null, null, List.of(1L), null
        );

        // when & then
        assertThatThrownBy(() -> factory.create(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("실패: CATEGORY 타겟인데 카테고리 ID가 없으면 예외가 발생한다")
    void create_category_target_fail_without_ids() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "카테고리 실패", CouponTarget.CATEGORY, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 20000, 30, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> factory.create(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("실패: COURSE 타겟인데 코스 ID가 없으면 예외가 발생한다")
    void create_course_target_fail_without_ids() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "코스 실패", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 5000, null, 20000, 30, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> factory.create(request))
                .isInstanceOf(CustomException.class);
    }
}
