package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.dto.CouponIssueRequestResponse;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;
import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponIssueRequestService {

    private static final String ISSUE_KEY_PREFIX = "SELECTED_USERS_";

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final UserRepository userRepository;
    private final CouponIssueRequestStreamPublisher streamPublisher;
    private final Clock clock;

    public CouponIssueRequestResponse requestUsersIssue(Long policyId, List<Long> userIds, String requestKey, Long requestedBy) {
        if (userIds == null || userIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);

        LocalDateTime now = LocalDateTime.now(clock);
        policy.validateIssuePeriod(now);

        List<Long> distinctUserIds = new LinkedHashSet<>(userIds.stream()
                .filter(id -> id != null && id > 0)
                .toList())
                .stream()
                .toList();
        if (distinctUserIds.isEmpty() || userRepository.findAllById(distinctUserIds).size() != distinctUserIds.size()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String normalizedRequestKey = normalizeRequestKey(requestKey);
        CouponIssueRequest request = CouponIssueRequest.request(
                policyId,
                normalizedRequestKey,
                CouponIssueRequestType.SELECTED_USERS,
                requestedBy,
                now
        );
        request.addRequestedCount(distinctUserIds.size());
        request.markProcessing(now);

        CouponIssueRequest savedRequest = saveRequest(request);
        String issueKey = ISSUE_KEY_PREFIX + normalizedRequestKey;
        for (Long userId : distinctUserIds) {
            streamPublisher.publish(savedRequest.getId(), policyId, userId, issueKey);
        }

        return CouponIssueRequestResponse.from(savedRequest);
    }

    private CouponIssueRequest saveRequest(CouponIssueRequest request) {
        try {
            return couponIssueRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    private String normalizeRequestKey(String requestKey) {
        if (requestKey == null || requestKey.isBlank() || requestKey.length() > 80) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return requestKey.trim();
    }
}
