package com.team08.backend.domain.coupon.entity;

import com.team08.backend.domain.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "coupon_policies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType couponType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponTarget couponTarget;

    private LocalDateTime issueStartDate;

    private LocalDateTime issueEndDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public CouponPolicy(String name, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount, Integer validDays, Integer totalQuantity, Category category, CouponType couponType, CouponTarget couponTarget, LocalDateTime issueStartDate, LocalDateTime issueEndDate) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validDays = validDays;
        this.totalQuantity = totalQuantity;
        this.category = category;
        this.couponType = couponType;
        this.couponTarget = couponTarget;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime calculateExpirationDate() {
        return LocalDate.now()
                .plusDays(this.validDays)
                .atTime(LocalTime.MAX);
    }

    // 쿠폰 수량 차감
    public void decreaseQuantity() {
        if (this.totalQuantity == null) {

            return; // 수량 제한이 없는 무제한 쿠폰인 경우 통과
        }
        if (this.totalQuantity <= 0) {
            throw new IllegalStateException("선착순 쿠폰이 모두 소진되었습니다.");
        }
        this.totalQuantity -= 1;
    }

    // [시스템] 할인 금액 계산 로직
    public int calculateDiscountAmount(int originalPrice) {
        if (this.discountType == DiscountType.AMOUNT) {
            // 정액 할인: (할인 금액과 원래 가격 중 작은 값 반환 - 상품가보다 더 할인되는 것 방지)
            return Math.min(this.discountValue, originalPrice);
        } else {
            // 정률 할인: (원래 가격 * 할인율)
            int discount = (int) (originalPrice * (this.discountValue / 100.0));
            // 최대 할인 금액 제한이 있는 경우
            if (this.maxDiscountAmount != null && discount > this.maxDiscountAmount) {
                return this.maxDiscountAmount;
            }
            return discount;
        }
    }
}
