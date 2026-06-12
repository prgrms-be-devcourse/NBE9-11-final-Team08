package com.team08.backend.domain.issuedcoupon.scheduler;

import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final IssuedCouponRepository issuedCouponRepository;

    // [시스템] 매일 자정에 만료된 쿠폰 자동 처리 (ISSUED -> EXPIRED)
    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void expireCoupons() {
        int updatedCount = issuedCouponRepository.expirePastCoupons(
                CouponStatus.ISSUED,
                CouponStatus.EXPIRED
        );
        if (updatedCount > 0) {
            log.info("[Scheduler] 만료된 쿠폰 {}건을 EXPIRED 상태로 변경했습니다.", updatedCount);
        }
    }
}
