package com.team08.backend.domain.couponpolicy.repository;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicySearchRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CouponPolicyRepositoryCustom {

    Page<CouponPolicy> findAllByCondition(CouponPolicySearchRequest condition, Pageable pageable);
    
    Optional<CouponPolicy> findByIdWithDetails(Long id);
}
