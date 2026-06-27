package com.team08.backend.domain.couponreward.service;

import com.team08.backend.domain.attendance.event.AttendanceCheckedEvent;
import com.team08.backend.domain.auth.event.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class CouponRewardEventListener {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CouponRewardService couponRewardService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignedUp(UserSignedUpEvent event) {
        couponRewardService.issueSignupReward(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAttendanceChecked(AttendanceCheckedEvent event) {
        couponRewardService.issueAttendanceStreakReward(event.userId(), event.consecutiveDays());
        couponRewardService.issueMonthlyAttendanceReward(
                event.userId(),
                YEAR_MONTH_FORMATTER.format(event.attendanceDate()),
                event.monthlyTotalDays()
        );
    }
}
