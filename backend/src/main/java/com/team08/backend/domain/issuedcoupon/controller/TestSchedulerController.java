package com.team08.backend.domain.issuedcoupon.controller;

import com.team08.backend.domain.issuedcoupon.scheduler.IssuedCouponScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test/coupons")
@RequiredArgsConstructor
@Tag(name = "테스트 API", description = "성능 테스트 등을 위한 임시 API")
public class TestSchedulerController {

    private final IssuedCouponScheduler issuedCouponScheduler;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/expire-trigger")
    @Operation(summary = "쿠폰 만료 스케줄러 강제 실행", description = "성능 테스트 중 마감 처리(Bulk Update)에 의한 DB Lock 간섭을 확인하기 위해 강제로 스케줄러를 실행합니다.")
    public Map<String, String> triggerScheduler() {
        long startTime = System.currentTimeMillis();
        issuedCouponScheduler.expireCoupons();
        long endTime = System.currentTimeMillis();
        
        return Map.of(
            "status", "SUCCESS",
            "executionTimeMs", String.valueOf(endTime - startTime)
        );
    }

    /**
     * 더미 만료 쿠폰 데이터 대량 생성.
     *
     * <p>batchUpdate()는 JDBC URL에 rewriteBatchedStatements=true 가 없으면
     * 내부적으로 건별 PreparedStatement 실행과 동일하게 동작한다.
     * 따라서 VALUES 절을 직접 이어 붙이는 단일 multi-row INSERT 를 사용해
     * DB round-trip 을 count/batchSize 회로 최소화한다.</p>
     *
     * @param count    생성할 레코드 수 (기본 500,000)
     * @param batchSize 한 번의 INSERT 에 포함할 행 수 (기본 5,000)
     */
    @PostMapping("/seed-expired")
    @Operation(summary = "더미 만료 쿠폰 데이터 대량 생성",
            description = "마감 테스트를 위해 만료된 쿠폰 데이터를 대량으로 생성합니다. (기본 50만 개)")
    public Map<String, String> seedExpiredCoupons(
            @RequestParam(defaultValue = "500000") int count,
            @RequestParam(defaultValue = "5000")   int batchSize
    ) {
        long startTime = System.currentTimeMillis();

        Timestamp now     = Timestamp.valueOf(LocalDateTime.now());
        Timestamp expired = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        // policy_id 99999 고정, user_id 1_000_000~ 증가 → UNIQUE(user_id, policy_id) 충돌 없음
        long baseUserId = 1_000_000L;
        int totalInserted = 0;

        String nowStr     = now.toString();
        String expiredStr = expired.toString();
        String prefix = "INSERT IGNORE INTO issued_coupons (policy_id, user_id, status, issued_at, expired_at) VALUES ";

        for (int offset = 0; offset < count; offset += batchSize) {
            int rows = Math.min(batchSize, count - offset);
            StringBuilder sql = new StringBuilder(prefix);

            for (int i = 0; i < rows; i++) {
                long uid = baseUserId + offset + i;
                sql.append("(99999,").append(uid)
                   .append(",'ISSUED','").append(nowStr)
                   .append("','").append(expiredStr).append("')");
                if (i < rows - 1) sql.append(',');
            }

            jdbcTemplate.execute(sql.toString());
            totalInserted += rows;
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("[Test API] 더미 만료 쿠폰 {}건 생성 완료 ({}ms, batchSize={})",
                totalInserted, elapsedMs, batchSize);

        return Map.of(
                "status", "SUCCESS",
                "createdCount", String.valueOf(totalInserted),
                "executionTimeMs", String.valueOf(elapsedMs)
        );
    }
}
