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

    private CloudFrontCookieSigner cloudFrontCookieSigner;

    @BeforeEach
    void setUp() {
        cloudFrontCookieSigner = new CloudFrontCookieSigner();
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

        String resourcePath = "/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/*";
        String cookiePath = "/lectures/1/";

        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(resourcePath, cookiePath);

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
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
            assertThat(cookie.getPath()).isEqualTo(cookiePath);
        }
    }

    @Test
    void 개발_및_테스트_환경에서는_더미_서명으로_쿠키를_생성한다() {
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "distributionDomain", "cloudfront-domain");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "keyPairId", "dummy-id");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "privateKeyPem", "cloudfront-private-key");
        cloudFrontCookieSigner.init();

        String resourcePath = "/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/*";
        String cookiePath = "/lectures/1/";

        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(resourcePath, cookiePath);

        assertThat(cookies).hasSize(3);
        assertThat(cookies[1].getName()).isEqualTo("CloudFront-Signature");
        assertThat(cookies[1].getValue()).isEqualTo("dummy-signature");
        assertThat(cookies[2].getValue()).isEqualTo("dummy-id");
    }

    @Test
    void yaml설정의_PEM형식_내에_더미_키워드가_포함되어도_테스트_환경으로_인정하여_쿠키를_생성한다() {
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "distributionDomain", "cloudfront-domain");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "keyPairId", "dummy-id");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "privateKeyPem", "-----BEGIN PRIVATE KEY-----\ncloudfront-private-key\n-----END PRIVATE KEY-----");
        cloudFrontCookieSigner.init();

        String resourcePath = "/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/*";
        String cookiePath = "/lectures/1/";

        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(resourcePath, cookiePath);

        assertThat(cookies).hasSize(3);
        assertThat(cookies[1].getValue()).isEqualTo("dummy-signature");
    }

    @Test
    void 운영_환경에서_키_초기화가_실패했거나_누락된_채_서명_시도_시_예외가_발생한다() {
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "distributionDomain", "cloudfront-domain");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "keyPairId", "real-id");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "privateKeyPem", "invalid-or-missing-key-format");

        assertThatThrownBy(() -> cloudFrontCookieSigner.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CloudFront 키 초기화 실패로 애플리케이션을 시작할 수 없습니다.");
    }
}