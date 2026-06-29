package com.team08.backend.domain.couponissuerequest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssueSuccessCountRedisCounter {

    private static final String KEY_PREFIX = "coupon:issue_request:";
    private static final String KEY_SUFFIX = ":success_count";
    private static final String KEY_PATTERN = KEY_PREFIX + "*" + KEY_SUFFIX;

    private final StringRedisTemplate redisTemplate;

    public void incrementSuccessCount(Long policyId) {
        redisTemplate.opsForValue().increment(successCountKey(policyId));
    }

    public Cursor<String> scanSuccessCountKeys() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(KEY_PATTERN)
                .count(1000)
                .build();
        return redisTemplate.scan(options);
    }

    public long getAndReset(String key) {
        String value = redisTemplate.opsForValue().getAndSet(key, "0");
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    public void restore(String key, long count) {
        if (count > 0) {
            redisTemplate.opsForValue().increment(key, count);
        }
    }

    public Long extractPolicyId(String key) {
        if (key == null || !key.startsWith(KEY_PREFIX) || !key.endsWith(KEY_SUFFIX)) {
            return null;
        }

        String policyId = key.substring(KEY_PREFIX.length(), key.length() - KEY_SUFFIX.length());
        if (policyId.isBlank()) {
            return null;
        }
        return Long.valueOf(policyId);
    }

    private String successCountKey(Long policyId) {
        return KEY_PREFIX + policyId + KEY_SUFFIX;
    }
}
