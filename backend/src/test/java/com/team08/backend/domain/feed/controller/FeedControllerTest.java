package com.team08.backend.domain.feed.controller;

import com.team08.backend.domain.feed.dto.response.FeedItemResponse;
import com.team08.backend.domain.feed.entity.FeedItemType;
import com.team08.backend.domain.feed.service.FeedService;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @Test
    @WithMockLoginUser
    void 스터디_피드를_조회한다() throws Exception {
        Long studyId = 10L;

        FeedItemResponse response = new FeedItemResponse(
                100L,
                studyId,
                200L,
                FeedItemType.STUDY_ACTIVITY,
                1L,
                "스터디 활동 내용",
                LocalDateTime.of(2026, 6, 19, 10, 0)
        );

        given(feedService.getPagedFeedItems(eq(studyId), eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/studies/{studyId}/feed", studyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.content[0].type").value("STUDY_ACTIVITY"))
                .andExpect(jsonPath("$.content[0].sourceId").value(1L))
                .andExpect(jsonPath("$.content[0].studyId").value(studyId))
                .andExpect(jsonPath("$.content[0].actorId").value(200L))
                .andExpect(jsonPath("$.content[0].content").value("스터디 활동 내용"));

        then(feedService).should()
                .getPagedFeedItems(eq(studyId), eq(1L), any(Pageable.class));
    }

    @Test
    @WithMockLoginUser
    void 피드_조회시_pageable을_service로_전달한다() throws Exception {
        // given
        Long studyId = 10L;

        given(feedService.getPagedFeedItems(eq(studyId), eq(1L), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        mockMvc.perform(get("/api/studies/{studyId}/feed", studyId)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        then(feedService).should()
                .getPagedFeedItems(eq(studyId), eq(1L), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    @WithMockLoginUser
    void 스터디_접근_권한이_없으면_에러() throws Exception {
        Long studyId = 10L;

        given(feedService.getPagedFeedItems(eq(studyId), eq(1L), any(Pageable.class)))
                .willThrow(new CustomException(ErrorCode.STUDY_ACCESS_DENIED));

        mockMvc.perform(get("/api/studies/{studyId}/feed", studyId))
                .andExpect(status().isForbidden());

        then(feedService).should()
                .getPagedFeedItems(eq(studyId), eq(1L), any(Pageable.class));
    }
}
