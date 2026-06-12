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

    private final QnaAnswerRepository qnaAnswerRepository;
    private final QnaQuestionRepository qnaQuestionRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public QnaAnswerResponse createAnswer(Long questionId, Long courseId, Long requesterId, String content) {

        //질문 존재 검사
        qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_QUESTION_NOT_FOUND));

        //강의 존재 검사
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        //TODO: 답변자가 강사이고 다른 강의에 답변을 남겨버리는 상황 추가 점검
        // course<-chapter<-lecture<-Question<-Answer
        // answer에서 course.instructor를 참조하려면 쿼리를 여러번 타야하는 상황 개선

        //요청자=강사 인지 검사
        if (!course.getInstructorId().equals(requesterId)) {
            throw new CustomException(ErrorCode.QNA_ACCESS_DENIED);
        }

        //답변 이미 있는지 검사
        if (qnaAnswerRepository.existsByQuestionId(questionId)) {
            throw new CustomException(ErrorCode.QNA_ANSWER_ALREADY_EXISTS);
        }

        QnaAnswer answer = QnaAnswer.create(questionId, requesterId, content);
        return toResponse(qnaAnswerRepository.save(answer));
    }

    @Transactional
    public QnaAnswerResponse updateAnswer(Long questionId, Long instructorId, String content) {

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
    public void deleteAnswer(Long questionId, Long instructorId) {
        QnaAnswer answer = qnaAnswerRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_ANSWER_NOT_FOUND));

        if (!answer.getInstructorId().equals(instructorId)) {
            throw new CustomException(ErrorCode.QNA_ACCESS_DENIED);
        }

        qnaAnswerRepository.delete(answer);
    }


    private QnaAnswerResponse toResponse(QnaAnswer a) {
        return new QnaAnswerResponse(
                a.getId(), a.getQuestionId(), a.getInstructorId(),
                a.getContent(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
