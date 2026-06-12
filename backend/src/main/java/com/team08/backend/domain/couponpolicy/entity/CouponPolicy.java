package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.global.common.BaseTimeEntity;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer discountValue;

    private Integer maxDiscountAmount;

    @Column(nullable = false)
    private Integer validDays;

    private Integer totalQuantity;

    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType couponType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponTarget couponTarget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponUsageType usageType;

    @Column(nullable = false)
    private Boolean isStackable = false;

    private LocalDateTime issueStartDate;

    private LocalDateTime issueEndDate;

    @Builder(access = AccessLevel.PRIVATE)
    private CouponPolicy(String name, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount, Integer validDays, Integer totalQuantity, Long categoryId, CouponType couponType, CouponTarget couponTarget, CouponUsageType usageType, Boolean isStackable, LocalDateTime issueStartDate, LocalDateTime issueEndDate) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validDays = validDays;
        this.totalQuantity = totalQuantity;
        this.categoryId = categoryId;
        this.couponType = couponType;
        this.couponTarget = couponTarget;
        this.usageType = usageType;
        this.isStackable = isStackable != null ? isStackable : false;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;
    }

    public static CouponPolicy create(CouponPolicyCreateRequest request) {
        return CouponPolicy.builder()
                .name(request.name())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .maxDiscountAmount(null)
                .validDays(request.validDays())
                .totalQuantity(request.totalQuantity())
                .categoryId(request.categoryId())
                .couponType(request.couponType())
                .couponTarget(request.couponTarget())
                .usageType(request.usageType())
                .isStackable(request.isStackable())
                .issueStartDate(request.issueStartDate())
                .issueEndDate(request.issueEndDate())
                .build();
    }

    public LocalDateTime calculateExpirationDate() {
        return java.time.LocalDate.now()
                .plusDays(this.validDays)
                .atTime(java.time.LocalTime.MAX);
    }

    // 쿠폰 발급 기간 검증
    public void validateIssuePeriod() {
        LocalDateTime now = LocalDateTime.now();
        if (issueStartDate != null && now.isBefore(issueStartDate)) {
            throw new CustomException(ErrorCode.COUPON_ISSUE_PERIOD_NOT_STARTED);
        }
        if (issueEndDate != null && now.isAfter(issueEndDate)) {
            throw new CustomException(ErrorCode.COUPON_ISSUE_PERIOD_ENDED);
        }
    }

    // 선착순 쿠폰 수량 차감
    public void decreaseQuantity() {
        if (this.totalQuantity == null) {
            return;
        }
        if (this.totalQuantity <= 0) {
            throw new CustomException(ErrorCode.COUPON_EXHAUSTED);
        }
        this.totalQuantity--;
    }
}
