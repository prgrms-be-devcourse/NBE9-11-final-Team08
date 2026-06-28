package com.team08.backend.domain.couponissuerequest.repository;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponIssueRequestRepository extends JpaRepository<CouponIssueRequest, Long> {

    Optional<CouponIssueRequest> findByIssueTypeAndRequestKey(CouponIssueRequestType issueType, String requestKey);
}
