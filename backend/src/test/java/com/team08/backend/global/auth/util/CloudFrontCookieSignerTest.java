package com.team08.backend.global.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CloudFrontCookieSignerTest {

    private CloudFrontCookieSigner cloudFrontCookieSigner;

    @Mock
    private Environment environment;

    @BeforeEach
    void setUp() throws Exception {
        given(environment.getActiveProfiles()).willReturn(new String[]{"test"});
        cloudFrontCookieSigner = new CloudFrontCookieSigner(environment);
        ReflectionTestUtils.setField(cloudFrontCookieSigner, "distributionDomain", "localhost");
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
    }

    @Test
    void 유효한_경로로_서명된_쿠키_배열을_정상_생성한다() {
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
}