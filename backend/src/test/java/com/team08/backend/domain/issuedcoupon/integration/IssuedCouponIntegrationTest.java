package com.team08.backend.domain.issuedcoupon.integration;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IssuedCouponIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    @DisplayName("사용자가 일반 쿠폰을 다운로드하는 통합 테스트")
    void downloadCoupon_IntegrationTest() throws Exception {
        // given
        saveUser();

        CouponPolicy policy = savePolicy("일반 할인 쿠폰", CouponType.NORMAL);

        // when
        mockMvc.perform(post("/api/coupons/" + policy.getId() + "/download")
                        .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("쿠폰이 성공적으로 발급되었습니다!"));

        // then
        assertThat(issuedCouponRepository.findAll()).hasSize(1);
        assertThat(issuedCouponRepository.findAll().get(0).getPolicyId()).isEqualTo(policy.getId());
    }

    private void saveUser() {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "email", "test@example.com");
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", "테스트유저");
        ReflectionTestUtils.setField(user, "role", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        userRepository.save(user);
    }

    private CouponPolicy savePolicy(String name, CouponType type) {
        CouponPolicyCreateRequest request = new CouponPolicyCreateRequest(
                name,
                DiscountType.AMOUNT,
                1000,
                7,
                100,
                null,
                type,
                CouponTarget.ALL,
                CouponUsageType.SINGLE_USE,
                false,
                null,
                null
        );
        CouponPolicy policy = CouponPolicy.create(request);
        return couponPolicyRepository.save(policy);
    }

    private <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test entity.", e);
        }
    }
}
