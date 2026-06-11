package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.dto.QnaQuestionResponse;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class QnaQuestionServiceTest {

    @Mock
    private QnaQuestionRepository qnaQuestionRepository;

    @Mock
    private QnaAnswerRepository qnaAnswerRepository;

    @Mock
    private LectureRepository lectureRepository;

    @InjectMocks
    private QnaQuestionService qnaQuestionService;

    @Test
    @DisplayName("질문 작성 성공")
    void createQuestion_success() {
        // given
        Long lectureId = 1L;
        Long userId = 1L;
        Lecture lecture = mock(Lecture.class);
        given(lecture.getDeletedAt()).willReturn(null);
        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
        QnaQuestion question = QnaQuestion.create(userId, lectureId, "제목", "내용");
        given(qnaQuestionRepository.save(any())).willReturn(question);

        // when
        QnaQuestionResponse response = qnaQuestionService.createQuestion(lectureId, userId, "제목", "내용");

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.answer()).isNull();
    }

    @Test
    @DisplayName("질문 작성 실패 - 강의 없음")
    void createQuestion_lectureNotFound() {
        // given
        given(lectureRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> qnaQuestionService.createQuestion(1L, 1L, "title", "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND);
    }

    @Test
    @DisplayName("질문 수정 성공")
    void updateQuestion_success() {
        // given
        Long questionId = 1L;
        Long userId = 1L;
        QnaQuestion question = QnaQuestion.create(userId, 1L, "old title", "old content");
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)).willReturn(Optional.of(question));
        given(qnaQuestionRepository.save(any())).willReturn(question);
        given(qnaAnswerRepository.findByQuestionId(questionId)).willReturn(Optional.empty());

        // when
        QnaQuestionResponse response = qnaQuestionService.updateQuestion(questionId, userId, "new title", "new content");

        // then
        assertThat(response).isNotNull();
        assertThat(response.answer()).isNull();
    }

    @Test
    @DisplayName("질문 수정 실패 - 질문 없음")
    void updateQuestion_notFound() {
        // given
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> qnaQuestionService.updateQuestion(1L, 1L, "title", "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("질문 수정 실패 - 작성자 아님")
    void updateQuestion_accessDenied() {
        // given
        Long questionId = 1L;
        Long ownerId = 1L;
        Long otherId = 2L;
        QnaQuestion question = QnaQuestion.create(ownerId, 1L, "title", "content");
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)).willReturn(Optional.of(question));

        // when & then
        assertThatThrownBy(() -> qnaQuestionService.updateQuestion(questionId, otherId, "title", "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    @Test
    @DisplayName("질문 삭제 성공")
    void deleteQuestion_success() {
        // given
        Long questionId = 1L;
        Long userId = 1L;
        QnaQuestion question = QnaQuestion.create(userId, 1L, "title", "content");
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)).willReturn(Optional.of(question));
        given(qnaQuestionRepository.save(any())).willReturn(question);

        // when & then
        assertThatCode(() -> qnaQuestionService.deleteQuestion(questionId, userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("질문 삭제 실패 - 작성자 아님")
    void deleteQuestion_accessDenied() {
        // given
        Long questionId = 1L;
        QnaQuestion question = QnaQuestion.create(1L, 1L, "title", "content");
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)).willReturn(Optional.of(question));

        // when & then
        assertThatThrownBy(() -> qnaQuestionService.deleteQuestion(questionId, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    @Test
    @DisplayName("질문 목록 조회 - 답변 포함")
    void getQuestions_N_Answers_withAnswer() {
        // given
        Long lectureId = 1L;

        QnaQuestion q = mock(QnaQuestion.class);
        given(q.getId()).willReturn(1L);
        given(q.getLectureId()).willReturn(lectureId);
        given(q.getUserId()).willReturn(1L);
        given(q.getTitle()).willReturn("title");
        given(q.getContent()).willReturn("content");
        given(q.getCreatedAt()).willReturn(java.time.LocalDateTime.now());
        given(q.getUpdatedAt()).willReturn(java.time.LocalDateTime.now());

        Page<QnaQuestion> page = new PageImpl<>(List.of(q));
        given(qnaQuestionRepository.findByLectureIdAndDeletedAtIsNull(eq(lectureId), any()))
                .willReturn(page);

        QnaAnswer answer = QnaAnswer.create(1L, 10L, "answer content");
        given(qnaAnswerRepository.findByQuestionIdIn(any()))
                .willReturn(List.of(answer));
        // when

        List<QnaQuestionResponse> result = qnaQuestionService.getQuestionsNAnswers(lectureId,PageRequest.of(0, 1)
        ).getContent();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).answer()).isNotNull();
        assertThat(result.get(0).answer().content()).isEqualTo("answer content");
    }
}
