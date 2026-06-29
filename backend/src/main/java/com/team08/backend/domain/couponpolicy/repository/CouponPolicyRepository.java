package com.team08.backend.domain.couponpolicy.repository;

import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long>, CouponPolicyRepositoryCustom {

    // 쿠폰 타입으로 정책 단건 조회 (주로 자동 발급용 AUTO 타입 조회 시 사용)
    Optional<CouponPolicy> findByCouponType(CouponType couponType);

    // 쿠폰 이름으로 정책 단건 조회
    Optional<CouponPolicy> findByName(String name);

    // 비관적 락을 적용한 쿠폰 정책 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponPolicy c WHERE c.id = :id")
    Optional<CouponPolicy> findByIdWithLock(@Param("id") Long id);

    // 쿠폰 타입 조회
    @Query("SELECT c.couponType FROM CouponPolicy c WHERE c.id = :id")
    Optional<CouponType> findCouponTypeById(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CouponPolicy c SET c.totalQuantity = c.totalQuantity - 1 WHERE c.id = :id AND c.totalQuantity > 0")
    int decreaseQuantity(@Param("id") Long id);

    @Query("""
            SELECT c
            FROM CouponPolicy c
            WHERE c.autoIssueType = :autoIssueType
              AND (c.issueStartDate IS NULL OR c.issueStartDate <= :now)
              AND (c.issueEndDate IS NULL OR c.issueEndDate > :now)
            """)
    Optional<CouponPolicy> findActiveByAutoIssueType(
            @Param("autoIssueType") AutoIssueType autoIssueType,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT COUNT(c) > 0
            FROM CouponPolicy c
            WHERE c.autoIssueType = :autoIssueType
              AND (c.issueStartDate IS NULL OR c.issueStartDate <= :now)
              AND (c.issueEndDate IS NULL OR c.issueEndDate > :now)
            """)
    boolean existsActiveByAutoIssueType(
            @Param("autoIssueType") AutoIssueType autoIssueType,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT COUNT(c) > 0
            FROM CouponPolicy c
            WHERE c.autoIssueType = :autoIssueType
              AND c.id <> :id
              AND (c.issueStartDate IS NULL OR c.issueStartDate <= :now)
              AND (c.issueEndDate IS NULL OR c.issueEndDate > :now)
            """)
    boolean existsActiveByAutoIssueTypeAndIdNot(
            @Param("autoIssueType") AutoIssueType autoIssueType,
            @Param("id") Long id,
            @Param("now") LocalDateTime now
    );
}
