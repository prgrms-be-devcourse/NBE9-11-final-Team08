package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyDetailResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicySearchRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.factory.CouponPolicyFactory;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponPolicyFactory couponPolicyFactory;
    private final IssuedCouponRepository issuedCouponRepository;

    // 관리자 쿠폰 정책 생성
    @Transactional
    public CouponPolicyResponse createCouponPolicy(CouponPolicyCreateRequest request) {
        CouponPolicy newPolicy = couponPolicyFactory.create(request);
        CouponPolicy savedPolicy = couponPolicyRepository.save(newPolicy);

        return CouponPolicyResponse.from(savedPolicy);
    }

    // 관리자 쿠폰 정책 목록 조회
    @Transactional(readOnly = true)
    public Page<CouponPolicyResponse> getCouponPolicies(CouponPolicySearchRequest condition, Pageable pageable) {
        return couponPolicyRepository.findAllByCondition(condition, pageable)
                .map(CouponPolicyResponse::from);
    }

    // 관리자 쿠폰 정책 상세 조회
    @Transactional(readOnly = true)
    public CouponPolicyDetailResponse getCouponPolicy(Long id) {
        CouponPolicy policy = couponPolicyRepository.findByIdWithDetails(id)
                .orElseThrow(CouponPolicyNotFoundException::new);
        return CouponPolicyDetailResponse.from(policy);
    }

    // 관리자 쿠폰 정책 수정
    @Transactional
    public CouponPolicyResponse updateCouponPolicy(Long id, CouponPolicyUpdateRequest request) {
        CouponPolicy policy = couponPolicyRepository.findById(id)
                .orElseThrow(CouponPolicyNotFoundException::new);

        // 발급 이력 확인
        long issuedCount = issuedCouponRepository.countByPolicyId(id);
        if (issuedCount > 0) {
            throw new CustomException(ErrorCode.COUPON_POLICY_ALREADY_ISSUED);
        }

        policy.update(
                request.name(), request.discountType(), request.discountValue(),
                request.maxDiscountAmount(), request.minOrderAmount(), request.validDays(),
                request.totalQuantity(), request.categoryId(), request.courseIds(),
                request.couponTarget(), request.isStackable(),
                request.issueStartDate(), request.issueEndDate()
        );

        return CouponPolicyResponse.from(policy);
    }

    // 관리자 쿠폰 정책 조기 종료
    @Transactional
    public void terminateCouponPolicy(Long id) {
        CouponPolicy policy = couponPolicyRepository.findById(id)
                .orElseThrow(CouponPolicyNotFoundException::new);

        policy.terminate(LocalDateTime.now());
    }
}
