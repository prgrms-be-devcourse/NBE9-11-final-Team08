package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponDownloadResponse;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.CouponUsageResult;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponNotFoundException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.strategy.CouponIssueResult;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategy;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategyFactory;
import com.team08.backend.domain.ordercouponusage.entity.OrderCouponUsage;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserRepository userRepository;
    private final IssuedCouponStrategyFactory strategyFactory;
    private final IssuedCouponWriter issuedCouponWriter;
    private final Clock clock;

    public CouponDownloadResponse downloadCoupon(Long userId, Long policyId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        CouponType couponType = couponPolicyRepository.findCouponTypeById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);

        IssuedCouponStrategy strategy = strategyFactory.getStrategy(couponType);
        CouponIssueResult result = strategy.issue(userId, policyId);

        if (result.status() == CouponIssueResult.Status.REQUESTED) {
            return CouponDownloadResponse.requested(result.userId(), result.policyId());
        }
        return CouponDownloadResponse.issued(result.issuedCoupon());
    }

    @Transactional(readOnly = true)
    public List<CouponListResponse> getMyCoupons(Long userId) {
        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findByUserIdOrderByExpiredAtAsc(userId);

        List<Long> policyIds = issuedCoupons.stream()
                .map(IssuedCoupon::getPolicyId)
                .distinct()
                .toList();

        Map<Long, CouponPolicy> policyMap = couponPolicyRepository.findAllById(policyIds).stream()
                .collect(Collectors.toMap(CouponPolicy::getId, policy -> policy));

        LocalDateTime now = LocalDateTime.now(clock);
        return issuedCoupons.stream()
                .map(coupon -> {
                    CouponPolicy policy = policyMap.get(coupon.getPolicyId());
                    if (policy == null) {
                        throw new CouponPolicyNotFoundException();
                    }
                    return CouponListResponse.of(coupon, policy, now);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpectedDiscountResponse calculateExpectedDiscount(Long userId, Long issuedCouponId, int originalPrice) {
        CouponUsageContext context = getUsableCouponContext(userId, issuedCouponId, LocalDateTime.now(clock));
        CouponPolicy policy = context.couponPolicy();

        int discountAmount = policy.calculateDiscountAmount(originalPrice);
        int finalPrice = Math.max(0, originalPrice - discountAmount);

        return new ExpectedDiscountResponse(
                policy.getName(),
                originalPrice,
                discountAmount,
                finalPrice
        );
    }

    @Transactional(readOnly = true)
    public int calculateExpectedDiscounts(Long userId, Map<Long, Long> itemCouponIds, Long stackableCouponId, List<OrderItem> orderItems, int orderTotalPrice) {
        LocalDateTime now = LocalDateTime.now(clock);
        int totalDiscount = 0;

        if (itemCouponIds != null) {
            for (OrderItem item : orderItems) {
                Long couponId = itemCouponIds.get(item.getCourseId());
                if (couponId != null) {
                    CouponUsageContext context = getUsableCouponContext(userId, couponId, now);
                    if (context.couponPolicy().getIsStackable() || !context.couponPolicy().isApplicableTo(item.getCourseId(), null)) {
                        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                    }
                    totalDiscount += context.couponPolicy().calculateDiscountAmount(item.getPrice());
                }
            }
        }

        if (stackableCouponId != null) {
            CouponUsageContext stackableContext = getUsableCouponContext(userId, stackableCouponId, now);
            if (!stackableContext.couponPolicy().getIsStackable()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            totalDiscount += stackableContext.couponPolicy().calculateDiscountAmount(orderTotalPrice);
        }

        return Math.min(totalDiscount, orderTotalPrice);
    }

    @Transactional
    public CouponUsageResult useCouponsForOrder(Long userId, Map<Long, Long> itemCouponIds, Long stackableCouponId, List<OrderItem> orderItems, int orderTotalPrice, Long orderId) {
        LocalDateTime now = LocalDateTime.now(clock);
        int totalDiscount = 0;
        List<OrderCouponUsage> usages = new ArrayList<>();

        if (itemCouponIds != null) {
            for (OrderItem item : orderItems) {
                Long couponId = itemCouponIds.get(item.getCourseId());
                if (couponId != null) {
                    IssuedCoupon issuedCoupon = issuedCouponRepository.findByIdWithLock(couponId)
                            .orElseThrow(CouponNotFoundException::new);
                    issuedCoupon.validateUsable(userId, now);
                    CouponPolicy policy = couponPolicyRepository.findById(issuedCoupon.getPolicyId())
                            .orElseThrow(CouponPolicyNotFoundException::new);

                    if (policy.getIsStackable() || !policy.isApplicableTo(item.getCourseId(), null)) {
                        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                    }

                    int discount = policy.calculateDiscountAmount(item.getPrice());
                    if (totalDiscount + discount > orderTotalPrice) {
                        discount = Math.max(0, orderTotalPrice - totalDiscount);
                    }
                    totalDiscount += discount;
                    usages.add(new OrderCouponUsage(orderId, couponId, discount));
                    issuedCoupon.applyUsage(policy.getUsageType(), now);
                }
            }
        }

        if (stackableCouponId != null) {
            IssuedCoupon stackableCoupon = issuedCouponRepository.findByIdWithLock(stackableCouponId)
                    .orElseThrow(CouponNotFoundException::new);
            stackableCoupon.validateUsable(userId, now);
            CouponPolicy stackablePolicy = couponPolicyRepository.findById(stackableCoupon.getPolicyId())
                    .orElseThrow(CouponPolicyNotFoundException::new);

            if (!stackablePolicy.getIsStackable()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }

            int discount = stackablePolicy.calculateDiscountAmount(orderTotalPrice);
            if (totalDiscount + discount > orderTotalPrice) {
                discount = Math.max(0, orderTotalPrice - totalDiscount);
            }
            totalDiscount += discount;
            usages.add(new OrderCouponUsage(orderId, stackableCouponId, discount));
            stackableCoupon.applyUsage(stackablePolicy.getUsageType(), now);
        }

        return new CouponUsageResult(totalDiscount, usages);
    }

    private CouponUsageContext getUsableCouponContext(Long userId, Long issuedCouponId, LocalDateTime now) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(CouponNotFoundException::new);

        issuedCoupon.validateUsable(userId, now);

        CouponPolicy policy = couponPolicyRepository.findById(issuedCoupon.getPolicyId())
                .orElseThrow(CouponPolicyNotFoundException::new);

        return new CouponUsageContext(issuedCoupon, policy);
    }

    private record CouponUsageContext(
            IssuedCoupon issuedCoupon,
            CouponPolicy couponPolicy
    ) {
    }

    @Transactional
    public void refundCoupon(Long issuedCouponId) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findByIdWithLock(issuedCouponId)
                .orElseThrow(CouponNotFoundException::new);

        issuedCoupon.refund(LocalDateTime.now(clock));
    }
}
