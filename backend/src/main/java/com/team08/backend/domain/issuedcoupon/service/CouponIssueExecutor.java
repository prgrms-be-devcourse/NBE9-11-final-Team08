package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponreward.entity.CouponRewardHistory;
import com.team08.backend.domain.couponreward.repository.CouponRewardHistoryRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueExecutor {

    private final CouponRewardHistoryRepository couponRewardHistoryRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    @Transactional
    public CouponIssueResult issueRewardCoupon(Long userId, CouponPolicy policy, String rewardKey, String rewardType) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (couponRewardHistoryRepository.existsByUserIdAndRewardKey(userId, rewardKey)) {
            return CouponIssueResult.skipped();
        }

        IssuedCoupon existingCoupon = issuedCouponRepository.findByUserIdAndPolicyId(
                        userId,
                        policy.getId()
                )
                .orElse(null);
        if (existingCoupon != null) {
            CouponRewardHistory history = CouponRewardHistory.create(
                    userId,
                    policy.getId(),
                    rewardKey,
                    rewardType
            );
            history.markIssued(existingCoupon.getId(), existingCoupon.getIssuedAt());
            couponRewardHistoryRepository.save(history);
            return CouponIssueResult.skipped();
        }

        CouponRewardHistory history = CouponRewardHistory.create(
                userId,
                policy.getId(),
                rewardKey,
                rewardType
        );
        couponRewardHistoryRepository.save(history);

        policy.validateIssuePeriod(now);
        IssuedCoupon issuedCoupon = IssuedCoupon.create(policy, userId, rewardKey, now);
        IssuedCoupon savedCoupon = issuedCouponRepository.save(issuedCoupon);
        history.markIssued(savedCoupon.getId(), now);
        return CouponIssueResult.issued(savedCoupon.getId());
    }

    public record CouponIssueResult(
            boolean issued,
            Long issuedCouponId
    ) {

        public static CouponIssueResult issued(Long issuedCouponId) {
            return new CouponIssueResult(true, issuedCouponId);
        }

        public static CouponIssueResult skipped() {
            return new CouponIssueResult(false, null);
        }
    }
}
