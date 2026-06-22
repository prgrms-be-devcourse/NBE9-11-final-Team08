package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class FcfsCouponRedisIssuerTest {

    private static final String STOCK_KEY = "coupon:fcfs:{1}:stock";
    private static final String ISSUED_KEY = "coupon:fcfs:{1}:issued";

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    private StringRedisTemplate redisTemplate;
    private FcfsCouponRedisIssuer fcfsCouponRedisIssuer;

    @BeforeAll
    static void beforeAll() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void afterAll() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.delete(STOCK_KEY);
        redisTemplate.delete(ISSUED_KEY);
        fcfsCouponRedisIssuer = new FcfsCouponRedisIssuer(redisTemplate);
    }

    @Test
    @DisplayName("성공: Redis Lua 스크립트로 재고를 차감하고 발급 유저를 기록한다")
    void issue_success() {
        // given
        CouponPolicy policy = policy(1L, 2);

        // when
        fcfsCouponRedisIssuer.issue(1L, policy);

        // then
        assertThat(redisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("1");
        assertThat(redisTemplate.opsForSet().isMember(ISSUED_KEY, "1")).isTrue();
    }

    @Test
    @DisplayName("실패: 이미 발급된 유저는 중복 발급 예외가 발생한다")
    void issue_fail_duplicate() {
        // given
        CouponPolicy policy = policy(1L, 2);
        fcfsCouponRedisIssuer.issue(1L, policy);

        // when & then
        assertThatThrownBy(() -> fcfsCouponRedisIssuer.issue(1L, policy))
                .isInstanceOf(CouponAlreadyIssuedException.class);
        assertThat(redisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("1");
    }

    @Test
    @DisplayName("실패: 재고가 없으면 선착순 쿠폰 소진 예외가 발생한다")
    void issue_fail_soldOut() {
        // given
        CouponPolicy policy = policy(1L, 1);
        fcfsCouponRedisIssuer.issue(1L, policy);

        // when & then
        assertThatThrownBy(() -> fcfsCouponRedisIssuer.issue(2L, policy))
                .isInstanceOf(CouponExhaustedException.class);
        assertThat(redisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("0");
        assertThat(redisTemplate.opsForSet().isMember(ISSUED_KEY, "2")).isFalse();
    }

    @Test
    @DisplayName("성공: Redis 발급 보상 시 발급 유저를 제거하고 재고를 복구한다")
    void rollback_success() {
        // given
        CouponPolicy policy = policy(1L, 1);
        fcfsCouponRedisIssuer.issue(1L, policy);

        // when
        fcfsCouponRedisIssuer.rollback(1L, 1L);

        // then
        assertThat(redisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("1");
        assertThat(redisTemplate.opsForSet().isMember(ISSUED_KEY, "1")).isFalse();
    }

    private CouponPolicy policy(Long policyId, Integer totalQuantity) {
        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getId()).thenReturn(policyId);
        when(policy.getTotalQuantity()).thenReturn(totalQuantity);
        return policy;
    }
}
