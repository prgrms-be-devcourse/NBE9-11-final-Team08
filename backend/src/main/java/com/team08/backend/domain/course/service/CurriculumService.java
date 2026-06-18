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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final CurriculumValidator curriculumValidator;

    @Transactional
    public void reorderChapters(Long courseId, Long instructorId, ChapterReorderRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        course.validateOwner(instructorId);

        List<Chapter> dbChapters = chapterRepository.findByCourseIdOrderByOrderNo(courseId);
        curriculumValidator.validateSize(dbChapters.size(), request.reorders().size());

        Set<Long> dbIds = dbChapters.stream().map(Chapter::getId).collect(Collectors.toSet());
        Set<Long> requestIds = request.reorders().stream()
                .map(ChapterReorderRequest.ChapterOrderElement::chapterId)
                .collect(Collectors.toSet());
        curriculumValidator.validateIds(dbIds, requestIds);

        List<Integer> sortedOrders = request.reorders().stream()
                .map(ChapterReorderRequest.ChapterOrderElement::orderNo)
                .sorted()
                .toList();
        curriculumValidator.validateOrderSequence(sortedOrders);

        for (ChapterReorderRequest.ChapterOrderElement element : request.reorders()) {
            chapterRepository.updateOrderNo(element.chapterId(), element.orderNo(), courseId);
        }
    }

    @Transactional
    public void reorderLectures(Long chapterId, Long instructorId, LectureReorderRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
        chapter.getCourse().validateOwner(instructorId);

        List<Lecture> dbLectures = lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId);
        curriculumValidator.validateSize(dbLectures.size(), request.reorders().size());

        Set<Long> dbIds = dbLectures.stream().map(Lecture::getId).collect(Collectors.toSet());
        Set<Long> requestIds = request.reorders().stream()
                .map(LectureReorderRequest.LectureOrderElement::lectureId)
                .collect(Collectors.toSet());
        curriculumValidator.validateIds(dbIds, requestIds);

        List<Integer> sortedOrders = request.reorders().stream()
                .map(LectureReorderRequest.LectureOrderElement::orderNo)
                .sorted()
                .toList();
        curriculumValidator.validateOrderSequence(sortedOrders);

        for (LectureReorderRequest.LectureOrderElement element : request.reorders()) {
            lectureRepository.updateOrderNo(element.lectureId(), element.orderNo(), chapterId);
        }
    }
}