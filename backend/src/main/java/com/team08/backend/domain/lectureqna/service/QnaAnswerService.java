package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
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
    private final CourseRepository courseRepository;

    @Transactional
    public QnaAnswerResponse createAnswer(Long questionId, Long courseId, Long instructorId, String role, String content) {
        //답변자검사 1차: role_seller 검증
        validateInstructor(role);

        //질문 존재 검증
        qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_QUESTION_NOT_FOUND));

        //답변자검사 2차: 강좌 존재 및 요청자-강좌강사 대조
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.getInstructorId().equals(instructorId)) {
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

        //요청자와 기존 답변자(강사)와 대조
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

    //강사권한 확인
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
