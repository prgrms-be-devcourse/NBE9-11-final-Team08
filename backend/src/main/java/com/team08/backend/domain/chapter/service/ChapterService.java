package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.chapter.dto.LectureEnterResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
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

    /**
     * 챕터 생성
     */
    @Transactional
    public Long createChapter(Long courseId, ChapterCreateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        Chapter chapter = request.toEntity(course);
        course.addChapter(chapter);

        return chapterRepository.save(chapter).getId();
    }

    /**
     * 챕터 리스트 조회 — 코스 기준, 각 챕터에 속한 강의 목록 포함
     */
    @Transactional(readOnly = true)
    public List<ChapterWithLecturesResponse> getChaptersWithLectures(Long courseId) {
        List<Chapter> chapters = chapterRepository.findByCourseIdWithLecturesOrderByOrderNo(courseId);
        return chapters.stream()
                .map(ChapterWithLecturesResponse::from)
                .toList();
    }

    /**
     * 강좌 내 가장 최근 수강 강의 조회
     * 수강 이력이 없으면 null 반환
     */
    @Transactional(readOnly = true)
    public LectureEnterResponse getLastWatchedLecture(Long courseId, Long userId) {
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

    /**
     * 챕터 첫 강의 입장 — 해당 챕터의 orderNo 가장 낮은 강의 반환
     * 사용자의 학습 진행 정보도 함께 제공 (없으면 null)
     */
    @Transactional(readOnly = true)
    public LectureEnterResponse enterFirstLecture(Long chapterId, Long userId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));

        Lecture lecture = lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER));

        LectureProgress progress = lectureProgressRepository
                .findByUserIdAndLectureId(userId, lecture.getId())
                .orElse(null);

        return LectureEnterResponse.of(lecture, progress);
    }

}