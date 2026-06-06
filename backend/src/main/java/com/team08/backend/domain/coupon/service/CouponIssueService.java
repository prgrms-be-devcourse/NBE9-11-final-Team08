package com.team08.backend.domain.coupon.service;

import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import com.team08.backend.domain.coupon.repository.CouponRepository;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public void issueFirstComeCoupon(Long userId, Long couponId, LocalDateTime requestTime) {

        // 1. [실패 방어] 시간 검증 (오전 10시 이전인지 확인)
        if (requestTime.getHour() < 10) {
            throw new IllegalArgumentException("쿠폰 발급은 오전 10시부터 가능합니다.");
        }

        // 2. [실패 방어] 중복 발급 검증
        if (issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 3. [실패 방어] 수량 소진 검증 (1000개 제한)
        long issuedCount = issuedCouponRepository.countByCouponId(couponId);
        if (issuedCount >= 1000) {
            throw new IllegalStateException("선착순 쿠폰이 모두 소진되었습니다.");
        }

        // 4. [성공 로직] 쿠폰 발급 (저장)
        // (주의: IssuedCoupon 엔티티 구조에 맞게 Mock이나 Builder로 생성해야 합니다.
        // 현재는 로직 흐름을 완성하기 위해 임시 객체를 만들어 save 합니다.)

        // 만약 IssuedCoupon에 @Builder가 없다면 User때처럼 임시로 Mock을 쓰거나 @Builder를 추가해 주세요!
        IssuedCoupon newCoupon = IssuedCoupon.builder()
                //.userId(userId)        // 엔티티에 유저 아이디(또는 User 객체)가 들어간다면 세팅
                //.couponId(couponId)    // 쿠폰 아이디 세팅
                .build();

        issuedCouponRepository.save(newCoupon);
    }
}
