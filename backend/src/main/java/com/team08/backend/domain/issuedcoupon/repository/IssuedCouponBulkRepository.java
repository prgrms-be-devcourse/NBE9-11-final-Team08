package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class IssuedCouponBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    public Set<Long> findIssuedUserIds(Long policyId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }

        String placeholders = String.join(",", userIds.stream().map(id -> "?").toList());
        String sql = """
                SELECT user_id
                FROM issued_coupons
                WHERE policy_id = ?
                  AND user_id IN (%s)
                """.formatted(placeholders);

        Object[] args = new Object[userIds.size() + 1];
        args[0] = policyId;
        for (int i = 0; i < userIds.size(); i++) {
            args[i + 1] = userIds.get(i);
        }

        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, args));
    }

    public int bulkInsertIssuedCoupons(CouponPolicy policy, List<Long> userIds, String issueKey, LocalDateTime now) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        LocalDateTime expiredAt = policy.calculateExpirationDate(now);
        int[][] results = jdbcTemplate.batchUpdate(
                """
                        INSERT IGNORE INTO issued_coupons
                            (policy_id, user_id, issue_key, status, issued_at, expired_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                userIds,
                userIds.size(),
                (PreparedStatement ps, Long userId) -> {
                    ps.setLong(1, policy.getId());
                    ps.setLong(2, userId);
                    ps.setString(3, issueKey);
                    ps.setString(4, CouponStatus.ISSUED.name());
                    ps.setTimestamp(5, Timestamp.valueOf(now));
                    ps.setTimestamp(6, Timestamp.valueOf(expiredAt));
                }
        );

        int insertedCount = 0;
        for (int[] batchResults : results) {
            for (int result : batchResults) {
                if (result > 0) {
                    insertedCount += result;
                }
            }
        }
        return insertedCount;
    }
}
