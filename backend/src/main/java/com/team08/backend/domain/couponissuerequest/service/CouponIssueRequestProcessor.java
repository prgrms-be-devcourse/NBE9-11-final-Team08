package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponBulkRepository;
import com.team08.backend.domain.issuedcoupon.service.CouponIssueExecutor;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CouponIssueRequestProcessor {

    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserRepository userRepository;
    private final CouponIssueExecutor couponIssueExecutor;
    private final IssuedCouponBulkRepository issuedCouponBulkRepository;
    private final Clock clock;

    @Transactional
    public void processSelectedUser(Long requestId, Long policyId, Long userId, String issueKey) {
        LocalDateTime now = LocalDateTime.now(clock);

        try {
            if (!userRepository.existsById(userId)) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            CouponPolicy policy = couponPolicyRepository.findById(policyId)
                    .orElseThrow(CouponPolicyNotFoundException::new);
            validateManualIssuePolicy(policy);
            CouponIssueExecutor.CouponIssueResult result = couponIssueExecutor.issueRewardCoupon(
                    userId,
                    policy,
                    issueKey,
                    "SELECTED_USERS"
            );

            if (result.issued()) {
                couponIssueRequestRepository.incrementSuccessCount(requestId);
            } else {
                couponIssueRequestRepository.incrementSkippedCount(requestId);
            }
        } catch (RuntimeException e) {
            couponIssueRequestRepository.incrementFailedCount(requestId);
        }
        couponIssueRequestRepository.completeIfProcessed(requestId, now);
    }

    @Transactional
    public void processSelectedUsers(List<SelectedUserIssueCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        Map<BatchKey, List<SelectedUserIssueCommand>> groupedCommands = commands.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        command -> new BatchKey(command.requestId(), command.policyId(), command.issueKey()),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        groupedCommands.forEach(this::processSelectedUserBatch);
    }

    private void processSelectedUserBatch(BatchKey batchKey, List<SelectedUserIssueCommand> commands) {
        LocalDateTime now = LocalDateTime.now(clock);
        long successCount = 0;
        long skippedCount = 0;
        long failedCount = 0;

        try {
            CouponPolicy policy = couponPolicyRepository.findById(batchKey.policyId())
                    .orElseThrow(CouponPolicyNotFoundException::new);
            validateManualIssuePolicy(policy);
            policy.validateIssuePeriod(now);

            List<Long> userIds = commands.stream()
                    .map(SelectedUserIssueCommand::userId)
                    .distinct()
                    .toList();
            Set<Long> existingUserIds = new HashSet<>(userRepository.findExistingIds(userIds));

            List<Long> validUserIds = userIds.stream()
                    .filter(existingUserIds::contains)
                    .toList();
            failedCount += userIds.size() - validUserIds.size();

            Set<Long> issuedUserIds = issuedCouponBulkRepository.findIssuedUserIds(batchKey.policyId(), validUserIds);
            List<Long> targetUserIds = validUserIds.stream()
                    .filter(userId -> !issuedUserIds.contains(userId))
                    .toList();
            skippedCount += validUserIds.size() - targetUserIds.size();

            int insertedCount = issuedCouponBulkRepository.bulkInsertIssuedCoupons(
                    policy,
                    targetUserIds,
                    batchKey.issueKey(),
                    now
            );
            successCount += insertedCount;
            skippedCount += targetUserIds.size() - insertedCount;
        } catch (RuntimeException e) {
            failedCount += commands.size();
        }

        couponIssueRequestRepository.incrementProcessCounts(
                batchKey.requestId(),
                successCount,
                skippedCount,
                failedCount
        );
        couponIssueRequestRepository.completeIfProcessed(batchKey.requestId(), now);
    }

    private void validateManualIssuePolicy(CouponPolicy policy) {
        if (policy.getCouponType() != CouponType.ADMIN) {
            throw new CustomException(ErrorCode.INVALID_COUPON_TYPE);
        }
    }

    private record BatchKey(Long requestId, Long policyId, String issueKey) {
    }

    public record SelectedUserIssueCommand(Long requestId, Long policyId, Long userId, String issueKey) {
    }
}
