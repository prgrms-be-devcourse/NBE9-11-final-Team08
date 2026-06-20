package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.enrollment.service.EnrollmentAccessValidator;
import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.service.LectureProgressService;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureService {

    private final LectureRepository lectureRepository;
    private final ChapterRepository chapterRepository;
    private final LectureProgressService lectureProgressService;
    private final EnrollmentAccessValidator enrollmentAccessValidator;
    /**
     * 특정 강의 러닝 스페이스 입장 — 강의 메타데이터 + 학습 진행 정보를 반환한다.
     * 입장 시 진행 행이 없으면 생성한다(수강권 보유·무료 강의에 한해). 이는 이후 하트비트가
     * 벽시계 경계를 적용할 서버 기준점(updatedAt)을 입장 시점에 확보하기 위함이다.
     */
    @Transactional
    public LectureEnterResponse enterLecture(Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        //수강권 검사
        enrollmentAccessValidator.validateActiveEnrollment(userId, lecture.getChapter().getCourse().getId());

        LectureProgress progress = lectureProgressService.ensureStarted(userId, lecture, LocalDateTime.now());
        return LectureEnterResponse.of(lecture, progress);
    }

    @Transactional
    public Long createLecture(Long courseId, Long chapterId, LectureCreateRequest request) {
        Chapter chapter = chapterRepository.findByIdWithCourse(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));

        if (!chapter.getCourse().getId().equals(courseId)) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }

        Lecture lecture = request.toEntity(chapter);
        chapter.addLecture(lecture);

        return lectureRepository.save(lecture).getId();
    }
}