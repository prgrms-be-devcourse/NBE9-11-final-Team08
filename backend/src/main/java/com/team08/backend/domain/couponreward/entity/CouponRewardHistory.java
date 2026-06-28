package com.team08.backend.domain.couponreward.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_reward_histories", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "reward_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponRewardHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false, length = 100)
    private String rewardKey;

    @Column(nullable = false, length = 50)
    private String rewardType;

    private Long issuedCouponId;

    private LocalDateTime issuedAt;

    private CouponRewardHistory(Long userId, Long policyId, String rewardKey, String rewardType) {
        this.userId = userId;
        this.policyId = policyId;
        this.rewardKey = rewardKey;
        this.rewardType = rewardType;
    }

    public static CouponRewardHistory create(Long userId, Long policyId, String rewardKey, String rewardType) {
        return new CouponRewardHistory(userId, policyId, rewardKey, rewardType);
    }

    public void markIssued(Long issuedCouponId, LocalDateTime issuedAt) {
        this.issuedCouponId = issuedCouponId;
        this.issuedAt = issuedAt;
    }
}
