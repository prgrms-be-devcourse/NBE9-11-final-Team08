package com.team08.backend.domain.couponpolicy.factory;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.*;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyFactoryTest {

    private final CouponPolicyFactory factory = new CouponPolicyFactory(List.of(
            new FcfsCouponCreator(),
            new NormalCouponCreator()
    ));

    @Test
    @DisplayName("성공: 선착순(FCFS) 쿠폰 요청 시 FcfsCouponCreator가 엔티티를 생성한다")
    void create_fcfs_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "선착순 쿠폰", DiscountType.AMOUNT, 5000, null, 20000, 30, 100,
                null, null, CouponType.FCFS, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                true, null, null
        );

        // when
        CouponPolicy policy = factory.create(request);

        // then
        assertThat(policy).isNotNull();
        assertThat(policy.getCouponType()).isEqualTo(CouponType.FCFS);
        assertThat(policy.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("성공: 일반(NORMAL) 쿠폰 요청 시 NormalCouponCreator가 엔티티를 생성한다")
    void create_normal_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "일반 쿠폰", DiscountType.PERCENT, 10, 10000, 0, 7, null,
                null, null, CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, null, null
        );

        // when
        CouponPolicy policy = factory.create(request);

        // then
        assertThat(policy).isNotNull();
        assertThat(policy.getCouponType()).isEqualTo(CouponType.NORMAL);
        assertThat(policy.getTotalQuantity()).isNull();
    }

    @Test
    @DisplayName("실패: 선착순 쿠폰인데 수량이 없으면 예외가 발생한다 (FcfsCouponCreator 검증)")
    void create_fcfs_fail_no_quantity() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "선착순 실패", DiscountType.AMOUNT, 1000, null, 0, 30, null,
                null, null, CouponType.FCFS, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, null, null
        );

        // when & then
        assertThatThrownBy(() -> factory.create(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("실패: 일반 쿠폰인데 수량이 설정되어 있으면 예외가 발생한다 (NormalCouponCreator 검증)")
    void create_normal_fail_with_quantity() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "일반 실패", DiscountType.AMOUNT, 1000, null, 0, 30, 100,
                null, null, CouponType.NORMAL, CouponTarget.ALL, CouponUsageType.SINGLE_USE,
                false, null, null
        );

        // when & then
        assertThatThrownBy(() -> factory.create(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }
}
