package com.team08.backend.domain.couponpolicy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("관리자 쿠폰 정책 생성 API 통합 테스트")
    void createCouponPolicy_IntegrationTest() throws Exception {
        // given
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                "통합 테스트 쿠폰",
                DiscountType.PERCENT,
                10,
                null,
                50,
                7,
                null,
                null,
                null,
                CouponType.NORMAL,
                CouponTarget.ALL,
                CouponUsageType.SINGLE_USE,
                false,
                null,
                null
        );

        // when
        mockMvc.perform(post("/api/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // then
        assertThat(couponPolicyRepository.findAll()).hasSize(1);
        assertThat(couponPolicyRepository.findAll().get(0).getName()).isEqualTo("통합 테스트 쿠폰");
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("관리자 쿠폰 정책 목록 조회 API 통합 테스트")
    void getCoupons_IntegrationTest() throws Exception {
        // given
        couponPolicyRepository.save(com.team08.backend.domain.couponpolicy.entity.CouponPolicy.createNormalPolicy(
                "쿠폰1", DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));
        couponPolicyRepository.save(com.team08.backend.domain.couponpolicy.entity.CouponPolicy.createNormalPolicy(
                "쿠폰2", DiscountType.AMOUNT, 2000, null, 20000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));

        // when & then
        mockMvc.perform(get("/api/admin/coupons")
                        .param("name", "쿠폰")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("쿠폰2")) // ID desc order
                .andExpect(jsonPath("$.content[1].name").value("쿠폰1"));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("관리자 쿠폰 정책 상세 조회 API 통합 테스트")
    void getCoupon_IntegrationTest() throws Exception {
        // given
        com.team08.backend.domain.couponpolicy.entity.CouponPolicy policy = couponPolicyRepository.save(com.team08.backend.domain.couponpolicy.entity.CouponPolicy.createNormalPolicy(
                "상세 쿠폰", DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));

        // when & then
        mockMvc.perform(get("/api/admin/coupons/" + policy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(policy.getId()))
                .andExpect(jsonPath("$.name").value("상세 쿠폰"));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("발급 이력이 없는 쿠폰 정책 수정 API 통합 테스트")
    void updateCoupon_IntegrationTest_Success() throws Exception {
        // given
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.createNormalPolicy(
                "기존 쿠폰", DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));

        CouponPolicyUpdateRequest updateRequest = new CouponPolicyUpdateRequest(
                "수정 쿠폰", DiscountType.PERCENT, 10, 5000, 20000, 14, null, null, null, CouponTarget.ALL, true, null, null
        );

        // when
        mockMvc.perform(put("/api/admin/coupons/" + policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정 쿠폰"))
                .andExpect(jsonPath("$.discountType").value("PERCENT"));

        // then
        CouponPolicy updatedPolicy = couponPolicyRepository.findById(policy.getId()).orElseThrow();
        assertThat(updatedPolicy.getName()).isEqualTo("수정 쿠폰");
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("발급 이력이 있는 쿠폰 정책 수정 시 실패 응답을 반환한다")
    void updateCoupon_IntegrationTest_FailWhenIssued() throws Exception {
        // given
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.createNormalPolicy(
                "기존 쿠폰", DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));

        issuedCouponRepository.save(IssuedCoupon.create(policy, 1L, LocalDateTime.now()));

        CouponPolicyUpdateRequest updateRequest = new CouponPolicyUpdateRequest(
                "수정 시도", DiscountType.AMOUNT, 2000, null, 20000, 7, null, null, null, CouponTarget.ALL, false, null, null
        );

        // when & then
        mockMvc.perform(put("/api/admin/coupons/" + policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COUPON_010"));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("쿠폰 정책 조기 종료 API 통합 테스트")
    void terminateCoupon_IntegrationTest() throws Exception {
        // given
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.createNormalPolicy(
                "종료 예정 쿠폰", DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));

        // when
        mockMvc.perform(patch("/api/admin/coupons/" + policy.getId() + "/terminate"))
                .andExpect(status().isNoContent());

        // then
        CouponPolicy terminatedPolicy = couponPolicyRepository.findById(policy.getId()).orElseThrow();
        assertThat(terminatedPolicy.getIssueEndDate()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Autowired
    private jakarta.persistence.EntityManager em;

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("발급 이력이 없는 쿠폰 정책 소프트 삭제 API 통합 테스트")
    void deleteCoupon_IntegrationTest_Success() throws Exception {
        // given
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.createNormalPolicy(
                "삭제할 쿠폰", DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));
        em.flush();
        em.clear();

        // when
        mockMvc.perform(delete("/api/admin/coupons/" + policy.getId()))
                .andExpect(status().isNoContent());

        em.flush();
        em.clear();

        // then
        // @SQLRestriction 때문에 findById로 조회되지 않아야 함
        assertThat(couponPolicyRepository.findById(policy.getId())).isEmpty();
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("발급 이력이 있는 쿠폰 정책 삭제 시 실패 응답을 반환한다")
    void deleteCoupon_IntegrationTest_FailWhenIssued() throws Exception {
        // given
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.createNormalPolicy(
                "삭제 시도", DiscountType.AMOUNT, 1000, null, 10000, 7, null, null, CouponTarget.ALL, CouponUsageType.SINGLE_USE, false, null, null
        ));
        issuedCouponRepository.save(IssuedCoupon.create(policy, 1L, LocalDateTime.now()));

        // when & then
        mockMvc.perform(delete("/api/admin/coupons/" + policy.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COUPON_011"));
    }
}
