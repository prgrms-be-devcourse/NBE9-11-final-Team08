package com.team08.backend.domain.lecture.dto;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import java.util.List;

public record VideoStreamResponse(String path, List<ResponseCookie> cookies) {

    public VideoStreamResponse {
        cookies = List.copyOf(cookies);
    }

    public HttpHeaders asHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        cookies.forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie.toString()));
        return headers;
    }
}