package com.team08.backend.domain.lectureprogress.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 재생 중 하트비트로 보내는 진행 정보 갱신 요청.
 * 재생 중 주기적으로 호출하며(클라이언트가 정하기 나름(10분이하), 이에 대한 서버 요청 횟수 제한도 있으면 좋을 듯), 강의를 떠날 때(클라이언트: beacon) 마지막으로 한 번 더 호출한다.
 */
public record LectureProgressUpdateRequest(

        @NotNull
        @PositiveOrZero
        Integer positionSeconds,

        /** 직전 하트비트 이후 실제로 재생된 초. 일시정지/탐색 시간은 제외하고 클라이언트가 계산한다.
         * 백엔드에서는 상한선을 제한하여 조작데이터에 대해 대비한다.
         * */

        @NotNull
        @PositiveOrZero
        Integer watchedDeltaSeconds
) {}
