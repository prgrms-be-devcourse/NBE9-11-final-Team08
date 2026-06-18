package com.team08.backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "강의 목록 순서 일괄 변경 요청")
public record LectureReorderRequest(
        @NotNull(message = "순서 변경 데이터 리스트는 필수입니다.")
        @Schema(description = "변경된 순서 매핑 리스트")
        List<LectureOrderElement> reorders
) {
    public record LectureOrderElement(
            @NotNull(message = "강의 ID는 필수입니다.")
            @Schema(description = "강의 영상 식별 번호")
            Long lectureId,

            @NotNull(message = "순서 번호는 필수입니다.")
            @Schema(description = "새로운 노출 순서 번호 (1부터 시작)")
            Integer orderNo
    ) {}
}