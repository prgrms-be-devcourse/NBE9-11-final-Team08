package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.IssuedCouponResponse;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserRepository userRepository;

    // [사용자] 일반 쿠폰 다운로드
    @Transactional
    public IssuedCouponResponse downloadCoupon(Long userId, Long policyId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        // 일반 다운로드 쿠폰 여부 검증
        if (policy.getCouponType() != CouponType.NORMAL) {
            throw new CustomException(ErrorCode.INVALID_COUPON_TYPE);
        }

        // 중복 발급 체크
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 쿠폰 발급 기간 검증
        policy.validateIssuePeriod();

        // 쿠폰 발급 기록 생성 및 저장
        IssuedCoupon newCoupon = IssuedCoupon.issue(
                policy.getId(),
                userId,
                policy.calculateExpirationDate()
        );

        IssuedCoupon savedCoupon = issuedCouponRepository.save(newCoupon);
        return IssuedCouponResponse.from(savedCoupon);
    }

    // [사용자] 선착순 쿠폰 다운로드
    @Transactional
    public IssuedCouponResponse downloadFcfsCoupon(Long userId, Long policyId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 비관적 락을 적용한 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(policyId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        // 선착순 쿠폰 여부 검증
        if (policy.getCouponType() != CouponType.FCFS) {
            throw new CustomException(ErrorCode.INVALID_COUPON_TYPE);
        }

        // 중복 발급 체크
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 쿠폰 발급 기간 검증
        policy.validateIssuePeriod();

        // 쿠폰 수량 차감 및 재고 소진 체크
        policy.decreaseQuantity();

        // 쿠폰 발급 기록 생성 및 저장
        IssuedCoupon newCoupon = IssuedCoupon.issue(
                policy.getId(),
                userId,
                policy.calculateExpirationDate()
        );

        IssuedCoupon savedCoupon = issuedCouponRepository.save(newCoupon);
        return IssuedCouponResponse.from(savedCoupon);
    }

    // [사용자] 내 쿠폰 목록 조회
    @Transactional(readOnly = true)
    public List<CouponListResponse> getMyCoupons(Long userId) {
        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findByUserIdOrderByExpiredAtAsc(userId);

        // 쿠폰 정책 ID 목록 추출
        List<Long> policyIds = issuedCoupons.stream()
                .map(IssuedCoupon::getPolicyId)
                .distinct()
                .toList();

        // 정책 정보 한꺼번에 조회 및 맵으로 변환
        Map<Long, CouponPolicy> policyMap = couponPolicyRepository.findAllById(policyIds).stream()
                .collect(Collectors.toMap(CouponPolicy::getId, policy -> policy));

        return issuedCoupons.stream()
                .map(coupon -> {
                    CouponPolicy policy = policyMap.get(coupon.getPolicyId());
                    if (policy == null) {
                        throw new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND);
                    }
                    return CouponListResponse.of(coupon, policy);
                })
                .toList();
    }
}
