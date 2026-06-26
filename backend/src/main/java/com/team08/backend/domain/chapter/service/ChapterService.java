package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lastwatchedlecture.service.LastWatchedLectureService;
import com.team08.backend.domain.lecture.access.LectureAccessValidator;
import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecture.service.LectureService;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LastWatchedLectureService lastWatchedLectureService;
    private final LectureService lectureService;
    private final LectureAccessValidator lectureAccessValidator;
    private final CourseAccessAuthorizer courseAccessAuthorizer;

    //챕터생성
    @Transactional
    public Long createChapter(Long courseId, Long userId, ChapterCreateRequest request) {
        // TODO: course access 검증 추가됨. 도훈님 확인 필요
        courseAccessAuthorizer.authorizeByCourseId(courseId, userId, CourseAction.MANAGE_COURSE);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        Chapter chapter = request.toEntity(course);
        course.addChapter(chapter);

        return chapterRepository.save(chapter).getId();
    }

    //챕터&강의 리스트 조회
    @Transactional(readOnly = true)
    public List<ChapterWithLecturesResponse> getChaptersWithLectures(Long courseId) {
        List<Chapter> chapters = chapterRepository.findByCourseIdWithLecturesOrderByOrderNo(courseId);
        return chapters.stream()
                .map(ChapterWithLecturesResponse::from)
                .toList();
    }

    //강좌 내 가장 최근 수강강의 조회
    @Transactional(readOnly = true)
    public LectureEnterResponse getLastWatchedLecture(Long courseId, Long userId) {
        lectureAccessValidator.validateCourseAccess(courseId, userId);

        return lastWatchedLectureService
                .findLectureId(userId, courseId)
                .map(lectureId -> buildResponse(userId, lectureId))
                .orElseGet(() -> getLastWatchedByProgress(courseId, userId));
    }

    // [마이그레이션 브리지] 정상 경로는 아님.
    // 정상 입장(enterLecture)은 last_watched_lectures 행을 항상 upsert 하므로 보통 이 폴백은 안 탄다.
    // 이 폴백이 실제로 값을 찾는 경우는 두 가지뿐:
    //   1) last_watched_lectures 도입(2026-06-21) 이전 데이터 — progress 행은 있으나 last_watched 행이 없음(백필 없이 무중단용)
    //   2) "입장 없이 하트비트" 비정상 경로 — LectureProgressService 가 progress 행만 lazy 생성
    // legacy 데이터가 소진되거나 백필을 돌리면 1) 사유는 사라지므로, 이후 제거 가능.
    private LectureEnterResponse getLastWatchedByProgress(Long courseId, Long userId) {
        List<Long> lectureIds = lectureRepository.findIdsByCourseId(courseId);
        if (lectureIds.isEmpty()) {
            return null;
        }
        return lectureProgressRepository
                .findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(userId, lectureIds)
                .map(progress -> {
                    Lecture lecture = lectureRepository.findById(progress.getLectureId())
                            .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER));
                    return LectureEnterResponse.of(lecture, progress);
                })
                .orElse(null);
    }

    // last_watched_lectures 에 행이 있을 때 강의 메타 + 진행도를 조립한다.
    private LectureEnterResponse buildResponse(Long userId, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER));
        LectureProgress progress = lectureProgressRepository
                .findByUserIdAndLectureId(userId, lectureId)
                .orElse(null);
        return LectureEnterResponse.of(lecture, progress);
    }

    @Transactional
    public LectureEnterResponse enterFirstLecture(Long courseId, Long chapterId, Long userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));

        Lecture lecture = lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER));

        return lectureService.enterLecture(courseId,chapterId,lecture.getId(), userId);
    }

}