package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.lectureqna.dto.QnaQuestionResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.fixture.QnaFixture;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.team08.backend.domain.lectureqna.fixture.QnaFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QnaQuestionServiceTest {

    @Mock private QnaQuestionRepository qnaQuestionRepository;
    @Mock private QnaAnswerRepository qnaAnswerRepository;
    @Mock private StudyAccessAuthorizer studyAccessAuthorizer;

    @InjectMocks
    private QnaQuestionService qnaQuestionService;

    private QnaQuestion question;

    @BeforeEach
    void setUp() {
        question = QnaFixture.question();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private void givenQuestionExists(Long questionId, QnaQuestion q) {
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId))
                .willReturn(Optional.of(q));
    }

    // ── 질문 작성 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("질문 작성 성공 - 활성 스터디 학습자")
    void createQuestion_success() {
        given(qnaQuestionRepository.save(any())).willReturn(question);

        QnaQuestionResponse response = qnaQuestionService.createQuestion(lecture_id, user_id, "제목", "내용");

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.answer()).isNull();
        verify(studyAccessAuthorizer).authorizeByLectureId(lecture_id, user_id, StudyAction.WRITE_STUDY_CONTENT);
    }

    @Test
    @DisplayName("질문 작성 실패 - 강의 없음")
    void createQuestion_lectureNotFound() {
        willThrow(new CustomException(ErrorCode.LECTURE_NOT_FOUND))
                .given(studyAccessAuthorizer)
                .authorizeByLectureId(lecture_id, user_id, StudyAction.WRITE_STUDY_CONTENT);

        assertThatThrownBy(() -> qnaQuestionService.createQuestion(lecture_id, user_id, "제목", "내용"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND);
    }

    @Test
    @DisplayName("질문 작성 실패 - 스터디 학습자 아님")
    void createQuestion_notStudyLearner() {
        willThrow(new CustomException(ErrorCode.STUDY_ACCESS_DENIED))
                .given(studyAccessAuthorizer)
                .authorizeByLectureId(lecture_id, user_id, StudyAction.WRITE_STUDY_CONTENT);

        assertThatThrownBy(() -> qnaQuestionService.createQuestion(lecture_id, user_id, "제목", "내용"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);
    }

    // ── 질문 수정 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("질문 수정 성공")
    void updateQuestion_success() {
        givenQuestionExists(question_id, question);
        given(qnaQuestionRepository.save(any())).willReturn(question);

        QnaQuestionResponse response = qnaQuestionService.updateQuestion(question_id, user_id, "new title", "new content");

        assertThat(response).isNotNull();
        assertThat(response.answer()).isNull();
    }

    @Test
    @DisplayName("질문 수정 실패 - 질문 없음")
    void updateQuestion_notFound() {
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaQuestionService.updateQuestion(question_id, user_id, "title", "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("질문 수정 실패 - 작성자 아님")
    void updateQuestion_accessDenied() {
        givenQuestionExists(question_id, question);

        assertThatThrownBy(() -> qnaQuestionService.updateQuestion(question_id, other_user_id, "title", "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    @Test
    @DisplayName("질문 수정 실패 - 이미 답변 존재")
    void updateQuestion_alreadyAnswered() {
        givenQuestionExists(question_id, question);
        given(qnaAnswerRepository.existsByQuestionId(question_id)).willReturn(true);

        assertThatThrownBy(() -> qnaQuestionService.updateQuestion(question_id, user_id, "new title", "new content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ALREADY_ANSWERED);
    }

    // ── 질문 삭제 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("질문 삭제 성공")
    void deleteQuestion_success() {
        givenQuestionExists(question_id, question);

        assertThatCode(() -> qnaQuestionService.deleteQuestion(question_id, user_id))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("질문 삭제 실패 - 작성자 아님")
    void deleteQuestion_accessDenied() {
        givenQuestionExists(question_id, question);

        assertThatThrownBy(() -> qnaQuestionService.deleteQuestion(question_id, other_user_id))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    @Test
    @DisplayName("질문 삭제 실패 - 이미 답변 존재")
    void deleteQuestion_alreadyAnswered() {
        givenQuestionExists(question_id, question);
        given(qnaAnswerRepository.existsByQuestionId(question_id)).willReturn(true);

        assertThatThrownBy(() -> qnaQuestionService.deleteQuestion(question_id, user_id))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ALREADY_ANSWERED);
    }

    @Test
    @DisplayName("질문 목록 조회 - 답변 포함")
    void getQuestions_N_Answers_withAnswer() {
        QnaQuestion q = mock(QnaQuestion.class);
        given(q.getId()).willReturn(question_id);
        given(q.getLectureId()).willReturn(lecture_id);
        given(q.getUserId()).willReturn(user_id);
        given(q.getTitle()).willReturn("제목");
        given(q.getContent()).willReturn("내용");
        given(q.getCreatedAt()).willReturn(LocalDateTime.now());
        given(q.getUpdatedAt()).willReturn(LocalDateTime.now());

        Page<QnaQuestion> page = new PageImpl<>(List.of(q));
        given(qnaQuestionRepository.findByLectureIdAndDeletedAtIsNull(eq(lecture_id), any()))
                .willReturn(page);

        QnaAnswer answer = QnaFixture.answer(question_id, instructor_id, "answer content");
        given(qnaAnswerRepository.findByQuestionIdIn(any())).willReturn(List.of(answer));

        List<QnaQuestionResponse> result = qnaQuestionService
                .getQuestionsNAnswers(lecture_id, PageRequest.of(0, 1))
                .getContent();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).answer()).isNotNull();
        assertThat(result.get(0).answer().content()).isEqualTo("answer content");
    }
}
