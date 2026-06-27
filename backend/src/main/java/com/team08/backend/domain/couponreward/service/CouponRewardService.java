package com.team08.backend.domain.couponreward.service;

import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.couponreward.entity.CouponRewardHistory;
import com.team08.backend.domain.couponreward.repository.CouponRewardHistoryRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRewardService {

    private static final int ATTENDANCE_STREAK_REWARD_INTERVAL_DAYS = 7;
    private static final int MONTHLY_ATTENDANCE_REWARD_DAYS = 15;

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRewardHistoryRepository couponRewardHistoryRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueSignupReward(Long userId) {
        issueRewardByAutoIssueType(userId, AutoIssueType.SIGNUP, "SIGNUP");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueAttendanceStreakReward(Long userId, int consecutiveDays) {
        if (consecutiveDays <= 0 || consecutiveDays % ATTENDANCE_STREAK_REWARD_INTERVAL_DAYS != 0) {
            return;
        }
        issueRewardByAutoIssueType(
                userId,
                AutoIssueType.ATTENDANCE_STREAK,
                "ATTENDANCE_STREAK_" + consecutiveDays
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueMonthlyAttendanceReward(Long userId, String yearMonth, int monthlyTotalDays) {
        if (monthlyTotalDays != MONTHLY_ATTENDANCE_REWARD_DAYS) {
            return;
        }
        issueRewardByAutoIssueType(
                userId,
                AutoIssueType.MONTHLY_ATTENDANCE,
                "MONTHLY_ATTENDANCE_" + MONTHLY_ATTENDANCE_REWARD_DAYS + "_" + yearMonth
        );
    }

    private void issueRewardByAutoIssueType(Long userId, AutoIssueType autoIssueType, String rewardKey) {
        LocalDateTime now = LocalDateTime.now(clock);
        CouponPolicy policy = couponPolicyRepository.findActiveByAutoIssueType(autoIssueType, now)
                .orElse(null);
        if (policy == null) {
            log.warn("자동 발급 쿠폰 정책을 찾을 수 없습니다. autoIssueType={}, userId={}", autoIssueType, userId);
            return;
        }
        issueReward(userId, policy, rewardKey, autoIssueType.name(), now);
    }

    private void issueReward(Long userId, CouponPolicy policy, String rewardKey, String rewardType, LocalDateTime now) {
        CouponRewardHistory history = CouponRewardHistory.create(
                userId,
                policy.getId(),
                rewardKey,
                rewardType
        );

        try {
            couponRewardHistoryRepository.saveAndFlush(history);
        } catch (DataIntegrityViolationException e) {
            log.info("이미 처리된 쿠폰 보상입니다. userId={}, rewardKey={}", userId, rewardKey);
            return;
        }

        policy.validateIssuePeriod(now);
        IssuedCoupon issuedCoupon = IssuedCoupon.create(policy, userId, rewardKey, now);
        IssuedCoupon savedCoupon = issuedCouponRepository.saveAndFlush(issuedCoupon);
        history.markIssued(savedCoupon.getId(), now);
    }
}
