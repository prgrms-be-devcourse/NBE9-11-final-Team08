package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class QnaAnswerServiceTest {

    @Mock
    private QnaAnswerRepository qnaAnswerRepository;

    @Mock
    private QnaQuestionRepository qnaQuestionRepository;

    @InjectMocks
    private QnaAnswerService qnaAnswerService;

    @Test
    @DisplayName("답변 작성 성공")
    void createAnswer_success() {
        Long questionId = 1L;
        Long instructorId = 10L;

        QnaQuestion question = mock(QnaQuestion.class);
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)).willReturn(Optional.of(question));
        given(qnaAnswerRepository.existsByQuestionId(questionId)).willReturn(false);

        QnaAnswer answer = QnaAnswer.create(questionId, instructorId, "답변 내용");
        given(qnaAnswerRepository.save(any())).willReturn(answer);

        QnaAnswerResponse response = qnaAnswerService.createAnswer(questionId, instructorId, "답변 내용");

        assertThat(response.content()).isEqualTo("답변 내용");
        assertThat(response.instructorId()).isEqualTo(instructorId);
    }

    @Test
    @DisplayName("답변 작성 실패 - 질문 없음")
    void createAnswer_questionNotFound() {
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(1L, 10L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("답변 작성 실패 - 이미 답변 존재")
    void createAnswer_alreadyExists() {
        QnaQuestion question = mock(QnaQuestion.class);
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(question));
        given(qnaAnswerRepository.existsByQuestionId(any())).willReturn(true);

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(1L, 10L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ANSWER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("답변 수정 성공")
    void updateAnswer_success() {
        Long questionId = 1L;
        Long instructorId = 10L;

        QnaAnswer answer = QnaAnswer.create(questionId, instructorId, "old content");
        given(qnaAnswerRepository.findByQuestionId(questionId)).willReturn(Optional.of(answer));

        QnaAnswerResponse response = qnaAnswerService.updateAnswer(questionId, instructorId, "new content");

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("답변 수정 실패 - 답변 없음")
    void updateAnswer_notFound() {
        given(qnaAnswerRepository.findByQuestionId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaAnswerService.updateAnswer(1L, 10L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ANSWER_NOT_FOUND);
    }

    @Test
    @DisplayName("답변 수정 실패 - 작성 강사 아님")
    void updateAnswer_accessDenied() {
        Long questionId = 1L;
        Long instructorId = 10L;
        Long otherId = 99L;

        QnaAnswer answer = QnaAnswer.create(questionId, instructorId, "content");
        given(qnaAnswerRepository.findByQuestionId(questionId)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> qnaAnswerService.updateAnswer(questionId, otherId, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    @Test
    @DisplayName("답변 삭제 성공")
    void deleteAnswer_success() {
        Long questionId = 1L;
        Long instructorId = 10L;

        QnaAnswer answer = QnaAnswer.create(questionId, instructorId, "content");
        given(qnaAnswerRepository.findByQuestionId(questionId)).willReturn(Optional.of(answer));

        assertThatCode(() -> qnaAnswerService.deleteAnswer(questionId, instructorId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("답변 삭제 실패 - 작성 강사 아님")
    void deleteAnswer_accessDenied() {
        Long questionId = 1L;

        QnaAnswer answer = QnaAnswer.create(questionId, 10L, "content");
        given(qnaAnswerRepository.findByQuestionId(questionId)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> qnaAnswerService.deleteAnswer(questionId, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }
}
