package com.team08.backend.domain.issuedcoupon.integration;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.*;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @DisplayName("사용자가 일반 쿠폰을 다운로드하는 통합 테스트")
    void downloadCoupon_IntegrationTest() throws Exception {
        // given
        User user = saveUser("test@example.com");
        setUpSecurityContext(user);
        CouponPolicy policy = savePolicy("일반 할인 쿠폰", CouponType.NORMAL);

        // when
        mockMvc.perform(post("/api/coupons/" + policy.getId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(policy.getId()))
                .andExpect(jsonPath("$.status").value("ISSUED"));

        // then
        assertThat(issuedCouponRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("사용자가 선착순 쿠폰을 다운로드하는 통합 테스트")
    void downloadFcfsCoupon_IntegrationTest() throws Exception {
        // given
        User user = saveUser("fcfs_test@example.com");
        setUpSecurityContext(user);
        CouponPolicy policy = savePolicy("선착순 100명 쿠폰", CouponType.FCFS);

        // when
        mockMvc.perform(post("/api/coupons/" + policy.getId() + "/download-fcfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(policy.getId()))
                .andExpect(jsonPath("$.status").value("ISSUED"));

        // then
        assertThat(issuedCouponRepository.findAll()).hasSize(1);
    }

    private User saveUser(String email) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", "테스트유저");
        ReflectionTestUtils.setField(user, "role", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return userRepository.save(user);
    }

    private void setUpSecurityContext(User user) {
        LoginUserDto loginUserDto = new LoginUserDto(user.getId(), user.getEmail(), user.getNickname(), user.getRole().name());
        LoginUserPrincipal principal = new LoginUserPrincipal(loginUserDto);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
