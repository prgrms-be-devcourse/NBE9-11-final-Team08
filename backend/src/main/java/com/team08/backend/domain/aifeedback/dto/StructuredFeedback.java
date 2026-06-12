package com.team08.backend.domain.aifeedback.dto;

import java.util.List;

public record StructuredFeedback(
        String summary,
        List<String> strengths,
        List<String> improvements,
        List<String> nextSteps
) {
}
