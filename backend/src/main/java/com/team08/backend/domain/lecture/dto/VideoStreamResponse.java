package com.team08.backend.domain.lecture.dto;

import org.springframework.http.ResponseCookie;
import java.util.List;

public record VideoStreamResponse(String path, List<ResponseCookie> cookies) {}