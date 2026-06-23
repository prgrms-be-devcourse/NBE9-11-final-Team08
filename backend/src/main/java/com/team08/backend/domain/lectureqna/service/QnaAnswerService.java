package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
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
    private final StudyAccessAuthorizer studyAccessAuthorizer;

    @Transactional
    public QnaAnswerResponse createAnswer(Long questionId, Long requesterId, String content) {

        //질문 존재 검사
        QnaQuestion question = qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_QUESTION_NOT_FOUND));

        // TODO: validateHierarchy 와 authorizeByLectureId 의 성능을 비교해보고 싶어 따로 courseId나 ChapterId를 받지 않고...byLectureID 남겨두었습니다.
        //  enterlecture와 다르게 authorizeByLectureId 를 활용했습니다
        studyAccessAuthorizer.authorizeByLectureId(question.getLectureId(), requesterId, StudyAction.MANAGE_ANSWER);

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
