package com.team08.backend.domain.lectureqna.controller;

import com.team08.backend.domain.lectureqna.dto.QnaAnswerRequest;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.service.QnaAnswerService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import com.team08.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "QnA")
@RestController
@RequiredArgsConstructor
public class QnaAnswerController {

    private final QnaAnswerService qnaAnswerService;

    @Operation(summary = "QnA 답변 작성 (강사 전용)")
    @PostMapping("/api/qna/questions/{questionId}/answers")
    public ApiResponse<QnaAnswerResponse> createAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody QnaAnswerRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return ApiResponse.success(
                qnaAnswerService.createAnswer(questionId, principal.user().id(), request.content()));
    }

    @Operation(summary = "QnA 답변 수정 (강사 전용)")
    @PutMapping("/api/qna/questions/{questionId}/answers")
    public ApiResponse<QnaAnswerResponse> updateAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody QnaAnswerRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return ApiResponse.success(
                qnaAnswerService.updateAnswer(questionId, principal.user().id(), request.content()));
    }

    @Operation(summary = "QnA 답변 삭제 (강사 전용)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/qna/questions/{questionId}/answers")
    public ApiResponse<Void> deleteAnswer(
            @PathVariable Long questionId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        qnaAnswerService.deleteAnswer(questionId, principal.user().id());
        return ApiResponse.success(null);
    }
}
