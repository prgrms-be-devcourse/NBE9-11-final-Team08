package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
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

    @Mock private QnaAnswerRepository qnaAnswerRepository;
    @Mock private QnaQuestionRepository qnaQuestionRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private QnaAnswerService qnaAnswerService;

    private QnaAnswer answer;

    @BeforeEach
    void setUp() {
        answer = QnaFixture.answer();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private void givenQuestion(Long questionId) {
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId))
                .willReturn(Optional.of(mock(QnaQuestion.class)));
    }

    private void givenCourseOwnedBy(Long courseId, Long instructorId) {
        Course course = mock(Course.class);
        given(course.getInstructorId()).willReturn(instructorId);
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
    }

    // ── 답변 작성 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("답변 작성 성공")
    void createAnswer_success() {
        givenQuestion(question_id);
        givenCourseOwnedBy(course_id, instructor_id);
        given(qnaAnswerRepository.existsByQuestionId(question_id)).willReturn(false);
        given(qnaAnswerRepository.save(any())).willReturn(answer);

        QnaAnswerResponse response = qnaAnswerService.createAnswer(question_id, course_id, instructor_id, "답변 내용");

        assertThat(response.content()).isEqualTo("답변 내용");
        assertThat(response.instructorId()).isEqualTo(instructor_id);
    }

    @Test
    @DisplayName("답변 작성 실패 - 질문 없음")
    void createAnswer_questionNotFound() {
        given(qnaQuestionRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, course_id, instructor_id, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("답변 작성 실패 - 코스 없음")
    void createAnswer_courseNotFound() {
        givenQuestion(question_id);
        given(courseRepository.findById(course_id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, course_id, instructor_id, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("답변 작성 실패 - 해당 코스 강사 아님")
    void createAnswer_notCourseInstructor() {
        givenQuestion(question_id);
        givenCourseOwnedBy(course_id, instructor_id);

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, course_id, 99L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    @Test
    @DisplayName("답변 작성 실패 - 이미 답변 존재")
    void createAnswer_alreadyExists() {
        givenQuestion(question_id);
        givenCourseOwnedBy(course_id, instructor_id);
        given(qnaAnswerRepository.existsByQuestionId(question_id)).willReturn(true);

        assertThatThrownBy(() -> qnaAnswerService.createAnswer(question_id, course_id, instructor_id, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ANSWER_ALREADY_EXISTS);
    }

    // ── 답변 수정 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("답변 수정 성공")
    void updateAnswer_success() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        QnaAnswerResponse response = qnaAnswerService.updateAnswer(question_id, instructor_id, "new content");

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("new content");
    }

    @Test
    @DisplayName("답변 수정 실패 - 답변 없음")
    void updateAnswer_notFound() {
        given(qnaAnswerRepository.findByQuestionId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> qnaAnswerService.updateAnswer(question_id, instructor_id, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ANSWER_NOT_FOUND);
    }

    @Test
    @DisplayName("답변 수정 실패 - 작성 강사 아님")
    void updateAnswer_accessDenied() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> qnaAnswerService.updateAnswer(question_id, 99L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }

    // ── 답변 삭제 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("답변 삭제 성공")
    void deleteAnswer_success() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        assertThatCode(() -> qnaAnswerService.deleteAnswer(question_id, instructor_id))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("답변 삭제 실패 - 작성 강사 아님")
    void deleteAnswer_accessDenied() {
        given(qnaAnswerRepository.findByQuestionId(question_id)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> qnaAnswerService.deleteAnswer(question_id, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QNA_ACCESS_DENIED);
    }
}
