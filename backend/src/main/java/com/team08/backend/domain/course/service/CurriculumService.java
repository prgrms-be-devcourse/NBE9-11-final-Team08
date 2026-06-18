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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        List<Chapter> dbChapters = chapterRepository.findByCourseIdWithLecturesOrderByOrderNo(courseId);

        if (dbChapters.size() != request.reorders().size()) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }

        Set<Long> dbChapterIds = dbChapters.stream()
                .map(Chapter::getId)
                .collect(Collectors.toSet());

        Set<Long> requestIds = request.reorders().stream()
                .map(ChapterReorderRequest.ChapterOrderElement::chapterId)
                .collect(Collectors.toSet());

        if (!dbChapterIds.equals(requestIds)) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }

        List<Integer> sortedOrders = request.reorders().stream()
                .map(ChapterReorderRequest.ChapterOrderElement::orderNo)
                .sorted()
                .toList();

        validateOrderSequence(sortedOrders);

        for (ChapterReorderRequest.ChapterOrderElement element : request.reorders()) {
            chapterRepository.updateOrderNo(element.chapterId(), element.orderNo(), courseId);
        }
    }

    @Transactional
    public void reorderLectures(Long chapterId, Long instructorId, LectureReorderRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        chapter.getCourse().validateOwner(instructorId);

        List<Lecture> dbLectures = lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId);

        if (dbLectures.size() != request.reorders().size()) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }

        Set<Long> dbLectureIds = dbLectures.stream()
                .map(Lecture::getId)
                .collect(Collectors.toSet());

        Set<Long> requestIds = request.reorders().stream()
                .map(LectureReorderRequest.LectureOrderElement::lectureId)
                .collect(Collectors.toSet());

        if (!dbLectureIds.equals(requestIds)) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }

        List<Integer> sortedOrders = request.reorders().stream()
                .map(LectureReorderRequest.LectureOrderElement::orderNo)
                .sorted()
                .toList();

        validateOrderSequence(sortedOrders);

        for (LectureReorderRequest.LectureOrderElement element : request.reorders()) {
            lectureRepository.updateOrderNo(element.lectureId(), element.orderNo(), chapterId);
        }
    }

    private void validateOrderSequence(List<Integer> sortedOrders) {
        if (new HashSet<>(sortedOrders).size() != sortedOrders.size()) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }

        boolean isInvalid = IntStream.range(0, sortedOrders.size())
                .anyMatch(i -> sortedOrders.get(i) != i + 1);

        if (isInvalid) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }
    }
}