package com.team08.backend.domain.couponreward.repository;

import com.team08.backend.domain.couponreward.entity.CouponRewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRewardHistoryRepository extends JpaRepository<CouponRewardHistory, Long> {
}
