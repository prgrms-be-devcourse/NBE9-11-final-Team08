package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueCountSyncScheduler {

    private final CouponIssueSuccessCountRedisCounter successCountRedisCounter;
    private final CouponIssueRequestRepository couponIssueRequestRepository;

    @Scheduled(fixedDelayString = "${app.coupon-issue-count-sync.fixed-delay:60000}")
    public void syncSuccessCounts() {
        try (Cursor<String> keys = successCountRedisCounter.scanSuccessCountKeys()) {
            while (keys.hasNext()) {
                syncSuccessCount(keys.next());
            }
        } catch (RuntimeException e) {
            log.error("[전체회원 쿠폰 발급 카운트 동기화] Redis 키 스캔 실패", e);
        }
    }

    private void syncSuccessCount(String key) {
        Long policyId = extractPolicyId(key);
        if (policyId == null) {
            return;
        }

        long increment = getAndReset(key);
        if (increment <= 0) {
            return;
        }

        try {
            couponIssueRequestRepository.incrementAllUsersSuccessCount(policyId, increment);
        } catch (RuntimeException e) {
            restore(key, increment);
            log.error("[전체회원 쿠폰 발급 카운트 동기화] DB 반영 실패. policyId={}, increment={}", policyId, increment, e);
        }
    }

    private void restore(String key, long increment) {
        try {
            successCountRedisCounter.restore(key, increment);
        } catch (RuntimeException e) {
            log.error("[전체회원 쿠폰 발급 카운트 동기화] Redis 카운트 복구 실패. key={}, increment={}", key, increment, e);
        }
    }

    private Long extractPolicyId(String key) {
        try {
            return successCountRedisCounter.extractPolicyId(key);
        } catch (NumberFormatException e) {
            log.warn("[전체회원 쿠폰 발급 카운트 동기화] 잘못된 Redis 키 형식입니다. key={}", key);
            return null;
        }
    }

    private long getAndReset(String key) {
        try {
            return successCountRedisCounter.getAndReset(key);
        } catch (NumberFormatException e) {
            log.warn("[전체회원 쿠폰 발급 카운트 동기화] Redis 카운트 값이 숫자가 아닙니다. key={}", key);
            return 0L;
        }
    }
}
