package com.team08.backend.domain.dashboard.dto;

import java.util.List;

/**
 * 강의 "자주 본 구간"(YouTube 의 most-replayed 와 유사) 히트맵 응답.
 *
 * <p>VIDEO_START(재생/재개) → VIDEO_END(정지) 이벤트를 사용자별로 페어링해 시청 구간을 만들고,
 * 강의 길이를 {@code bins} 개의 동일 폭 구간으로 나눠 각 구간을 덮은 시청 횟수를 센다.
 * {@code heat} 는 최대 구간 대비 0~1 정규화 값으로, 그래프 높이에 바로 쓸 수 있다.
 *
 * @param binSeconds    한 구간의 대표 폭(초, 반올림)
 * @param totalIntervals 페어링된 시청 구간 총수
 * @param viewerCount   재생 이벤트를 남긴 고유 시청자 수
 * @param bins          구간별 시청 횟수/열기(heat)
 * @param hotspots      가장 많이 본 상위 구간(자주 본 구간)
 */
public record LectureReplayResponse(
        Long lectureId,
        String title,
        int durationSeconds,
        int binSeconds,
        long totalIntervals,
        long viewerCount,
        List<Bin> bins,
        List<Hotspot> hotspots
) {

    public record Bin(int index, int startSeconds, int endSeconds, long count, double heat) {
    }

    public record Hotspot(int startSeconds, int endSeconds, long count, double heat) {
    }
}
