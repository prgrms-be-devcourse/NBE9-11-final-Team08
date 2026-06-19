package com.team08.backend.domain.feed.controller;

import com.team08.backend.domain.feed.dto.response.FeedCursor;
import com.team08.backend.domain.feed.dto.response.FeedCursorResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 19, 10, 0);

        FeedItemResponse response = new FeedItemResponse(
                100L,
                studyId,
                200L,
                "테스트유저",
                FeedItemType.STUDY_ACTIVITY,
                1L,
                "스터디 활동 내용",
                occurredAt
        );

        given(feedService.getFeedItems(eq(studyId), eq(1L), eq(null), eq(null), eq(10)))
                .willReturn(new FeedCursorResponse(
                        List.of(response),
                        new FeedCursor(occurredAt, 100L),
                        true
                ));

        mockMvc.perform(get("/api/studies/{studyId}/feed", studyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(100L))
                .andExpect(jsonPath("$.items[0].type").value("STUDY_ACTIVITY"))
                .andExpect(jsonPath("$.items[0].sourceId").value(1L))
                .andExpect(jsonPath("$.items[0].studyId").value(studyId))
                .andExpect(jsonPath("$.items[0].actorId").value(200L))
                .andExpect(jsonPath("$.items[0].actorNickname").value("테스트유저"))
                .andExpect(jsonPath("$.items[0].content").value("스터디 활동 내용"))
                .andExpect(jsonPath("$.nextCursor.id").value(100L))
                .andExpect(jsonPath("$.hasNext").value(true));

        then(feedService).should()
                .getFeedItems(eq(studyId), eq(1L), eq(null), eq(null), eq(10));
    }

    @Test
    @WithMockLoginUser
    void 피드_조회시_cursor와_size를_service로_전달한다() throws Exception {
        // given
        Long studyId = 10L;
        LocalDateTime cursorOccurredAt = LocalDateTime.of(2026, 6, 19, 10, 0);

        given(feedService.getFeedItems(
                eq(studyId),
                eq(1L),
                eq(cursorOccurredAt),
                eq(100L),
                eq(5)
        )).willReturn(new FeedCursorResponse(List.of(), null, false));

        // when
        mockMvc.perform(get("/api/studies/{studyId}/feed", studyId)
                        .param("cursorOccurredAt", "2026-06-19T10:00:00")
                        .param("cursorId", "100")
                        .param("size", "5"))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<LocalDateTime> cursorOccurredAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Long> cursorIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

        then(feedService).should()
                .getFeedItems(
                        eq(studyId),
                        eq(1L),
                        cursorOccurredAtCaptor.capture(),
                        cursorIdCaptor.capture(),
                        sizeCaptor.capture()
                );

        assertThat(cursorOccurredAtCaptor.getValue()).isEqualTo(cursorOccurredAt);
        assertThat(cursorIdCaptor.getValue()).isEqualTo(100L);
        assertThat(sizeCaptor.getValue()).isEqualTo(5);
    }

    @Test
    @WithMockLoginUser
    void 스터디_접근_권한이_없으면_에러() throws Exception {
        Long studyId = 10L;

        given(feedService.getFeedItems(eq(studyId), eq(1L), eq(null), eq(null), eq(10)))
                .willThrow(new CustomException(ErrorCode.STUDY_ACCESS_DENIED));

        mockMvc.perform(get("/api/studies/{studyId}/feed", studyId))
                .andExpect(status().isForbidden());

        then(feedService).should()
                .getFeedItems(eq(studyId), eq(1L), eq(null), eq(null), eq(10));
    }
}
