package com.team08.backend.domain.couponissuerequest.repository;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponIssueRequestRepository extends JpaRepository<CouponIssueRequest, Long> {

    Optional<CouponIssueRequest> findByIssueTypeAndRequestKey(CouponIssueRequestType issueType, String requestKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CouponIssueRequest r WHERE r.id = :id")
    Optional<CouponIssueRequest> findByIdForUpdate(@Param("id") Long id);
}
