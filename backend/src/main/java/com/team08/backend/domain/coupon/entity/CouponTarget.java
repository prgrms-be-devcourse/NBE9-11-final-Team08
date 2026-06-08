package com.team08.backend.domain.coupon.entity;

public enum CouponTarget {
    ALL,  // 전체 (강의, 도서 둘 다 사용 가능)
    COURSE,  // 강의 전용
    BOOK  // 도서 전용
}
