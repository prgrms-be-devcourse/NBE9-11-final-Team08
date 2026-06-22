package com.team08.backend.global.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudFrontCookieSignerTest {

    private CloudFrontCookieSignerImpl cloudFrontCookieSigner;

    @BeforeEach
    void setUp() {
        cloudFrontCookieSigner = new CloudFrontCookieSignerImpl();
    }

    @Test
    void 유효한_경로로_서명된_쿠키_배열을_정상_생성한다() throws Exception {
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "distributionDomain", "cloudfront-domain");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "keyPairId", "real-id");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        String generatedPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(privateKeyBytes) +
                "\n-----END PRIVATE KEY-----";

        ReflectionTestUtils.setField(cloudFrontCookieSigner, "privateKeyPem", generatedPem);
        cloudFrontCookieSigner.init();

        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(
                "/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/*",
                "/lectures/1/"
        );

        assertThat(cookies).hasSize(3);
        assertThat(cookies[0].getName()).isEqualTo("CloudFront-Policy");
        assertThat(cookies[1].getName()).isEqualTo("CloudFront-Signature");
        assertThat(cookies[2].getName()).isEqualTo("CloudFront-Key-Pair-Id");
        assertThat(cookies[0].getValue()).doesNotContain("+", "/", "=");
        assertThat(cookies[1].getValue()).doesNotContain("+", "/", "=");
        assertThat(cookies[2].getValue()).isEqualTo("real-id");

        for (ResponseCookie cookie : cookies) {
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("None");
        }
    }

    @Test
    void 키_포맷이_잘못되면_init_시점에_예외가_발생한다() {
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "distributionDomain", "cloudfront-domain");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "keyPairId", "real-id");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "privateKeyPem", "invalid-key-format");

        assertThatThrownBy(() -> cloudFrontCookieSigner.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CloudFront 키 초기화 실패로 애플리케이션을 시작할 수 없습니다.");
    }
}