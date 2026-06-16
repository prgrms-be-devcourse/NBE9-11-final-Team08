package com.team08.backend.domain.feed.service;

import com.team08.backend.domain.feeditem.service.FeedContentSummarizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;



public class FeedContentSummarizerTest {

    @Test
    void content의_앞뒤공백과_연속공백을_정리한다() {
        FeedContentSummarizer summarizer = new FeedContentSummarizer();

        String result = summarizer.summarize("  오늘   학습한   내용입니다.  ");

        assertThat(result).isEqualTo("오늘 학습한 내용입니다.");
    }

    @Test
    void 공백_정리_후_100자를_초과하면_정리된_문자열을_100자까지_자른다() {
        // given
        FeedContentSummarizer summarizer = new FeedContentSummarizer();
        String content = "  " + "가".repeat(50) + "   " + "나".repeat(51) + "  ";

        // when
        String result = summarizer.summarize(content);

        // then
        String normalized = "가".repeat(50) + " " + "나".repeat(51);

        assertThat(result).isEqualTo(normalized.substring(0, 100) + "...");
    }
}
