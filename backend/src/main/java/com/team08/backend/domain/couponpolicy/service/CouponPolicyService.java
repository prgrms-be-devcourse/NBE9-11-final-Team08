package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.component.CouponPolicyFactory;
import com.team08.backend.domain.couponpolicy.component.CouponPolicyUpdater;
import com.team08.backend.domain.couponpolicy.component.CouponPolicyValidator;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyDetailResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicySearchRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponPolicyFactory couponPolicyFactory;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponPolicyValidator couponPolicyValidator;
    private final CouponPolicyUpdater couponPolicyUpdater;
    private final Clock clock;

    // 쿠폰 정책 생성
    @Transactional
    public CouponPolicyResponse createCouponPolicy(CouponPolicyCreateRequest request) {
        CouponPolicy newPolicy = couponPolicyFactory.create(request);
        CouponPolicy savedPolicy = couponPolicyRepository.save(newPolicy);

        return CouponPolicyResponse.from(savedPolicy);
    }

    // 쿠폰 정책 목록 조회
    @Transactional(readOnly = true)
    public Page<CouponPolicyResponse> getCouponPolicies(CouponPolicySearchRequest condition, Pageable pageable) {
        return couponPolicyRepository.findAllByCondition(condition, LocalDateTime.now(clock), pageable)
                .map(CouponPolicyResponse::from);
    }

    // 쿠폰 정책 상세 조회
    @Transactional(readOnly = true)
    public CouponPolicyDetailResponse getCouponPolicy(Long id) {
        CouponPolicy policy = couponPolicyRepository.findByIdWithDetails(id)
                .orElseThrow(CouponPolicyNotFoundException::new);
        return CouponPolicyDetailResponse.from(policy);
    }

    // 쿠폰 정책 수정
    @Transactional
    public CouponPolicyResponse updateCouponPolicy(Long id, CouponPolicyUpdateRequest request) {
        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(id)
                .orElseThrow(CouponPolicyNotFoundException::new);

        // 발급 이력 확인
        long issuedCount = issuedCouponRepository.countByPolicyId(id);
        if (issuedCount > 0) {
            throw new CustomException(ErrorCode.COUPON_POLICY_ALREADY_ISSUED);
        }

        couponPolicyValidator.validateForUpdate(request, policy.getCouponType(), policy.getCouponTarget());

        policy.update(
                request.name(),
                request.totalQuantity(),
                request.usageType(),
                request.isStackable(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.minOrderAmount(),
                request.validDays(),
                request.issueStartDate(),
                request.issueEndDate()
        );

        couponPolicyUpdater.updateTargets(policy, request.categoryIds(), request.courseIds());

        return CouponPolicyResponse.from(policy);
    }

    // 쿠폰 정책 조기 종료
    @Transactional
    public void terminateCouponPolicy(Long id) {
        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(id)
                .orElseThrow(CouponPolicyNotFoundException::new);

        policy.terminate(LocalDateTime.now(clock));
    }

    // 쿠폰 정책 삭제
    @Transactional
    public void deleteCouponPolicy(Long id) {
        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(id)
                .orElseThrow(CouponPolicyNotFoundException::new);

        // 발급 이력 확인
        long issuedCount = issuedCouponRepository.countByPolicyId(id);
        if (issuedCount > 0) {
            throw new CustomException(ErrorCode.COUPON_POLICY_ALREADY_ISSUED_CANNOT_DELETE);
        }

        policy.delete(LocalDateTime.now(clock));
    }
}
