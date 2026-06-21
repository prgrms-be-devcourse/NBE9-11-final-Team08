package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.service.EnrollmentAccessValidator;
import com.team08.backend.domain.lastwatchedlecture.service.LastWatchedLectureService;
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
    private final EnrollmentAccessValidator enrollmentAccessValidator;

    //챕터생성
    @Transactional
    public Long createChapter(Long courseId, ChapterCreateRequest request) {
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

    // TODO: (멘토님:강은혜) JPA 메소드가 병목 포인트,따로 벌크 처리같은게 필요함. 트랜잭션하나에 여러 쿼리가 함께쓰임-> 어떤 트랜잭션 전략으로 갈지 고민
    //  반영 후 : 백필로 repository 순회 및 정렬 중단

    //강좌 내 가장 최근 수강강의 조회
    @Transactional(readOnly = true)
    public LectureEnterResponse getLastWatchedLecture(Long courseId, Long userId) {
        enrollmentAccessValidator.validateActiveEnrollment(userId, courseId);

        // 1) 강좌별 마지막 시청 강의를 사전 계산해 둔 조회용 테이블(last_watched_lectures)에서 단건으로 가져온다.
        //    강의 입장 시 갱신되므로, 한 번이라도 강좌를 시청한 적이 있으면 여기서 끝난다(강좌 강의 수와 무관).
        return lastWatchedLectureService
                .findLectureId(userId, courseId)
                .map(lectureId -> buildResponse(userId, lectureId))
                // 2) 조회용 테이블에 아직 행이 없는 과거 데이터는 진행도 집계로 폴백한다.
                .orElseGet(() -> getLastWatchedByProgress(courseId, userId));
    }


    // TODO: (멘토님:강은혜) JPA 메소드가 병목 포인트,따로 벌크 처리같은게 필요함. 트랜잭션하나에 여러 쿼리가 함께쓰임-> 어떤 트랜잭션 전략으로 갈지 고민
    //  반영 전 : 일일이 테이블 순회 및 정렬

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

        // URL 정합성: 해당 챕터가 실제로 path 의 강좌 소속인지 확인한다. (수강권 검사는 enterLecture 가 담당)
        if (!chapter.getCourse().getId().equals(courseId)) {
            throw new CustomException(ErrorCode.CHAPTER_NOT_FOUND);
        }

        Lecture lecture = lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER));

        return lectureService.enterLecture(lecture.getId(), userId);
    }

}