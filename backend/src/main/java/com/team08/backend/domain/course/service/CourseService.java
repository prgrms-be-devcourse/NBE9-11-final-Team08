package com.team08.backend.domain.course.service;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.dto.CurriculumSaveRequest;
import com.team08.backend.domain.course.dto.ChapterSaveDto;
import com.team08.backend.domain.course.dto.LectureSaveDto;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.instructor.repository.InstructorProfileRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final InstructorProfileRepository instructorProfileRepository;

    @Transactional
    public Long createCourse(CourseCreateRequest request, Long userId) {
        if (!instructorProfileRepository.existsByUserIdAndApprovedAtIsNotNull(userId)) {
            throw new AccessDeniedException("강사 등록 및 승인이 필요한 서비스입니다.");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        User instructor = instructorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("강사 프로필을 찾을 수 없습니다."))
                .getUser();

        Course course = Course.builder()
                .category(category)
                .instructor(instructor)
                .title(request.title())
                .description(request.description())
                .thumbnail(request.thumbnail())
                .price(request.price())
                .build();

        return courseRepository.save(course).getId();
    }

    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findWithCurriculumById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 강의입니다."));
        return CourseDetailResponse.from(course);
    }

    @Transactional
    public void updateCourse(Long courseId, CourseUpdateRequest request, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의 상품입니다."));

        if (!course.getInstructor().getId().equals(userId)) {
            throw new AccessDeniedException("본인이 등록한 강의 상품만 수정할 수 있습니다.");
        }

        Category category = getCategoryOrNull(request.categoryId());

        course.update(
                request.title(),
                request.description(),
                request.thumbnail(),
                request.price(),
                category
        );
    }

    @Transactional
    public void deleteCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        if (!course.getInstructor().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 강의만 삭제할 수 있습니다.");
        }

        course.delete();
    }

    @Transactional
    public void saveCurriculum(Long courseId, CurriculumSaveRequest request, Long userId) {
        Course course = courseRepository.findWithCurriculumById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의 상품입니다."));

        if (!course.getInstructor().getId().equals(userId)) {
            throw new AccessDeniedException("본인이 등록한 강의 상품의 커리큘럼만 관리할 수 있습니다.");
        }

        List<ChapterSaveDto> chapterDtos = request.chapters() == null
                ? List.of()
                : request.chapters();

        // 요청에 포함된 챕터 ID 집합
        Set<Long> incomingChapterIds = chapterDtos.stream()
                .map(ChapterSaveDto::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 기존 챕터 중 요청에 없는 것은 삭제
        course.getChapters().removeIf(ch -> !incomingChapterIds.contains(ch.getId()));

        // 기존 챕터를 ID로 빠르게 조회
        Map<Long, Chapter> existingChapterById = course.getChapters().stream()
                .collect(Collectors.toMap(Chapter::getId, Function.identity()));

        for (ChapterSaveDto chapterDto : chapterDtos) {
            if (chapterDto.id() != null && existingChapterById.containsKey(chapterDto.id())) {
                // 수정
                Chapter chapter = existingChapterById.get(chapterDto.id());
                chapter.update(chapterDto.title(), chapterDto.orderNo());
                syncLectures(chapter, chapterDto.lectures());
            } else {
                // 신규 추가
                Chapter newChapter = Chapter.builder()
                        .course(course)
                        .title(chapterDto.title())
                        .orderNo(chapterDto.orderNo())
                        .build();
                syncLectures(newChapter, chapterDto.lectures());
                course.getChapters().add(newChapter);
            }
        }
    }

    private void syncLectures(Chapter chapter, List<LectureSaveDto> lectureDtos) {
        if (lectureDtos == null) {
            lectureDtos = List.of();
        }

        Set<Long> incomingLectureIds = lectureDtos.stream()
                .map(LectureSaveDto::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 요청에 없는 강의 삭제
        chapter.getLectures().removeIf(lec -> !incomingLectureIds.contains(lec.getId()));

        Map<Long, Lecture> existingLectureById = chapter.getLectures().stream()
                .collect(Collectors.toMap(Lecture::getId, Function.identity()));

        for (LectureSaveDto dto : lectureDtos) {
            if (dto.id() != null && existingLectureById.containsKey(dto.id())) {
                // 수정
                existingLectureById.get(dto.id())
                        .update(dto.videoId(), dto.title(), dto.durationSeconds(), dto.orderNo(), dto.isFreePreview());
            } else {
                // 신규 추가
                chapter.getLectures().add(Lecture.builder()
                        .chapter(chapter)
                        .videoId(dto.videoId())
                        .title(dto.title())
                        .durationSeconds(dto.durationSeconds())
                        .orderNo(dto.orderNo())
                        .isFreePreview(dto.isFreePreview())
                        .build());
            }
        }
    }

    private Category getCategoryOrNull(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }
}