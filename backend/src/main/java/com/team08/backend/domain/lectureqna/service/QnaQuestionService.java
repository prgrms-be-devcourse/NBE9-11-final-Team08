package com.team08.backend.domain.lectureqna.service;

import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.lectureqna.dto.MyCommentResponse;
import com.team08.backend.domain.lectureqna.dto.MyQnaRow;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerSummary;
import com.team08.backend.domain.lectureqna.dto.QnaQuestionResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QnaQuestionService {

    private final QnaQuestionRepository qnaQuestionRepository;
    private final QnaAnswerRepository qnaAnswerRepository;
    private final CourseAccessAuthorizer courseAccessAuthorizer;

    @Transactional
    public QnaQuestionResponse createQuestion(Long lectureId, Long userId, String title, String content) {
        // 해당 강의가 속한 스터디의 활성 학습자만 질문을 작성할 수 있다.
        // authorizer 의 resolver 가 lecture 존재 검증도 함께 수행한다(없으면 LECTURE_NOT_FOUND).
        courseAccessAuthorizer.authorizeByLectureId(lectureId, userId, CourseAction.WRITE_CONTENT);

        QnaQuestion question = QnaQuestion.create(userId, lectureId, title, content);
        QnaQuestion saved = qnaQuestionRepository.save(question);
        return toResponse(saved, null);
    }

    @Transactional
    public QnaQuestionResponse updateQuestion(Long questionId, Long userId, String title, String content) {
        QnaQuestion question = qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_QUESTION_NOT_FOUND));

        if (!question.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.QNA_ACCESS_DENIED);
        }

        if (qnaAnswerRepository.existsByQuestionId(questionId)) {
            throw new CustomException(ErrorCode.QNA_ALREADY_ANSWERED);
        }

        question.update(title, content);
        QnaQuestion saved = qnaQuestionRepository.save(question);

        return toResponse(saved, null);
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        QnaQuestion question = qnaQuestionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_QUESTION_NOT_FOUND));

        if (!question.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.QNA_ACCESS_DENIED);
        }

        if (qnaAnswerRepository.existsByQuestionId(questionId)) {
            throw new CustomException(ErrorCode.QNA_ALREADY_ANSWERED);
        }

        question.delete();
    }

    @Transactional(readOnly = true)
    public Page<QnaQuestionResponse> getQuestionsNAnswers(Long lectureId, Long userId, Pageable pageable) {
        courseAccessAuthorizer.authorizeByLectureId(lectureId, userId, CourseAction.WRITE_CONTENT);

        Page<QnaQuestion> questions = qnaQuestionRepository.findByLectureIdAndDeletedAtIsNull(lectureId, pageable);

        List<Long> questionIds = questions.stream().map(QnaQuestion::getId).toList();
        Map<Long, QnaAnswerSummary> answerMap = qnaAnswerRepository.findByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(
                        QnaAnswer::getQuestionId,
                        a -> new QnaAnswerSummary(a.getId(), a.getContent(), a.getCreatedAt())
                ));

        return questions.map(q -> toResponse(q, answerMap.get(q.getId())));
    }

    /**
     * 마이페이지 "작성한 댓글": 내가 작성한 QnA 질문을 전 강의에 걸쳐 최신순으로 모아준다.
     * 각 질문의 답변 여부(answered)를 한 번의 배치 조회로 채워 N+1 을 피한다.
     */
    @Transactional(readOnly = true)
    public List<MyCommentResponse> getMyComments(Long userId) {
        List<MyQnaRow> rows = qnaQuestionRepository.findMyComments(userId);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = rows.stream().map(MyQnaRow::id).toList();
        Set<Long> answeredIds = qnaAnswerRepository.findByQuestionIdIn(questionIds)
                .stream()
                .map(QnaAnswer::getQuestionId)
                .collect(Collectors.toSet());

        return rows.stream()
                .map(r -> new MyCommentResponse(
                        r.id(), r.lectureId(), r.courseTitle(), r.lectureTitle(),
                        r.title(), r.content(), r.createdAt(),
                        answeredIds.contains(r.id())))
                .toList();
    }

    private QnaQuestionResponse toResponse(QnaQuestion q, QnaAnswerSummary answer) {
        return new QnaQuestionResponse(
                q.getId(), q.getLectureId(), q.getUserId(),
                q.getTitle(), q.getContent(),
                q.getCreatedAt(), q.getUpdatedAt(),
                answer);
    }
}
