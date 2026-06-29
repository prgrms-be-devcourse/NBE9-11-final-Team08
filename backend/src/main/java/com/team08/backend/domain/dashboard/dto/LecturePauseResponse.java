package com.team08.backend.domain.dashboard.dto;

import java.util.List;

/**
 * 강의 "어려워서 멈춘 구간" 히트맵 응답.
 *
 * <p>학습 도메인에선 많이 본 구간보다 학습자가 멈춘(어려워한) 구간이 더 중요하다.
 * VIDEO_PAUSE 이벤트의 멈춤 위치를 강의 길이 {@code bins} 개 버킷에 직접 누적해
 * 멈춤이 몰린 구간을 찾는다. (시작/재개 페어링이 없어 자원도 절약된다.)
 * {@code heat} 는 최대 구간 대비 0~1 정규화 값으로, 그래프 높이에 바로 쓸 수 있다.
 *
 * @param binSeconds  한 구간의 대표 폭(초, 반올림)
 * @param totalPauses 집계된 멈춤 이벤트 총수
 * @param viewerCount 멈춤 이벤트를 남긴 고유 학습자 수
 * @param bins        구간별 멈춤 횟수/열기(heat)
 * @param hotspots    멈춤이 가장 많은 상위 구간(어려워한 구간)
 */
public record LecturePauseResponse(
        Long lectureId,
        String title,
        int durationSeconds,
        int binSeconds,
        long totalPauses,
        long viewerCount,
        List<Bin> bins,
        List<Hotspot> hotspots
) {

    public record Bin(int index, int startSeconds, int endSeconds, long count, double heat) {
    }

    public record Hotspot(int startSeconds, int endSeconds, long count, double heat) {
    }
}
