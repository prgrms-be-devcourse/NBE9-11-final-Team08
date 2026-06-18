package com.team08.backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "챕터 목록 순서 일괄 변경 요청")
public record ChapterReorderRequest(
        @NotNull(message = "순서 변경 데이터 리스트는 필수입니다.")
        @Schema(description = "변경된 순서 매핑 리스트")
        List<ChapterOrderElement> reorders
) {
    public record ChapterOrderElement(
            @NotNull(message = "챕터 ID는 필수입니다.")
            @Schema(description = "챕터 식별 번호")
            Long chapterId,

            @NotNull(message = "순서 번호는 필수입니다.")
            @Min(value = 1, message = "순서 번호는 1 이상이어야 합니다.")
            @Schema(description = "새로운 노출 순서 번화 (1부터 시작)")
            Integer orderNo
    ) {}
}