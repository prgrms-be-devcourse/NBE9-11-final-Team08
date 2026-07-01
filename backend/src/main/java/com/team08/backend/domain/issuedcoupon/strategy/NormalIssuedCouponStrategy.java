package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class NormalIssuedCouponStrategy extends AbstractIssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final IssuedCouponWriter issuedCouponWriter;
    private final TransactionTemplate transactionTemplate;

    public NormalIssuedCouponStrategy(
            IssuedCouponRepository issuedCouponRepository,
            Clock clock,
            CouponPolicyRepository couponPolicyRepository,
            IssuedCouponWriter issuedCouponWriter,
            TransactionTemplate transactionTemplate
    ) {
        super(clock);
        this.issuedCouponRepository = issuedCouponRepository;
        this.couponPolicyRepository = couponPolicyRepository;
        this.issuedCouponWriter = issuedCouponWriter;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CouponType getSupportedType() {
        return CouponType.NORMAL;
    }

    @Override
    protected CouponPolicy findPolicy(Long policyId) {
        return couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
    }

    @Override
    protected void validateDuplicateIssue(Long userId, Long policyId) {
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new CouponAlreadyIssuedException();
        }
    }

    @Override
    protected CouponIssueResult processIssue(Long userId, CouponPolicy policy, LocalDateTime now) {
        return transactionTemplate.execute(status -> {
            IssuedCoupon newCoupon = IssuedCoupon.create(policy, userId, now);
            IssuedCoupon savedCoupon = issuedCouponWriter.saveWithConcurrencyProtection(newCoupon);
            return CouponIssueResult.issued(savedCoupon);
        });
    }
}
