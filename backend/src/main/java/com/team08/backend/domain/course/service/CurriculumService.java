package com.team08.backend.domain.course.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.dto.ChapterReorderRequest;
import com.team08.backend.domain.course.dto.LectureReorderRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public void reorderChapters(Long courseId, Long instructorId, ChapterReorderRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        course.validateOwner(instructorId);

        List<Long> chapterIds = request.reorders().stream()
                .map(ChapterReorderRequest.ChapterOrderElement::chapterId)
                .toList();

        List<Chapter> existingChapters = chapterRepository.findAllById(chapterIds);

        for (Chapter chapter : existingChapters) {
            if (!chapter.getCourse().getId().equals(courseId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_COURSE_OWNER);
            }
        }

        Map<Long, Chapter> chapterMap = existingChapters.stream()
                .collect(Collectors.toMap(Chapter::getId, c -> c));

        for (ChapterReorderRequest.ChapterOrderElement element : request.reorders()) {
            Chapter chapter = chapterMap.get(element.chapterId());
            if (chapter != null) {
                chapter.updateGeneralInfo(chapter.getTitle(), element.orderNo(), new ArrayList<>());
            }
        }
    }

    @Transactional
    public void reorderLectures(Long chapterId, Long instructorId, LectureReorderRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        chapter.getCourse().validateOwner(instructorId);

        List<Long> lectureIds = request.reorders().stream()
                .map(LectureReorderRequest.LectureOrderElement::lectureId)
                .toList();

        List<Lecture> existingLectures = lectureRepository.findAllById(lectureIds);

        for (Lecture lecture : existingLectures) {
            if (!lecture.getChapter().getId().equals(chapterId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_COURSE_OWNER);
            }
        }

        Map<Long, Lecture> lectureMap = existingLectures.stream()
                .collect(Collectors.toMap(Lecture::getId, l -> l));

        for (LectureReorderRequest.LectureOrderElement element : request.reorders()) {
            Lecture lecture = lectureMap.get(element.lectureId());
            if (lecture != null) {
                lecture.updateGeneralInfo(
                        lecture.getTitle(),
                        lecture.getSummary(),
                        lecture.getDurationSeconds(),
                        element.orderNo(),
                        lecture.isFreePreview()
                );
            }
        }
    }
}