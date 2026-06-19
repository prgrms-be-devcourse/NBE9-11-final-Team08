package com.team08.backend.domain.studyreport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyProgressEntry(
        LocalDate date,
        BigDecimal progressRate
) {}
