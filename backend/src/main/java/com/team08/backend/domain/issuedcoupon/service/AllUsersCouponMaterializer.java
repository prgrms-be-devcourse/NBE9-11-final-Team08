package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyException;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllUsersCouponMaterializer {

    private static final String ALL_USERS_ISSUE_KEY = "ALL_USERS";

    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final IssuedCouponWriter issuedCouponWriter;
    private final Clock clock;

    public void materializeForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<CouponPolicy> policies = couponIssueRequestRepository.findMaterializableAllUsersPolicies(userId, now);
        if (policies.isEmpty()) {
            return;
        }

        java.util.List<IssuedCoupon> couponsToSave = new java.util.ArrayList<>();
        distinctByPolicyId(policies).forEach(policy -> {
            try {
                policy.validateIssuePeriod(now);
                IssuedCoupon coupon = IssuedCoupon.create(policy, userId, ALL_USERS_ISSUE_KEY, now);
                couponsToSave.add(coupon);
            } catch (CouponPolicyException e) {
                log.debug("[전체회원 쿠폰 발급] 발급 기간이 만료되었습니다. userId={}, policyId={}", userId, policy.getId());
            }
        });

        if (!couponsToSave.isEmpty()) {
            try {
                java.util.List<IssuedCoupon> savedCoupons = issuedCouponWriter.saveAllWithConcurrencyProtection(couponsToSave);
                for (IssuedCoupon saved : savedCoupons) {
                    couponIssueRequestRepository.incrementSuccessCountForAllUsers(saved.getPolicyId(), 1L);
                }
            } catch (CouponAlreadyIssuedException e) {
                for (IssuedCoupon coupon : couponsToSave) {
                    try {
                        IssuedCoupon saved = issuedCouponWriter.saveWithConcurrencyProtection(coupon);
                        couponIssueRequestRepository.incrementSuccessCountForAllUsers(saved.getPolicyId(), 1L);
                    } catch (CouponAlreadyIssuedException ignore) {
                    }
                }
            }
        }
    }

    private List<CouponPolicy> distinctByPolicyId(List<CouponPolicy> policies) {
        LinkedHashMap<Long, CouponPolicy> policyMap = new LinkedHashMap<>();
        for (CouponPolicy policy : policies) {
            policyMap.putIfAbsent(policy.getId(), policy);
        }
        return List.copyOf(policyMap.values());
    }
}
