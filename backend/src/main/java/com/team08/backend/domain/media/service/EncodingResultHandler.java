package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.dto.EncodingContext;

public interface EncodingResultHandler {
    void handleSuccess(EncodingContext context);
}