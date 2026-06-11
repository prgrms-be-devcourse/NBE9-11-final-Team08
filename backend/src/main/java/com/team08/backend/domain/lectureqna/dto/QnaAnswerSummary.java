package com.team08.backend.domain.lectureqna.dto;

import java.time.LocalDateTime;

public record QnaAnswerSummary(Long id, String content, LocalDateTime createdAt) {
}
