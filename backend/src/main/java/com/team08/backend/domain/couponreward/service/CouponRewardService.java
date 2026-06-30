package com.team08.backend.domain.couponreward.service;

import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.service.CouponIssueExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    private final CouponIssueExecutor couponIssueExecutor;
    private final Clock clock;

    @Transactional
    public void issueSignupReward(Long userId) {
        issueRewardByAutoIssueType(userId, AutoIssueType.SIGNUP, "SIGNUP");
    }

    @Transactional
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

    @Transactional
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
        couponIssueExecutor.issueRewardCoupon(userId, policy, rewardKey, autoIssueType.name());
    }
}
