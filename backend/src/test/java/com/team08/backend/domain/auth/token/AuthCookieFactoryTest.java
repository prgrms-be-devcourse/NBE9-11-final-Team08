package com.team08.backend.domain.auth.token;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieFactoryTest {

    @Test
    void accessToken_쿠키에_domain_설정이_있으면_Domain_속성을_포함한다() {
        AccessTokenCookieFactory factory = new AccessTokenCookieFactory(
                new AccessCookieProperties("accessToken", true, "Lax", "domain.store")
        );

        String cookie = factory.create("access-token", Duration.ofHours(1)).toString();

        assertThat(cookie).contains("Domain=domain.store");
    }

    @Test
    void accessToken_쿠키에_domain_설정이_비어있으면_host_only_쿠키로_생성한다() {
        AccessTokenCookieFactory factory = new AccessTokenCookieFactory(
                new AccessCookieProperties("accessToken", false, "Lax", "")
        );

        String cookie = factory.create("access-token", Duration.ofHours(1)).toString();

        assertThat(cookie).doesNotContain("Domain=");
    }

    @Test
    void refreshToken_쿠키에_domain_설정이_있으면_Domain_속성을_포함한다() {
        RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(
                new RefreshCookieProperties("refreshToken", true, "Lax", "domain.store")
        );

        String cookie = factory.create("refresh-token", Duration.ofDays(14)).toString();

        assertThat(cookie).contains("Domain=domain.store");
    }
}
