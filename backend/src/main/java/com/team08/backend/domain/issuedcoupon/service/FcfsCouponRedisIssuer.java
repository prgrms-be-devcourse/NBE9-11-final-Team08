package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FcfsCouponRedisIssuer {

    private static final Long ISSUE_SUCCESS = 1L;
    private static final Long ALREADY_ISSUED = -1L;
    private static final Long SOLD_OUT = -2L;
    private static final String KEY_PREFIX = "coupon:fcfs:";

    private static final DefaultRedisScript<Long> ISSUE_SCRIPT = createIssueScript();

    private final StringRedisTemplate redisTemplate;

    private static DefaultRedisScript<Long> createIssueScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/fcfs-coupon-issue.lua")));
        script.setResultType(Long.class);
        return script;
    }

    // 선착순 쿠폰 발급 확정
    public void issue(Long userId, CouponPolicy policy) {
        Long policyId = policy.getId();
        String totalQuantity = policy.getTotalQuantity() == null ? "" : String.valueOf(policy.getTotalQuantity());

        Long result = redisTemplate.execute(
                ISSUE_SCRIPT,
                List.of(issuedKey(policyId), stockKey(policyId)),
                String.valueOf(userId),
                totalQuantity
        );

        if (ALREADY_ISSUED.equals(result)) {
            throw new CouponAlreadyIssuedException();
        }
        if (SOLD_OUT.equals(result)) {
            throw new CouponExhaustedException();
        }
        if (!ISSUE_SUCCESS.equals(result)) {
            throw new IllegalStateException("선착순 쿠폰 Redis 발급 처리에 실패했습니다.");
        }
    }

    private String stockKey(Long policyId) {
        return KEY_PREFIX + "{" + policyId + "}:stock";
    }

    private String issuedKey(Long policyId) {
        return KEY_PREFIX + "{" + policyId + "}:issued";
    }
}
