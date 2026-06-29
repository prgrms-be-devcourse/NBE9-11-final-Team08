package com.team08.backend.domain.media.entity;

public enum DlqStatus {
    /** 처리 대기 중 - 드레이너가 재시도 예정 */
    PENDING,
    /** 최대 재시도 횟수 초과 - 수동 처리 필요 */
    DEAD
}
