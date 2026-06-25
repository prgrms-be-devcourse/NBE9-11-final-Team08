package com.team08.backend.domain.studyreport.dto;

/**
 * 리포트 응답이 어떤 경로로 만들어졌는지 클라이언트에 알리는 상태값.
 *
 * <ul>
 *   <li>{@link #LOADED} 조회 — 신선한 기존 리포트를 그대로 반환했다.</li>
 *   <li>{@link #REGENERATED} 갱신 — 리포트가 없거나 쿨다운이 지나 새로 집계했다.</li>
 *   <li>{@link #COOLDOWN} 갱신 불가 — 갱신을 요청했으나 쿨다운 이내라 기존 리포트를 반환했다.</li>
 * </ul>
 */
public enum ReportStatus {
    LOADED,
    REGENERATED,
    COOLDOWN
}
