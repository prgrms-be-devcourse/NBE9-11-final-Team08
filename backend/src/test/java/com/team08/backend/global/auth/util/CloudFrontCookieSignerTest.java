package com.team08.backend.global.auth.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudFrontCookieSignerTest {

    private CloudFrontCookieSignerImpl cloudFrontCookieSigner;

    @Test
    void 유효한_경로로_서명된_쿠키_배열을_정상_생성한다() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        String generatedPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(privateKeyBytes) +
                "\n-----END PRIVATE KEY-----";

        cloudFrontCookieSigner = new CloudFrontCookieSignerImpl(
                "cloudfront-domain",
                "real-id",
                generatedPem,
                true,
                Clock.systemDefaultZone()
        );

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
    void 키_포맷이_잘못되면_객체_생성_시점에_예외가_발생한다() {
        assertThatThrownBy(() -> new CloudFrontCookieSignerImpl(
                "cloudfront-domain",
                "real-id",
                "invalid-key-format",
                true,
                Clock.systemDefaultZone()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CloudFront 키 초기화 실패로 애플리케이션을 시작할 수 없습니다.");
    }

    @Test
    void 설정_정보가_하나라도_누락되면_객체_생성_시점에_예외가_발생한다() {
        assertThatThrownBy(() -> new CloudFrontCookieSignerImpl(
                "",
                "real-id",
                "valid-key",
                true,
                Clock.systemDefaultZone()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CloudFront 설정 정보가 누락되었습니다.");

        assertThatThrownBy(() -> new CloudFrontCookieSignerImpl(
                "cloudfront-domain",
                null,
                "valid-key",
                true,
                Clock.systemDefaultZone()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CloudFront 설정 정보가 누락되었습니다.");
    }

    @Test
    void 비활성화_설정이어도_기본_정보가_누락되면_객체_생성_시점에_예외가_발생한다() {
        assertThatThrownBy(() -> new CloudFrontCookieSignerImpl(
                "cloudfront-domain",
                "real-id",
                null,
                false,
                Clock.systemDefaultZone()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CloudFront 설정 정보가 누락되었습니다.");
    }

    @Test
    void 비활성화_환경이면서_진짜_PEM_키가_주입되면_정상_생성_후_쿠키를_Lax_속성으로_생성한다() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        String generatedPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(privateKeyBytes) +
                "\n-----END PRIVATE KEY-----";

        cloudFrontCookieSigner = new CloudFrontCookieSignerImpl(
                "cloudfront-domain",
                "real-id",
                generatedPem,
                false,
                Clock.systemDefaultZone()
        );

        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(
                "/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/*",
                "/lectures/1/"
        );

        assertThat(cookies).hasSize(3);
        for (ResponseCookie cookie : cookies) {
            assertThat(cookie.isSecure()).isFalse();
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
        }
    }

    @Test
    void 비활성화_환경이면서_더미_키가_들어오면_파싱을_우회하고_가짜_서명을_반환한다() {
        cloudFrontCookieSigner = new CloudFrontCookieSignerImpl(
                "cloudfront-domain",
                "real-id",
                "cloudfront-private-key",
                false,
                Clock.systemDefaultZone()
        );

        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(
                "/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/*",
                "/lectures/1/"
        );

        assertThat(cookies).hasSize(3);
        assertThat(cookies[1].getName()).isEqualTo("CloudFront-Signature");
        assertThat(cookies[1].getValue()).isEqualTo("dummy-signature");

        for (ResponseCookie cookie : cookies) {
            assertThat(cookie.isSecure()).isFalse();
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
        }
    }
}