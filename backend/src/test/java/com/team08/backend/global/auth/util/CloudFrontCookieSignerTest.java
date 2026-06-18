package com.team08.backend.global.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFrontCookieSignerTest {

    private CloudFrontCookieSigner cloudFrontCookieSigner;

    @BeforeEach
    void setUp() throws Exception {
        cloudFrontCookieSigner = new CloudFrontCookieSigner();
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "distributionDomain", "localhost");
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "keyPairId", "dummy-id");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        String generatedPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(privateKeyBytes) +
                "\n-----END PRIVATE KEY-----";

        ReflectionTestUtils.setField(cloudFrontCookieSigner, "privateKeyPem", generatedPem);
        cloudFrontCookieSigner.init();
    }

    @Test
    void 유효한_강의ID와_UUID로_서명된_쿠키_배열을_정상_생성한다() {
        Long lectureId = 1L;
        String uuid = "sample-uuid";

        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(lectureId, uuid);

        assertThat(cookies).hasSize(3);
        assertThat(cookies[0].getName()).isEqualTo("CloudFront-Policy");
        assertThat(cookies[1].getName()).isEqualTo("CloudFront-Signature");
        assertThat(cookies[2].getName()).isEqualTo("CloudFront-Key-Pair-Id");

        assertThat(cookies[0].getValue()).doesNotContain("+", "/", "=");
        assertThat(cookies[1].getValue()).doesNotContain("+", "/", "=");
        assertThat(cookies[2].getValue()).isEqualTo("dummy-id");

        for (ResponseCookie cookie : cookies) {
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
            assertThat(cookie.getPath()).isEqualTo("/");
        }
    }
}