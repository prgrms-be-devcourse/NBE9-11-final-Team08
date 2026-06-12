package com.team08.backend.domain.lectureqna.fixture;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * QnaQuestion / QnaAnswer 테스트용 픽스쳐.
 * <p>
 * 두 엔티티가 같은 lectureqna 도메인에 속하므로 한 클래스로 관리한다.
 * Lecture → Chapter → Course 강사 체인 mock도 함께 제공한다.
 */
public final class QnaFixture {

    // ── 기본 ID 상수 ──────────────────────────────────────────────────────

    public static final Long question_id = 1L;
    public static final Long lecture_id = 5L;
    public static final Long instructor_id = 10L;
    public static final Long user_id = 1L;
    public static final Long other_user_id = 2L;

    private QnaFixture() {}

    // ── QnaQuestion 팩토리 ────────────────────────────────────────────────

    /** 기본값으로 QnaQuestion 생성 (id 없음) */
    public static QnaQuestion question() {
        return QnaQuestion.create(user_id, lecture_id, "제목", "내용");
    }

    /** userId, lectureId 를 지정해 QnaQuestion 생성 */
    public static QnaQuestion question(Long userId, Long lectureId) {
        return QnaQuestion.create(userId, lectureId, "제목", "내용");
    }

    /** id 포함 QnaQuestion 생성 */
    public static QnaQuestion question(Long id, Long userId, Long lectureId) {
        QnaQuestion q = QnaQuestion.create(userId, lectureId, "제목", "내용");
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }

    // ── QnaAnswer 팩토리 ──────────────────────────────────────────────────

    /** 기본값으로 QnaAnswer 생성 (id 없음) */
    public static QnaAnswer answer() {
        return QnaAnswer.create(question_id, instructor_id, "답변 내용");
    }

    /** questionId, instructorId, content 를 지정해 QnaAnswer 생성 */
    public static QnaAnswer answer(Long questionId, Long instructorId, String content) {
        return QnaAnswer.create(questionId, instructorId, content);
    }

    /** id 포함 QnaAnswer 생성 */
    public static QnaAnswer answer(Long id, Long questionId, Long instructorId, String content) {
        QnaAnswer a = QnaAnswer.create(questionId, instructorId, content);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    // ── Lecture 강사 체인 mock ─────────────────────────────────────────────

    /**
     * {@code instructorId} 를 소유자로 갖는 Lecture mock 을 반환한다.
     * <pre>
     *   Lecture → Chapter → Course (instructorId = ?)
     * </pre>
     * 반환된 mock 을 {@code LectureRepository.findById()} stub 에 연결해 사용한다.
     */
    public static Lecture mockLectureOwnedBy(Long instructorId) {
        Course course = mock(Course.class);
        given(course.getInstructorId()).willReturn(instructorId);

        Chapter chapter = mock(Chapter.class);
        given(chapter.getCourse()).willReturn(course);

        Lecture lecture = mock(Lecture.class);
        given(lecture.getChapter()).willReturn(chapter);

        return lecture;
    }
}
