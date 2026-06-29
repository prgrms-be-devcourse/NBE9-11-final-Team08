package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.dto.CouponIssueRequestResponse;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;
import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CouponIssueRequestService {

    private static final String SELECTED_USERS_ISSUE_KEY_PREFIX = "SELECTED_USERS_";

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final UserRepository userRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final AttendanceRepository attendanceRepository;
    private final CouponIssueRequestStreamPublisher streamPublisher;
    private final Clock clock;

    public Page<CouponIssueRequestResponse> getIssueRequests(Pageable pageable) {
        return couponIssueRequestRepository.findAllByOrderByRequestedAtDesc(pageable)
                .map(CouponIssueRequestResponse::from);
    }

    public CouponIssueRequestResponse getIssueRequest(Long requestId) {
        CouponIssueRequest request = couponIssueRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_ISSUE_REQUEST_NOT_FOUND));
        return CouponIssueRequestResponse.from(request);
    }

    public CouponIssueRequestResponse requestUsersIssue(Long policyId, List<Long> userIds, String requestKey, Long requestedBy) {
        if (userIds == null || userIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
        validateManualIssuePolicy(policy);

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
        Set<Long> alreadyIssuedUserIds = new HashSet<>(issuedCouponRepository.findIssuedUserIds(policyId, distinctUserIds));
        List<Long> targetUserIds = distinctUserIds.stream()
                .filter(userId -> !alreadyIssuedUserIds.contains(userId))
                .toList();

        String normalizedRequestKey = normalizeRequestKey(requestKey);
        CouponIssueRequest request = CouponIssueRequest.request(
                policyId,
                normalizedRequestKey,
                CouponIssueRequestType.SELECTED_USERS,
                requestedBy,
                now
        );
        request.addRequestedCount(distinctUserIds.size());
        request.addSkippedCount(alreadyIssuedUserIds.size());
        if (targetUserIds.isEmpty()) {
            request.markCompleted(now);
        } else {
            request.markProcessing(now);
        }

        CouponIssueRequest savedRequest = saveRequest(request);
        String issueKey = SELECTED_USERS_ISSUE_KEY_PREFIX + normalizedRequestKey;
        streamPublisher.publishAll(savedRequest.getId(), policyId, targetUserIds, issueKey);

        return CouponIssueRequestResponse.from(savedRequest);
    }

    public CouponIssueRequestResponse requestAllUsersIssue(Long policyId, String requestKey, Long requestedBy) {
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
        validateManualIssuePolicy(policy);

        LocalDateTime now = LocalDateTime.now(clock);
        policy.validateIssuePeriod(now);

        long userCount = userRepository.count();
        Long targetUserMaxId = userRepository.findMaxId().orElse(null);
        if (userCount <= 0 || targetUserMaxId == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String normalizedRequestKey = normalizeRequestKey(requestKey);
        CouponIssueRequest request = CouponIssueRequest.request(
                policyId,
                normalizedRequestKey,
                CouponIssueRequestType.ALL_USERS,
                requestedBy,
                now
        );
        request.markAllUsersGrantOpened(userCount, targetUserMaxId, now);

        CouponIssueRequest savedRequest = saveRequest(request);
        return CouponIssueRequestResponse.from(savedRequest);
    }

    public CouponIssueRequestResponse requestInactiveUsersIssue(Long policyId, int inactiveDays, Integer maxInactiveDays, String requestKey, Long requestedBy) {
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
        validateManualIssuePolicy(policy);

        LocalDateTime now = LocalDateTime.now(clock);
        policy.validateIssuePeriod(now);

        LocalDate today = now.toLocalDate();
        LocalDate thresholdDate = today.minusDays(inactiveDays);
        LocalDate limitDate = today.minusDays(maxInactiveDays != null ? maxInactiveDays : 365);

        List<Long> targetUserIds = attendanceRepository.findInactiveUserIds(thresholdDate, limitDate);
        if (targetUserIds.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Set<Long> alreadyIssuedUserIds = new HashSet<>(issuedCouponRepository.findIssuedUserIds(policyId, targetUserIds));
        List<Long> finalTargetUserIds = targetUserIds.stream()
                .filter(userId -> !alreadyIssuedUserIds.contains(userId))
                .toList();

        String normalizedRequestKey = normalizeRequestKey(requestKey);
        CouponIssueRequest request = CouponIssueRequest.request(
                policyId,
                normalizedRequestKey,
                CouponIssueRequestType.INACTIVE_USERS,
                requestedBy,
                now
        );
        request.addRequestedCount(targetUserIds.size());
        request.addSkippedCount(alreadyIssuedUserIds.size());
        
        if (finalTargetUserIds.isEmpty()) {
            request.markCompleted(now);
        } else {
            request.markProcessing(now);
        }

        CouponIssueRequest savedRequest = saveRequest(request);
        String issueKey = SELECTED_USERS_ISSUE_KEY_PREFIX + normalizedRequestKey;
        
        // Chunking the stream publishing to prevent Redis Pipeline memory spikes
        int chunkSize = 5000;
        for (int i = 0; i < finalTargetUserIds.size(); i += chunkSize) {
            int end = Math.min(finalTargetUserIds.size(), i + chunkSize);
            List<Long> chunk = finalTargetUserIds.subList(i, end);
            streamPublisher.publishAll(savedRequest.getId(), policyId, chunk, issueKey);
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

    private void validateManualIssuePolicy(CouponPolicy policy) {
        if (policy.getCouponType() != CouponType.ADMIN) {
            throw new CustomException(ErrorCode.INVALID_COUPON_TYPE);
        }
    }

    private String normalizeRequestKey(String requestKey) {
        if (requestKey == null || requestKey.isBlank() || requestKey.length() > 80) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return requestKey.trim();
    }
}
