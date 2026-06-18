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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
        validateSize(dbChapters.size(), request.reorders().size());

        Map<Long, Chapter> chapterMap = dbChapters.stream()
                .collect(Collectors.toMap(Chapter::getId, Function.identity()));

        validateIds(chapterMap.keySet(), request.reorders().stream().map(ChapterReorderRequest.ChapterOrderElement::chapterId).collect(Collectors.toSet()));

        List<Integer> sortedOrders = request.reorders().stream()
                .map(ChapterReorderRequest.ChapterOrderElement::orderNo)
                .sorted()
                .toList();
        validateOrderSequence(sortedOrders);

        for (ChapterReorderRequest.ChapterOrderElement element : request.reorders()) {
            chapterMap.get(element.chapterId()).updateOrderNo(element.orderNo());
        }
    }

    @Transactional
    public void reorderLectures(Long chapterId, Long instructorId, LectureReorderRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
        chapter.getCourse().validateOwner(instructorId);

        List<Lecture> dbLectures = lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId);
        validateSize(dbLectures.size(), request.reorders().size());

        Map<Long, Lecture> lectureMap = dbLectures.stream()
                .collect(Collectors.toMap(Lecture::getId, Function.identity()));

        validateIds(lectureMap.keySet(), request.reorders().stream().map(LectureReorderRequest.LectureOrderElement::lectureId).collect(Collectors.toSet()));

        List<Integer> sortedOrders = request.reorders().stream()
                .map(LectureReorderRequest.LectureOrderElement::orderNo)
                .sorted()
                .toList();
        validateOrderSequence(sortedOrders);

        for (LectureReorderRequest.LectureOrderElement element : request.reorders()) {
            lectureMap.get(element.lectureId()).updateOrderNo(element.orderNo());
        }
    }

    private void validateSize(int dbSize, int requestSize) {
        if (dbSize != requestSize) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }
    }

    private void validateIds(Set<Long> dbIds, Set<Long> requestIds) {
        if (!dbIds.equals(requestIds)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }
    }

    private void validateOrderSequence(List<Integer> sortedOrders) {
        if (new HashSet<>(sortedOrders).size() != sortedOrders.size()) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }

        boolean isInvalid = IntStream.range(0, sortedOrders.size())
                .anyMatch(i -> sortedOrders.get(i) != i + 1);

        if (isInvalid) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }
    }
}