package com.team08.backend.domain.couponpolicy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
