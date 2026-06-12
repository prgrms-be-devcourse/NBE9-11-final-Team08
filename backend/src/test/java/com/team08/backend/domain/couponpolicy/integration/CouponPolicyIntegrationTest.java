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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                7,
                50,
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
                        .header("Authorization", "Bearer dummy-token") // 보안 필터 통과용 헤더
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // then
        assertThat(couponPolicyRepository.findAll()).hasSize(1);
        assertThat(couponPolicyRepository.findAll().get(0).getName()).isEqualTo("통합 테스트 쿠폰");
    }
}
