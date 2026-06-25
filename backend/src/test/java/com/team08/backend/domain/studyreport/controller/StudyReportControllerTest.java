package com.team08.backend.domain.studyreport.controller;

import com.team08.backend.domain.studyreport.dto.DailyProgressEntry;
import com.team08.backend.domain.studyreport.dto.ReportStatus;
import com.team08.backend.domain.studyreport.dto.StudyReportResponse;
import com.team08.backend.domain.studyreport.dto.TopLectureEntry;
import com.team08.backend.domain.studyreport.service.StudyReportService;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean StudyReportService studyReportService;

    @Test
    @WithMockLoginUser
    @DisplayName("GET /api/studies/{studyId}/report - refresh 기본값 false로 리포트 조회/생성 성공")
    void getReport_defaultRefreshFalse_success() throws Exception {
        Long studyId = 10L;
        Long userId = 1L;
        LocalDateTime nextRegenerableAt = LocalDateTime.of(2026, 6, 24, 12, 0);
        StudyReportResponse response = new StudyReportResponse(
                studyId, 3600, 5, BigDecimal.valueOf(66.67), 10,
                List.of(new TopLectureEntry(1L, "Spring Core", 1800)),
                List.of(new DailyProgressEntry(LocalDate.of(2026, 6, 7), BigDecimal.valueOf(33.33))),
                Map.of("2026-06-07", 3),
                null,
                ReportStatus.REGENERATED,
                nextRegenerableAt);

        given(studyReportService.getReport(userId, studyId, false)).willReturn(response);

        mockMvc.perform(get("/api/studies/{studyId}/report", studyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(studyId))
                .andExpect(jsonPath("$.totalWatchTime").value(3600))
                .andExpect(jsonPath("$.totalQnaCount").value(5))
                .andExpect(jsonPath("$.progressRate").value(66.67))
                .andExpect(jsonPath("$.status").value("REGENERATED"))
                .andExpect(jsonPath("$.nextRegenerableAt").value("2026-06-24T12:00:00"));

        then(studyReportService).should().getReport(userId, studyId, false);
    }

    @Test
    @WithMockLoginUser
    @DisplayName("GET /api/studies/{studyId}/report?refresh=true - refresh=true로 갱신 시도")
    void getReport_refreshTrue_success() throws Exception {
        Long studyId = 10L;
        Long userId = 1L;
        StudyReportResponse response = new StudyReportResponse(
                studyId, 5000, 4, BigDecimal.valueOf(75.00), 15,
                List.of(new TopLectureEntry(2L, "JPA 기초", 2400)),
                List.of(new DailyProgressEntry(LocalDate.of(2026, 6, 10), BigDecimal.valueOf(75.00))),
                Map.of("2026-06-10", 5),
                null,
                ReportStatus.COOLDOWN,
                LocalDateTime.of(2026, 6, 24, 13, 0));

        given(studyReportService.getReport(userId, studyId, true)).willReturn(response);

        mockMvc.perform(get("/api/studies/{studyId}/report", studyId)
                        .param("refresh", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(studyId))
                .andExpect(jsonPath("$.totalWatchTime").value(5000))
                .andExpect(jsonPath("$.totalQnaCount").value(4))
                .andExpect(jsonPath("$.progressRate").value(75.00))
                .andExpect(jsonPath("$.status").value("COOLDOWN"))
                .andExpect(jsonPath("$.nextRegenerableAt").value("2026-06-24T13:00:00"));

        then(studyReportService).should().getReport(userId, studyId, true);
    }

    @Test
    @WithMockLoginUser
    @DisplayName("GET /api/studies/{studyId}/report - 스터디 없으면 404")
    void getReport_studyNotFound_returns404() throws Exception {
        Long studyId = 99L;
        Long userId = 1L;

        given(studyReportService.getReport(userId, studyId, false))
                .willThrow(new CustomException(ErrorCode.STUDY_NOT_FOUND));

        mockMvc.perform(get("/api/studies/{studyId}/report", studyId))
                .andExpect(status().isNotFound());

        then(studyReportService).should().getReport(userId, studyId, false);
    }
}
