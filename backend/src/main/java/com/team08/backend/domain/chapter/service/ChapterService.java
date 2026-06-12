package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.chapter.dto.LectureEnterResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
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
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;

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

    /**
     * 최근 수강 강의 입장 — 챕터 내에서 사용자가 가장 최근 학습한 강의 반환
     */
    @Transactional(readOnly = true)
    public LectureEnterResponse enterRecentLecture(Long chapterId, Long userId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));

        List<Long> lectureIds = lectureRepository
                .findByChapterIdOrderByOrderNoAsc(chapterId)
                .stream()
                .map(Lecture::getId)
                .toList();

        if (lectureIds.isEmpty()) {
            throw new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER);
        }

        LectureProgress recentProgress = lectureProgressRepository
                .findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(userId, lectureIds)
                .orElseThrow(() -> new CustomException(ErrorCode.RECENT_LECTURE_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(recentProgress.getLectureId())
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        return LectureEnterResponse.of(lecture, recentProgress);
    }
}
