package com.team08.backend.global.auth.util;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class MockCloudFrontCookieSigner implements CloudFrontCookieSigner {

    @Override
    public ResponseCookie[] createSignedCookies(String resourcePath, String cookiePath) {
        return new ResponseCookie[]{
                ResponseCookie.from("CloudFront-Policy", "dummy-policy").path(cookiePath).build(),
                ResponseCookie.from("CloudFront-Signature", "dummy-signature").path(cookiePath).build(),
                ResponseCookie.from("CloudFront-Key-Pair-Id", "dummy-id").path(cookiePath).build()
        };
    }
}