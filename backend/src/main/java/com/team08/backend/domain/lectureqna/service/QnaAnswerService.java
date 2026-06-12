package com.team08.backend.domain.lectureqna.service;

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

    @Transactional
    public QnaAnswerResponse createAnswer(Long questionId, Long instructorId, String content) {
        qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_QUESTION_NOT_FOUND));

        if (qnaAnswerRepository.existsByQuestionId(questionId)) {
            throw new CustomException(ErrorCode.QNA_ANSWER_ALREADY_EXISTS);
        }

        QnaAnswer answer = QnaAnswer.create(questionId, instructorId, content);
        return toResponse(qnaAnswerRepository.save(answer));
    }

    @Transactional
    public QnaAnswerResponse updateAnswer(Long questionId, Long instructorId, String content) {
        QnaAnswer answer = qnaAnswerRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_ANSWER_NOT_FOUND));

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
