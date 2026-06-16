package com.team08.backend.domain.feeditem.service;

import org.springframework.stereotype.Component;

@Component
public class FeedContentSummarizer {

    private static final int MAX_LENGTH = 100;

    public String summarize(String content) {
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LENGTH) + "...";
    }
}
