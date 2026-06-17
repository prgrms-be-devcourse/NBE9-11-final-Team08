package com.team08.backend.domain.issuedcoupon.integration;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponWriter;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private IssuedCouponService issuedCouponService;

    @MockitoBean
    private IssuedCouponWriter issuedCouponWriter;

    @BeforeEach
    void setUp() {
        // IssuedCouponWriter가 받은 객체를 그대로 Repository에 저장하고 반환하도록 설정 (테스트 트랜잭션 유지를 위해)
        when(issuedCouponWriter.saveWithConcurrencyProtection(any(IssuedCoupon.class)))
                .thenAnswer(invocation -> issuedCouponRepository.save(invocation.getArgument(0)));
    }

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
        mockMvc.perform(post("/api/coupons/" + policy.getId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(policy.getId()))
                .andExpect(jsonPath("$.status").value("ISSUED"));

        // then
        assertThat(issuedCouponRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("사용자의 보유 쿠폰 목록 조회 통합 테스트")
    void getMyCoupons_IntegrationTest() throws Exception {
        // given
        User user = saveUser("list_test@example.com");
        setUpSecurityContext(user);
        CouponPolicy policy = savePolicy("조회용 쿠폰", CouponType.NORMAL);
        issuedCouponService.downloadCoupon(user.getId(), policy.getId());

        // when
        mockMvc.perform(get("/api/coupons/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].couponName").value("조회용 쿠폰"))
                .andExpect(jsonPath("$.length()").value(1));

        // then
        assertThat(issuedCouponRepository.findByUserIdOrderByExpiredAtAsc(user.getId())).hasSize(1);
    }

    private User saveUser(String email) {
        User user = createUserInstance();
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", "테스트유저");
        ReflectionTestUtils.setField(user, "role", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return userRepository.save(user);
    }

    private void setUpSecurityContext(User user) {
        LoginUserDto loginUserDto = new LoginUserDto(user.getId(), user.getEmail(), user.getNickname(), user.getRole().name());
        LoginUserPrincipal principal = LoginUserPrincipal.from(loginUserDto);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private CouponPolicy savePolicy(String name, CouponType type) {
        return couponPolicyRepository.save(CouponPolicy.createPolicy(
                name, CouponTarget.ALL, type, type == CouponType.FCFS ? 100 : null,
                CouponUsageType.SINGLE_USE, false, DiscountType.AMOUNT, 1000, null, 0, 7, null, null, null, null
        ));
    }

    private User createUserInstance() {
        try {
            var constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test User entity.", e);
        }
    }
}
