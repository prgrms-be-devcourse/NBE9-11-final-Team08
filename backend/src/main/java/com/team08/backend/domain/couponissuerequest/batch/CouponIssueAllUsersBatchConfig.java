package com.team08.backend.domain.couponissuerequest.batch;

import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.service.CouponIssueExecutor;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CouponIssueAllUsersBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final String LAST_USER_ID_KEY = "lastUserId";
    private static final String SUCCESS_COUNT_KEY = "successCount";
    private static final String SKIPPED_COUNT_KEY = "skippedCount";
    private static final String FAILED_COUNT_KEY = "failedCount";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final CouponIssueExecutor couponIssueExecutor;
    private final Clock clock;

    @Bean
    public Job couponIssueAllUsersJob() {
        return new JobBuilder("couponIssueAllUsersJob", jobRepository)
                .start(couponIssueAllUsersStep())
                .build();
    }

    @Bean
    public Step couponIssueAllUsersStep() {
        return new StepBuilder("couponIssueAllUsersStep", jobRepository)
                .tasklet(couponIssueAllUsersTasklet(null, null, null), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet couponIssueAllUsersTasklet(
            @Value("#{jobParameters['requestId']}") Long requestId,
            @Value("#{jobParameters['policyId']}") Long policyId,
            @Value("#{jobParameters['issueKey']}") String issueKey
    ) {
        return (contribution, chunkContext) -> {
            LocalDateTime now = LocalDateTime.now(clock);
            ExecutionContext executionContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext();
            long lastUserId = executionContext.containsKey(LAST_USER_ID_KEY)
                    ? executionContext.getLong(LAST_USER_ID_KEY)
                    : 0L;

            List<Long> userIds = findNextUserIds(lastUserId);
            if (userIds.isEmpty()) {
                couponIssueRequestRepository.completeIfProcessed(requestId, now);
                log.info(
                        "[전체 회원 쿠폰 발급 배치] 완료. requestId={}, success={}, skipped={}, failed={}",
                        requestId,
                        executionContext.getLong(SUCCESS_COUNT_KEY, 0L),
                        executionContext.getLong(SKIPPED_COUNT_KEY, 0L),
                        executionContext.getLong(FAILED_COUNT_KEY, 0L)
                );
                return RepeatStatus.FINISHED;
            }

            CouponPolicy policy = couponPolicyRepository.findById(policyId)
                    .orElseThrow(CouponPolicyNotFoundException::new);
            validateManualIssuePolicy(policy);

            IssueChunkResult chunkResult = IssueChunkResult.empty();
            for (Long userId : userIds) {
                chunkResult = chunkResult.plus(issueOne(requestId, policy, userId, issueKey));
            }
            addCount(executionContext, SUCCESS_COUNT_KEY, chunkResult.successCount());
            addCount(executionContext, SKIPPED_COUNT_KEY, chunkResult.skippedCount());
            addCount(executionContext, FAILED_COUNT_KEY, chunkResult.failedCount());

            executionContext.putLong(LAST_USER_ID_KEY, userIds.get(userIds.size() - 1));
            for (int i = 0; i < userIds.size(); i++) {
                contribution.incrementReadCount();
            }
            contribution.incrementWriteCount(userIds.size());
            couponIssueRequestRepository.completeIfProcessed(requestId, now);
            return RepeatStatus.CONTINUABLE;
        };
    }

    private List<Long> findNextUserIds(long lastUserId) {
        return jdbcTemplate.queryForList("""
                        SELECT id
                        FROM users
                        WHERE id > ?
                        ORDER BY id
                        LIMIT ?
                        """,
                Long.class,
                lastUserId,
                CHUNK_SIZE
        );
    }

    private IssueChunkResult issueOne(Long requestId, CouponPolicy policy, Long userId, String issueKey) {
        try {
            CouponIssueExecutor.CouponIssueResult result = couponIssueExecutor.issueRewardCoupon(
                    userId,
                    policy,
                    issueKey,
                    "ALL_USERS"
            );

            if (result.issued()) {
                couponIssueRequestRepository.incrementSuccessCount(requestId);
                return IssueChunkResult.success();
            } else {
                couponIssueRequestRepository.incrementSkippedCount(requestId);
                return IssueChunkResult.skipped();
            }
        } catch (RuntimeException e) {
            couponIssueRequestRepository.incrementFailedCount(requestId);
            log.warn("[전체 회원 쿠폰 발급 배치] 회원 발급 실패. requestId={}, userId={}", requestId, userId, e);
            return IssueChunkResult.failed();
        }
    }

    private void addCount(ExecutionContext executionContext, String key, long count) {
        executionContext.putLong(key, executionContext.getLong(key, 0L) + count);
    }

    private void validateManualIssuePolicy(CouponPolicy policy) {
        if (policy.getCouponType() != CouponType.AUTO || policy.getAutoIssueType() != null) {
            throw new CustomException(ErrorCode.INVALID_COUPON_TYPE);
        }
    }

    private record IssueChunkResult(
            long successCount,
            long skippedCount,
            long failedCount
    ) {

        static IssueChunkResult empty() {
            return new IssueChunkResult(0, 0, 0);
        }

        static IssueChunkResult success() {
            return new IssueChunkResult(1, 0, 0);
        }

        static IssueChunkResult skipped() {
            return new IssueChunkResult(0, 1, 0);
        }

        static IssueChunkResult failed() {
            return new IssueChunkResult(0, 0, 1);
        }

        IssueChunkResult plus(IssueChunkResult other) {
            return new IssueChunkResult(
                    this.successCount + other.successCount,
                    this.skippedCount + other.skippedCount,
                    this.failedCount + other.failedCount
            );
        }
    }
}
