package com.team08.backend.domain.lectureqna.controller;

import com.team08.backend.domain.lectureqna.dto.QnaAnswerRequest;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.service.QnaAnswerService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
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
@RequestMapping("/api/qna/questions/{questionId}/answers")
public class QnaAnswerController {

    private final QnaAnswerService qnaAnswerService;

    @Operation(summary = "QnA 답변 작성 (강사 전용)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public QnaAnswerResponse createAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody QnaAnswerRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return qnaAnswerService.createAnswer(
                questionId, principal.user().id(), request.content());
    }

    @Operation(summary = "QnA 답변 수정 (강사 전용)")
    @PutMapping
    public QnaAnswerResponse updateAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody QnaAnswerRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return qnaAnswerService.updateAnswer(
                questionId, principal.user().id(), request.content());
    }

    @Operation(summary = "QnA 답변 삭제 (강사 전용)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    public void deleteAnswer(
            @PathVariable Long questionId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        qnaAnswerService.deleteAnswer(questionId, principal.user().id());
    }
}
