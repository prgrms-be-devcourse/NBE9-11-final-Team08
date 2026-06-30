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

    public LocalDateTime calculateExpirationDate(LocalDateTime now) {
        if (this.validDays == null) {
            return LocalDateTime.of(2099, 1, 1, 23, 59);
        }
        return now.toLocalDate()
                .plusDays(this.validDays)
                .atTime(java.time.LocalTime.MAX);
    }

    public void validateIssuePeriod(LocalDateTime now) {
        if (issueStartDate != null && now.isBefore(issueStartDate)) {
            throw new CouponIssuePeriodNotStartedException();
        }
        if (issueEndDate != null && now.isAfter(issueEndDate)) {
            throw new CouponIssuePeriodEndedException();
        }
    }

    public void decreaseQuantity() {
        if (this.totalQuantity == null) {
            return;
        }
        if (this.totalQuantity <= 0) {
            throw new CouponExhaustedException();
        }
        this.totalQuantity--;
    }

    public int calculateDiscountAmount(int originalPrice) {
        if (this.discountType == DiscountType.AMOUNT) {
            return Math.min(this.discountValue, originalPrice);
        } else {
            int discount = (int) ((long) originalPrice * this.discountValue / 100);

            if (this.maxDiscountAmount != null && discount > this.maxDiscountAmount) {
                return this.maxDiscountAmount;
            }
            return discount;
        }
    }

    public boolean isApplicableTo(Long targetCourseId, List<Long> targetCategoryIds) {
        return switch (this.couponTarget) {
            case ALL -> true;
            case CATEGORY -> targetCategoryIds != null && !targetCategoryIds.isEmpty() && this.targetCategories.stream()
                    .anyMatch(tc -> targetCategoryIds.contains(tc.getCategoryId()));
            case COURSE -> targetCourseId != null && this.targetCourses.stream()
                    .anyMatch(tc -> tc.getCourseId().equals(targetCourseId));
        };
    }

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

    public void terminate(LocalDateTime now) {
        this.issueEndDate = now;
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
