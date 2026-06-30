package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodEndedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodNotStartedException;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupon_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class CouponPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponTarget couponTarget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType couponType;

    @Enumerated(EnumType.STRING)
    private AutoIssueType autoIssueType;

    private Integer totalQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponUsageType usageType;

    @Column(nullable = false)
    private Boolean isStackable = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer discountValue;

    private Integer maxDiscountAmount;

    private Integer minOrderAmount;

    private Integer validDays;

    private LocalDateTime issueStartDate;

    private LocalDateTime issueEndDate;

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponPolicyCategory> targetCategories = new ArrayList<>();

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponPolicyCourse> targetCourses = new ArrayList<>();

    private LocalDateTime deletedAt;

    private CouponPolicy(String name, CouponTarget couponTarget, CouponType couponType, AutoIssueType autoIssueType, Integer totalQuantity, CouponUsageType usageType, Boolean isStackable, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount, Integer minOrderAmount, Integer validDays, LocalDateTime issueStartDate, LocalDateTime issueEndDate, List<Long> categoryIds, List<Long> courseIds) {
        this.name = name;
        this.couponTarget = couponTarget;
        this.couponType = couponType;
        this.autoIssueType = autoIssueType;
        this.totalQuantity = totalQuantity;
        this.usageType = usageType;
        this.isStackable = isStackable != null ? isStackable : false;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.validDays = validDays;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;

        if (categoryIds != null) {
            for (Long categoryId : categoryIds) {
                this.addTargetCategory(categoryId);
            }
        }

        if (courseIds != null) {
            for (Long courseId : courseIds) {
                this.addTargetCourse(courseId);
            }
        }
    }
    
    // 쿠폰 정책 생성
    public static CouponPolicy createPolicy(
            String name, CouponTarget couponTarget, CouponType couponType, Integer totalQuantity, CouponUsageType usageType, Boolean isStackable, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount, Integer minOrderAmount, Integer validDays, LocalDateTime issueStartDate, LocalDateTime issueEndDate, List<Long> categoryIds, List<Long> courseIds
    ) {
        return createPolicy(
                name, couponTarget, couponType, null, totalQuantity, usageType, isStackable, discountType, discountValue, maxDiscountAmount, minOrderAmount, validDays, issueStartDate, issueEndDate, categoryIds, courseIds
        );
    }

    public static CouponPolicy createPolicy(
            String name, CouponTarget couponTarget, CouponType couponType, AutoIssueType autoIssueType, Integer totalQuantity, CouponUsageType usageType, Boolean isStackable, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount, Integer minOrderAmount, Integer validDays, LocalDateTime issueStartDate, LocalDateTime issueEndDate, List<Long> categoryIds, List<Long> courseIds
    ) {
        return new CouponPolicy(
                name, couponTarget, couponType, autoIssueType, totalQuantity, usageType, isStackable, discountType, discountValue, maxDiscountAmount, minOrderAmount, validDays, issueStartDate, issueEndDate, categoryIds, courseIds
        );
    }

    // 연관관계 편의 메서드
    public void addTargetCourse(Long courseId) {
        if (this.targetCourses.stream().noneMatch(tc -> tc.getCourseId().equals(courseId))) {
            this.targetCourses.add(new CouponPolicyCourse(this, courseId));
        }
    }

    public void removeTargetCourse(Long courseId) {
        this.targetCourses.removeIf(tc -> tc.getCourseId().equals(courseId));
    }

    public void addTargetCategory(Long categoryId) {
        if (this.targetCategories.stream().noneMatch(tc -> tc.getCategoryId().equals(categoryId))) {
            this.targetCategories.add(new CouponPolicyCategory(this, categoryId));
        }
    }

    public void removeTargetCategory(Long categoryId) {
        this.targetCategories.removeIf(tc -> tc.getCategoryId().equals(categoryId));
    }

    // 쿠폰 발급 후 사용기간 
    public LocalDateTime calculateExpirationDate(LocalDateTime now) {
        if (this.validDays == null) {
            return LocalDateTime.of(2099, 1, 1, 23, 59);
        }
        return now.toLocalDate()
                .plusDays(this.validDays)
                .atTime(java.time.LocalTime.MAX);
    }

    // 쿠폰 발급 기간 검증
    public void validateIssuePeriod(LocalDateTime now) {
        if (issueStartDate != null && now.isBefore(issueStartDate)) {
            throw new CouponIssuePeriodNotStartedException();
        }
        if (issueEndDate != null && now.isAfter(issueEndDate)) {
            throw new CouponIssuePeriodEndedException();
        }
    }

    // 선착순 쿠폰 수량 차감
    public void decreaseQuantity() {
        if (this.totalQuantity == null) {
            return;
        }
        if (this.totalQuantity <= 0) {
            throw new CouponExhaustedException();
        }
        this.totalQuantity--;
    }

    // 할인 금액 계산
    public int calculateDiscountAmount(int originalPrice) {
        if (this.discountType == DiscountType.AMOUNT) {
            // 정액 할인: (할인 금액과 원래 가격 중 작은 값 반환 - 상품가보다 더 할인되는 것 방지)
            return Math.min(this.discountValue, originalPrice);
        } else {
            // 정률 할인 (원 단위 내림): (원래 가격 * 할인율) / 100
            int discount = (int) ((long) originalPrice * this.discountValue / 100);

            // 최대 할인 금액 제한이 있는 경우
            if (this.maxDiscountAmount != null && discount > this.maxDiscountAmount) {
                return this.maxDiscountAmount;
            }
            return discount;
        }
    }

    // 할인 적용 가능 여부 확인
    public boolean isApplicableTo(Long targetCourseId, Long targetCategoryId) {
        return switch (this.couponTarget) {
            case ALL -> true;
            case CATEGORY -> targetCategoryId != null && this.targetCategories.stream()
                    .anyMatch(tc -> tc.getCategoryId().equals(targetCategoryId));
            case COURSE -> targetCourseId != null && this.targetCourses.stream()
                    .anyMatch(tc -> tc.getCourseId().equals(targetCourseId));
        };
    }

    // 정책 핵심 정보 수정
    public void update(String name, AutoIssueType autoIssueType, Integer totalQuantity, CouponUsageType usageType, Boolean isStackable, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount, Integer minOrderAmount, Integer validDays, LocalDateTime issueStartDate, LocalDateTime issueEndDate) {
        this.name = name;
        this.autoIssueType = autoIssueType;
        this.totalQuantity = totalQuantity;
        this.usageType = usageType;
        this.isStackable = isStackable != null ? isStackable : false;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.validDays = validDays;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;
    }

    // 쿠폰 정책 조기 종료
    public void terminate(LocalDateTime now) {
        this.issueEndDate = now;
    }

    // 쿠폰 정책 삭제
    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
