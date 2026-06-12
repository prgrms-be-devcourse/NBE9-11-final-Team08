package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QnaAnswerService {

    private static final String INSTRUCTOR_ROLE = "ROLE_SELLER";

    private final QnaAnswerRepository qnaAnswerRepository;
    private final QnaQuestionRepository qnaQuestionRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public QnaAnswerResponse createAnswer(Long questionId, Long instructorId, String role, String content) {
        validateInstructor(role);

        QnaQuestion question = qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_QUESTION_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(question.getLectureId())
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
        if (!lecture.getChapter().getCourse().getInstructorId().equals(instructorId)) {
            throw new CustomException(ErrorCode.QNA_ACCESS_DENIED);
        }

        if (qnaAnswerRepository.existsByQuestionId(questionId)) {
            throw new CustomException(ErrorCode.QNA_ANSWER_ALREADY_EXISTS);
        }

        QnaAnswer answer = QnaAnswer.create(questionId, instructorId, content);
        return toResponse(qnaAnswerRepository.save(answer));
    }

    @Transactional
    public QnaAnswerResponse updateAnswer(Long questionId, Long instructorId, String role, String content) {
        validateInstructor(role);

        QnaAnswer answer = qnaAnswerRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_ANSWER_NOT_FOUND));

        if (!answer.getInstructorId().equals(instructorId)) {
            throw new CustomException(ErrorCode.QNA_ACCESS_DENIED);
        }

        answer.update(content);
        return toResponse(answer);
    }

    @Transactional
    public void deleteAnswer(Long questionId, Long instructorId, String role) {
        validateInstructor(role);

        QnaAnswer answer = qnaAnswerRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_ANSWER_NOT_FOUND));

        if (!answer.getInstructorId().equals(instructorId)) {
            throw new CustomException(ErrorCode.QNA_ACCESS_DENIED);
        }

        qnaAnswerRepository.delete(answer);
    }

    private void validateInstructor(String role) {
        if (!INSTRUCTOR_ROLE.equals(role)) {
            throw new CustomException(ErrorCode.INSTRUCTOR_ONLY);
        }
    }

    private QnaAnswerResponse toResponse(QnaAnswer a) {
        return new QnaAnswerResponse(
                a.getId(), a.getQuestionId(), a.getInstructorId(),
                a.getContent(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
