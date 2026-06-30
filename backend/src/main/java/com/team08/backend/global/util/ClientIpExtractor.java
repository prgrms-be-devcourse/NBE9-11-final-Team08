package com.team08.backend.global.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpExtractor {

    private ClientIpExtractor() {
        // 인스턴스화 방지
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getRemoteAddr();
        return (ip == null || ip.isBlank()) ? "unknown" : ip;
    }
}
