package com.team08.backend.domain.coupon.scheduler;

import com.team08.backend.domain.coupon.entity.CouponStatus;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final IssuedCouponRepository issuedCouponRepository;

    // 매일 자정(00시 00분 00초)에 실행
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireCoupons() {
        processCouponExpiration();
    }

    //서버 기동 완료 시 즉시 만료된 쿠폰 처리
    @Async // 별도 스레드 사용
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void processOnStartup() {
        log.info("애플리케이션 시작: 다운타임 누락 쿠폰 처리 시작");
        processCouponExpiration();
        log.info("애플리케이션 시작: 누락 쿠폰 처리 완료");
    }

    // 실제 만료 처리 로직 (중복 제거)
    private void processCouponExpiration() {
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = issuedCouponRepository.expirePastCoupons(
                now,
                CouponStatus.ISSUED,
                CouponStatus.EXPIRED
        );
        log.info("만료된 쿠폰 {}개가 EXPIRED 상태로 변경되었습니다.", updatedCount);
    }
}
