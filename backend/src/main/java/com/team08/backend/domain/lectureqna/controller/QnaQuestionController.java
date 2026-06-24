package com.team08.backend.domain.lectureqna.controller;

import com.team08.backend.domain.lectureqna.dto.QnaQuestionRequest;
import com.team08.backend.domain.lectureqna.dto.QnaQuestionResponse;
import com.team08.backend.domain.lectureqna.service.QnaQuestionService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "QnA")
@RestController
@RequiredArgsConstructor
public class QnaQuestionController {

    private final QnaQuestionService qnaQuestionService;

    @Operation(summary = "강의 QnA 목록 조회 (질문+답변 페이징)")
    @GetMapping("/api/lectures/{lectureId}/qna")
    public Page<QnaQuestionResponse> getQna(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return qnaQuestionService.getQuestionsNAnswers(lectureId, principal.user().id(), pageable);
    }

    @Operation(summary = "QnA 질문 작성")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/lectures/{lectureId}/qna/questions")
    public QnaQuestionResponse createQuestion(
            @PathVariable Long lectureId,
            @Valid @RequestBody QnaQuestionRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return qnaQuestionService.createQuestion(lectureId, principal.user().id(), request.title(), request.content());
    }

    @Operation(summary = "QnA 질문 수정")
    @PutMapping("/api/qna/questions/{questionId}")
    public QnaQuestionResponse updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QnaQuestionRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return qnaQuestionService.updateQuestion(questionId, principal.user().id(), request.title(), request.content());
    }

    @Operation(summary = "QnA 질문 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/qna/questions/{questionId}")
    public void deleteQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        qnaQuestionService.deleteQuestion(questionId, principal.user().id());
    }
}
