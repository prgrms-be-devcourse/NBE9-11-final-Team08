package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.component.CouponPolicyFactory;
import com.team08.backend.domain.couponpolicy.component.CouponPolicyUpdater;
import com.team08.backend.domain.couponpolicy.component.CouponPolicyValidator;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponPolicyUpdater couponPolicyUpdater;

    @Spy
    private CouponPolicyValidator couponPolicyValidator = new CouponPolicyValidator();

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-06-16T10:00:00Z"), ZoneId.systemDefault());

    @InjectMocks
    private CouponPolicyService couponPolicyService;

    @Test
    @DisplayName("쿠폰 정책 생성 요청 시 정상적으로 저장되고 DTO를 반환한다")
    void createCouponPolicy_success() {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "테스트 쿠폰", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 30, null, null, null, null
        );

        CouponPolicy policy = CouponPolicy.createPolicy(
                request.name(), request.couponTarget(), request.couponType(), request.totalQuantity(),
                request.usageType(), request.isStackable(), request.discountType(), request.discountValue(),
                request.maxDiscountAmount(), request.minOrderAmount(), request.validDays(),
                request.issueStartDate(), request.issueEndDate(), request.categoryIds(), request.courseIds()
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

    @Test
    @DisplayName("발급 이력이 있는 쿠폰 정책은 수정 시 예외가 발생한다")
    void updateCouponPolicy_fail_whenAlreadyIssued() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.createPolicy(
                "기존 쿠폰", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, null
        );
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.countByPolicyId(policyId)).thenReturn(1L);

        CouponPolicyUpdateRequest updateRequest = new CouponPolicyUpdateRequest(
                "수정 쿠폰", null, CouponUsageType.SINGLE_USE, false, DiscountType.AMOUNT, 2000, null, 20000, 7, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> couponPolicyService.updateCouponPolicy(policyId, updateRequest))
                .isInstanceOf(CustomException.class);
        verify(couponPolicyRepository).findByIdWithLock(policyId);
    }

    @Test
    @DisplayName("성공: 쿠폰 정책 수정 후에도 기존 적용 대상(CouponTarget)은 유지되어야 한다")
    void updateCouponPolicy_shouldMaintainExistingTarget() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.createPolicy(
                "기존 쿠폰", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, List.of(100L)
        );
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.countByPolicyId(policyId)).thenReturn(0L);

        // CouponTarget.COURSE인 경우 courseIds가 필수이므로 포함시켜야 함 (검증 통과를 위해)
        CouponPolicyUpdateRequest updateRequest = new CouponPolicyUpdateRequest(
                "수정 쿠폰", null, CouponUsageType.SINGLE_USE, true, DiscountType.PERCENT, 10, 5000, 20000, 14, null, null, null, List.of(100L)
        );

        // when
        CouponPolicyResponse response = couponPolicyService.updateCouponPolicy(policyId, updateRequest);

        // then
        assertThat(response.couponTarget()).isEqualTo(CouponTarget.COURSE);
        assertThat(policy.getCouponTarget()).isEqualTo(CouponTarget.COURSE);
    }

    @Test
    @DisplayName("발급 이력이 없는 쿠폰 정책은 정상적으로 수정된다")
    void updateCouponPolicy_success() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.createPolicy(
                "기존 쿠폰", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, null
        );
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.countByPolicyId(policyId)).thenReturn(0L);

        CouponPolicyUpdateRequest updateRequest = new CouponPolicyUpdateRequest(
                "수정 쿠폰", null, CouponUsageType.SINGLE_USE, true, DiscountType.PERCENT, 10, 5000, 20000, 14, null, null, null, null
        );

        // when
        CouponPolicyResponse response = couponPolicyService.updateCouponPolicy(policyId, updateRequest);

        // then
        assertThat(response.name()).isEqualTo("수정 쿠폰");
        assertThat(response.discountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(response.validDays()).isEqualTo(14);
        verify(couponPolicyRepository).findByIdWithLock(policyId);
    }

    @Test
    @DisplayName("쿠폰 정책 조기 종료 시 종료일이 현재 시간으로 업데이트된다")
    void terminateCouponPolicy_success() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.createPolicy(
                "종료할 쿠폰", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, null
        );
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));

        // when
        couponPolicyService.terminateCouponPolicy(policyId);

        // then
        assertThat(policy.getIssueEndDate()).isBeforeOrEqualTo(LocalDateTime.now(clock));
        verify(couponPolicyRepository).findByIdWithLock(policyId);
    }

    @Test
    @DisplayName("발급 이력이 있는 쿠폰 정책은 삭제 시 예외가 발생한다")
    void deleteCouponPolicy_fail_whenAlreadyIssued() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.createPolicy(
                "삭제 시도 쿠폰", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, null
        );
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.countByPolicyId(policyId)).thenReturn(1L);

        // when & then
        assertThatThrownBy(() -> couponPolicyService.deleteCouponPolicy(policyId))
                .isInstanceOf(CustomException.class);
        verify(couponPolicyRepository).findByIdWithLock(policyId);
    }

    @Test
    @DisplayName("발급 이력이 없는 쿠폰 정책은 정상적으로 소프트 삭제된다")
    void deleteCouponPolicy_success() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.createPolicy(
                "삭제할 쿠폰", CouponTarget.ALL, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, null
        );
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.countByPolicyId(policyId)).thenReturn(0L);

        // when
        couponPolicyService.deleteCouponPolicy(policyId);

        // then
        assertThat(policy.getDeletedAt()).isNotNull();
        assertThat(policy.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now(clock));
        verify(couponPolicyRepository).findByIdWithLock(policyId);
    }
}
