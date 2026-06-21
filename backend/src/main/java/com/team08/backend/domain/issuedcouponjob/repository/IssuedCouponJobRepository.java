package com.team08.backend.domain.issuedcouponjob.repository;

import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssuedCouponJobRepository extends JpaRepository<IssuedCouponJob, Long> {
    // 재처리 대상 쿠폰 발급 작업을 요청 순서대로 최대 100개 조회
    List<IssuedCouponJob> findTop100ByStatusInOrderByRequestedAtAsc(Collection<IssuedCouponJobStatus> statuses);

    // 특정 사용자의 쿠폰 발급 작업 조회
    Optional<IssuedCouponJob> findByIdAndUserId(Long id, Long userId);
}
