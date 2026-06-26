package com.team08.backend.domain.lecture.access;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 강의 입장 접근 검증 전용 모듈.
 *
 * <p>StudyAccessAuthorizer 는 "스터디 접근 권한"만 본다. 강의 입장에는 그 외에
 * <b>URL 계층 정합성</b>(lecture 가 path 의 chapter·course 소속인가)이 더 필요하므로,
 * 이를 여기서 관리하고 실제 권한 판단만 StudyAccessAuthorizer 에 위임한다.
 *
 * <ul>
 *   <li>404 — URL 정합성: lecture→chapter→course 가 path 값과 일치하는지</li>
 *   <li>그 외에는 StudyAccessAuthorizer 로 위임(403). 무료 맛보기 예외 없이 모든 강의가 권한 검사를 거친다.</li>
 * </ul>
 *
 * 검증을 통과한 Lecture(chapter·course 조인페치 완료)를 반환해 호출부의 재조회를 막는다.
 */
@Component
@RequiredArgsConstructor
public class LectureAccessValidator {

    private final LectureRepository lectureRepository;
    private final CourseAccessAuthorizer courseAccessAuthorizer;

    public Lecture validateForEnter(Long courseId, Long chapterId, Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        validateHierarchy(lecture, courseId, chapterId);

        // 무료 맛보기 예외 없이 모든 강의가 스터디 접근 권한 검사를 거친다.
        // (courseId 가 lecture 소속임을 위에서 검증했으므로 lecture 재조회 없는 authorizeByCourseId 사용)
        courseAccessAuthorizer.authorizeByCourseId(courseId, userId, CourseAction.VIEW_CONTENT);

        return lecture;
    }

    /**
     * 강좌 단위 접근 검증 — 특정 강의가 아니라 강좌 범위 조회(예: 마지막 시청 강의)에 쓴다.
     * 계층 정합성·무료 맛보기는 강좌 단위에선 의미가 없으므로 적용하지 않고, 권한 판단만 위임한다.
     */
    public void validateCourseAccess(Long courseId, Long userId) {
        courseAccessAuthorizer.authorizeByCourseId(courseId, userId, CourseAction.VIEW_CONTENT);
    }

    // URL 정합성: findByIdWithChapterAndCourse 가 chapter/course 를 이미 join fetch 하므로 추가 쿼리 없이 ID 비교만 한다.
    private void validateHierarchy(Lecture lecture, Long courseId, Long chapterId) {
        Chapter chapter = lecture.getChapter();
        if (!chapter.getId().equals(chapterId)) {
            throw new CustomException(ErrorCode.CHAPTER_NOT_FOUND);
        }
        if (!chapter.getCourse().getId().equals(courseId)) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }
    }
}
