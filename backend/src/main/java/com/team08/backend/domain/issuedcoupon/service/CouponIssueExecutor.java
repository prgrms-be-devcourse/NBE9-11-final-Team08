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
    public void issueRewardCoupon(Long userId, CouponPolicy policy, String rewardKey, String rewardType) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (couponRewardHistoryRepository.existsByUserIdAndRewardKey(userId, rewardKey)) {
            log.info("이미 처리된 쿠폰 보상입니다. userId={}, rewardKey={}", userId, rewardKey);
            return;
        }

        IssuedCoupon existingCoupon = issuedCouponRepository.findByUserIdAndPolicyIdAndIssueKey(
                        userId,
                        policy.getId(),
                        rewardKey
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
            log.info("이미 발급된 쿠폰 보상 이력을 보정했습니다. userId={}, rewardKey={}", userId, rewardKey);
            return;
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
    }
}
