package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuedCouponQueryFacade {

    private final AllUsersCouponMaterializer allUsersCouponMaterializer;
    private final IssuedCouponService issuedCouponService;

    public List<CouponListResponse> getMyCoupons(Long userId) {
        allUsersCouponMaterializer.materializeForUser(userId);
        return issuedCouponService.getMyCoupons(userId);
    }
}
