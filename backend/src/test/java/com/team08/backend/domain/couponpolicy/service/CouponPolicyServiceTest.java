package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.factory.CouponPolicyFactory;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponPolicyServiceTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private CouponPolicyFactory couponPolicyFactory;

    @InjectMocks
    private CouponPolicyService couponPolicyService;

    @Test
    @DisplayName("쿠폰 정책 생성 요청 시 정상적으로 저장되고 DTO를 반환한다")
    void createCouponPolicy_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트 쿠폰",
                DiscountType.AMOUNT,
                1000,
                null,
                30,
                100,
                null,
                null,
                null,
                CouponType.NORMAL,
                CouponTarget.ALL,
                CouponUsageType.SINGLE_USE,
                false,
                null,
                null
        );

        CouponPolicy policy = CouponPolicy.createNormalPolicy(
                request.name(), request.discountType(), request.discountValue(),
                request.maxDiscountAmount(), request.minOrderAmount(), request.validDays(),
                request.categoryId(), request.courseIds(),
                request.couponTarget(), request.usageType(),
                request.isStackable(), request.issueStartDate(), request.issueEndDate()
        );

        when(couponPolicyFactory.create(request)).thenReturn(policy);
        when(couponPolicyRepository.save(any(CouponPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CouponPolicyResponse response = couponPolicyService.createCouponPolicy(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("테스트 쿠폰");
        verify(couponPolicyFactory, times(1)).create(request);
        verify(couponPolicyRepository, times(1)).save(any(CouponPolicy.class));
    }
}
