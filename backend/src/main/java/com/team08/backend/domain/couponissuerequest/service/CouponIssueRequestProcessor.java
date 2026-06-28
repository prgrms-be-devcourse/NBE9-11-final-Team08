package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.service.CouponIssueExecutor;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CouponIssueRequestProcessor {

    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserRepository userRepository;
    private final CouponIssueExecutor couponIssueExecutor;
    private final Clock clock;

    @Transactional
    public void processSelectedUser(Long requestId, Long policyId, Long userId, String issueKey) {
        LocalDateTime now = LocalDateTime.now(clock);
        CouponIssueRequest request = couponIssueRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
        if (request.isFinished()) {
            return;
        }
        request.markProcessing(now);

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
                request.increaseSuccessCount();
            } else {
                request.increaseSkippedCount();
            }
        } catch (RuntimeException e) {
            request.increaseFailedCount();
        }
        request.completeIfProcessed(now);
    }

    private void validateManualIssuePolicy(CouponPolicy policy) {
        if (policy.getCouponType() != CouponType.AUTO || policy.getAutoIssueType() != null) {
            throw new CustomException(ErrorCode.INVALID_COUPON_TYPE);
        }
    }
}
