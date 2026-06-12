package com.team08.backend.domain.aifeedback.generator;

import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;

public interface AiFeedbackGenerator {

    StructuredFeedback generateKorean(String content);

    String modelName();

    String promptVersion();
}
