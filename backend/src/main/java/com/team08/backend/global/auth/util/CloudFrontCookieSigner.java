package com.team08.backend.global.auth.util;

import org.springframework.http.ResponseCookie;

public interface CloudFrontCookieSigner {
    ResponseCookie[] createSignedCookies(String resourcePath, String cookiePath);
}