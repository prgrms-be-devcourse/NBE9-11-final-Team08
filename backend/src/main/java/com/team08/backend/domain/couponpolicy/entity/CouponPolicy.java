package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodEndedException;
import com.team08.backend.domain.couponpolicy.exception.CouponIssuePeriodNotStartedException;
import com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private Integer minOrderAmount;

    private Integer validDays;

    private Integer totalQuantity;

    private Long categoryId;

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponPolicyCourse> targetCourses = new ArrayList<>();

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

    private CouponPolicy(String name, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount, Integer minOrderAmount, Integer validDays, Integer totalQuantity, Long categoryId, List<Long> courseIds, CouponType couponType, CouponTarget couponTarget, CouponUsageType usageType, Boolean isStackable, LocalDateTime issueStartDate, LocalDateTime issueEndDate) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.validDays = validDays;
        this.totalQuantity = totalQuantity;
        this.categoryId = categoryId;
        this.couponType = couponType;
        this.couponTarget = couponTarget;
        this.usageType = usageType;
        this.isStackable = isStackable != null ? isStackable : false;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;

        if (courseIds != null) {
            for (Long courseId : courseIds) {
                this.targetCourses.add(new CouponPolicyCourse(this, courseId));
            }
        }
    }
    // TODO 멘토링 
    /* 
    빌더 패턴이 하는 부분이 나중에 갔을 땐 필드값 추가마다 추가해야 됨
    빌더 말고 별도의 객체로 만들어보면 좋다
    여러가지의 정책들로 100개 여러가지 생성 정책
    객체를 생성하는 여러가지 방법을 조건 기준마다 생성 규칙 표ㅕ준화 어디 필수 비필수 생성방법 검증
    생성 방법이 빌더로 하면 너무 단편적이다
    다양한 객체 생성 패턴을 익혀라
    빌더 현업에서 잘 안 씀
    인터페이스 두고 여러 생성방법 정의 내리기
    쿠폰에 대한 서비스 == 컴퍼넌트 
    팩토리
    빌더는 단순 (MVP만) 
    코틀린 생성자 역할이 클래스가 함
    객체 생성에 대한 다양한 방식
     */

    // 대량 데이터 서버랑 동일하다

    // 선착순 쿠폰 생성
    public static CouponPolicy createFcfsPolicy(
            String name, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount,
            Integer minOrderAmount, Integer validDays, int totalQuantity, Long categoryId, List<Long> courseIds,
            CouponTarget couponTarget, CouponUsageType usageType, Boolean isStackable,
            LocalDateTime issueStartDate, LocalDateTime issueEndDate
    ) {
        return new CouponPolicy(
                name, discountType, discountValue, maxDiscountAmount,
                minOrderAmount, validDays, totalQuantity, categoryId, courseIds,
                CouponType.FCFS, couponTarget, usageType, isStackable, issueStartDate, issueEndDate
        );
    }

    // 일반 쿠폰 생성
    public static CouponPolicy createNormalPolicy(
            String name, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount,
            Integer minOrderAmount, Integer validDays, Long categoryId, List<Long> courseIds,
            CouponTarget couponTarget, CouponUsageType usageType, Boolean isStackable,
            LocalDateTime issueStartDate, LocalDateTime issueEndDate
    ) {
        return new CouponPolicy(
                name, discountType, discountValue, maxDiscountAmount,
                minOrderAmount, validDays, null, categoryId, courseIds,
                CouponType.NORMAL, couponTarget, usageType, isStackable, issueStartDate, issueEndDate
        );
    }


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

    // TODO 나중에 결제에서 사용 
    // 할인 적용 가능 여부 확인
    public boolean isApplicableTo(Long targetCourseId, Long targetCategoryId) {
        return switch (this.couponTarget) {
            case ALL -> true;
            case CATEGORY -> this.categoryId != null && this.categoryId.equals(targetCategoryId);
            case COURSE -> this.targetCourses.stream()
                    .anyMatch(tc -> tc.getCourseId().equals(targetCourseId));
        };
    }

    // 정책 핵심 정보 수정
    public void update(String name, DiscountType discountType, Integer discountValue, Integer maxDiscountAmount,
                       Integer minOrderAmount, Integer validDays, Integer totalQuantity, Long categoryId,
                       List<Long> courseIds, CouponTarget couponTarget, Boolean isStackable,
                       LocalDateTime issueStartDate, LocalDateTime issueEndDate) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.validDays = validDays;
        this.totalQuantity = totalQuantity;
        this.categoryId = categoryId;
        this.couponTarget = couponTarget;
        this.isStackable = isStackable != null ? isStackable : false;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;

        // 강좌 대상 업데이트
        this.targetCourses.clear();
        if (courseIds != null) {
            for (Long courseId : courseIds) {
                this.targetCourses.add(new com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse(this, courseId));
            }
        }
    }

    // 쿠폰 정책 조기 종료
    public void terminate(LocalDateTime now) {
        this.issueEndDate = now;
    }
}

