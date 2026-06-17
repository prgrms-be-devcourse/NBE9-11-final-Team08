package com.team08.backend.domain.couponpolicy.repository;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicySearchRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponStatus;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaConfig.class, CouponPolicyRepositoryCustomImpl.class})
class CouponPolicyRepositoryTest {

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;
    
    @Test
    @DisplayName("쿠폰명으로 정책을 필터링하여 조회한다")
    void findAllByCondition_filterByName() {
        // given
        savePolicy("할인 쿠폰", CouponType.NORMAL, null, null);
        savePolicy("이벤트 쿠폰", CouponType.NORMAL, null, null);

        CouponPolicySearchRequest condition = new CouponPolicySearchRequest("할인", null, null);

        // when
        Page<CouponPolicy> result = couponPolicyRepository.findAllByCondition(condition, LocalDateTime.now(), PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("할인 쿠폰");
    }

    @Test
    @DisplayName("진행 상태(진행중)로 정책을 필터링하여 조회한다")
    void findAllByCondition_filterByStatus_ongoing() {
        // given
        LocalDateTime now = LocalDateTime.now();
        savePolicy("진행중 쿠폰", CouponType.NORMAL, now.minusDays(1), now.plusDays(1));
        savePolicy("예정 쿠폰", CouponType.NORMAL, now.plusDays(1), now.plusDays(2));
        savePolicy("종료 쿠폰", CouponType.NORMAL, now.minusDays(2), now.minusDays(1));

        CouponPolicySearchRequest condition = new CouponPolicySearchRequest(null, null, CouponStatus.ONGOING);

        // when
        Page<CouponPolicy> result = couponPolicyRepository.findAllByCondition(condition, now, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("진행중 쿠폰");
    }

    @Test
    @DisplayName("쿠폰 타입(선착순)으로 정책을 필터링하여 조회한다")
    void findAllByCondition_filterByType() {
        // given
        savePolicy("일반 쿠폰", CouponType.NORMAL, null, null);
        savePolicy("선착순 쿠폰", CouponType.FCFS, null, null);

        CouponPolicySearchRequest condition = new CouponPolicySearchRequest(null, CouponType.FCFS, null);

        // when
        Page<CouponPolicy> result = couponPolicyRepository.findAllByCondition(condition, LocalDateTime.now(), PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponType()).isEqualTo(CouponType.FCFS);
    }

    @Test
    @DisplayName("상세 조회 시 연관된 강좌 목록까지 함께 조회한다")
    void findByIdWithDetails_success() {
        // given
        CouponPolicy policy = CouponPolicy.createPolicy(
                "강좌 쿠폰", CouponTarget.COURSE, CouponType.NORMAL, null, CouponUsageType.SINGLE_USE, false,
                DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, null, List.of(100L, 200L)
        );
        CouponPolicy savedPolicy = couponPolicyRepository.save(policy);

        // when
        Optional<CouponPolicy> result = couponPolicyRepository.findByIdWithDetails(savedPolicy.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTargetCourses()).hasSize(2);
        assertThat(result.get().getTargetCourses().get(0).getCourseId()).isIn(100L, 200L);
    }

    private void savePolicy(String name, CouponType type, LocalDateTime start, LocalDateTime end) {
        couponPolicyRepository.save(CouponPolicy.createPolicy(
                name, CouponTarget.ALL, type, type == CouponType.FCFS ? 100 : null, CouponUsageType.SINGLE_USE,
                false, DiscountType.AMOUNT, 1000, null, 10000, 7, start, end, null, null
        ));
    }
}
