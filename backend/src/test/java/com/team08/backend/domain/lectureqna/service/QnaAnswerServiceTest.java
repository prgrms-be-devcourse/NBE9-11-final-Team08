package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.fixture.QnaFixture;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.team08.backend.domain.lectureqna.fixture.QnaFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class QnaAnswerServiceTest {

    private static final String INSTRUCTOR_ROLE = "ROLE_SELLER";
    private static final String USER_ROLE = "ROLE_USER";

    @Mock private QnaAnswerRepository qnaAnswerRepository;
    @Mock private QnaQuestionRepository qnaQuestionRepository;
    @Mock private LectureRepository lectureRepository;

    @InjectMocks
    private QnaAnswerService qnaAnswerService;

    private QnaAnswer answer;

    @BeforeEach
    void setUp() {
        answer = QnaFixture.answer();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private QnaQuestion givenQuestion(Long questionId, Long lectureId) {
        QnaQuestion question = mock(QnaQuestion.class);
        given(question.getLectureId()).willReturn(lectureId);
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId))
                .willReturn(Optional.of(question));
        return question;
    }

    private void givenLectureOwnedBy(Long lectureId, Long instructorId) {
        Lecture lecture = mockLectureOwnedBy(instructorId);
        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
    }

    // ── 답변 작성 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("답변 작성 성공")
    void createAnswer_success() {
        givenQuestion(question_id, lecture_id);
        givenLectureOwnedBy(lecture_id, instructor_id);
        given(qnaAnswerRepository.existsByQuestionId(question_id)).willReturn(false);
        given(qnaAnswerRepository.save(any())).willReturn(answer);

        QnaAnswerResponse response = qnaAnswerService.createAnswer(question_id, instructor_id, INSTRUCTOR_ROLE, "답변 내용");

        assertThat(response.content()).isEqualTo("답변 내용");
        assertThat(response.instructorId()).isEqualTo(instructor_id);
    }

    @Test
    @DisplayName("답변 작성 실패 - 강사 아님")
    void createAnswer_notInstructor() {
        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, instructor_id, USER_ROLE, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTRUCTOR_ONLY);
    }

    @Test
    @DisplayName("답변 작성 실패 - 질문 없음")
    void createAnswer_questionNotFound() {
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, instructor_id, INSTRUCTOR_ROLE, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("답변 작성 실패 - 해당 코스 강사 아님")
    void createAnswer_notCourseInstructor() {
        givenQuestion(question_id, lecture_id);
        givenLectureOwnedBy(lecture_id, instructor_id);

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, 99L, INSTRUCTOR_ROLE, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    @Test
    @DisplayName("답변 작성 실패 - 이미 답변 존재")
    void createAnswer_alreadyExists() {
        givenQuestion(question_id, lecture_id);
        givenLectureOwnedBy(lecture_id, instructor_id);
        given(qnaAnswerRepository.existsByQuestionId(question_id)).willReturn(true);

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, instructor_id, INSTRUCTOR_ROLE, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ANSWER_ALREADY_EXISTS);
    }

    // ── 답변 수정 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("답변 수정 성공")
    void updateAnswer_success() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        QnaAnswerResponse response = qnaAnswerService.updateAnswer(question_id, instructor_id, INSTRUCTOR_ROLE, "new content");

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("new content");
    }

    @Test
    @DisplayName("답변 수정 실패 - 강사 아님")
    void updateAnswer_notInstructor() {
        assertThatThrownBy(() -> qnaAnswerService.updateAnswer(question_id, instructor_id, USER_ROLE, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTRUCTOR_ONLY);
    }

    @Test
    @DisplayName("답변 수정 실패 - 답변 없음")
    void updateAnswer_notFound() {
        given(qnaAnswerRepository.findByQuestionId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaAnswerService.updateAnswer(question_id, instructor_id, INSTRUCTOR_ROLE, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ANSWER_NOT_FOUND);
    }

    @Test
    @DisplayName("답변 수정 실패 - 작성 강사 아님")
    void updateAnswer_accessDenied() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> qnaAnswerService.updateAnswer(question_id, 99L, INSTRUCTOR_ROLE, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    // ── 답변 삭제 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("답변 삭제 성공")
    void deleteAnswer_success() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        assertThatCode(() -> qnaAnswerService.deleteAnswer(question_id, instructor_id, INSTRUCTOR_ROLE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("답변 삭제 실패 - 강사 아님")
    void deleteAnswer_notInstructor() {
        assertThatThrownBy(() -> qnaAnswerService.deleteAnswer(question_id, instructor_id, USER_ROLE))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTRUCTOR_ONLY);
    }

    @Test
    @DisplayName("답변 삭제 실패 - 작성 강사 아님")
    void deleteAnswer_accessDenied() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> qnaAnswerService.deleteAnswer(question_id, 99L, INSTRUCTOR_ROLE))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }
}
