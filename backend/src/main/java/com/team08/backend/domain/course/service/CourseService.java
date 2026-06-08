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
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createCourse(CourseCreateRequest request, Long userId) {
        User loginUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));

        if (!loginUser.isSeller()) {
            throw new AccessDeniedException("판매자(SELLER) 권한이 필요합니다.");
        }

        Category category = getCategoryOrNull(request.categoryId());

        Course course = Course.builder()
                .instructor(loginUser)
                .category(category)
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

        if (request.chapters() == null || request.chapters().isEmpty()) {
            course.clearChapters();
            return;
        }

        Map<String, Chapter> existingChapterMap = course.getChapters().stream()
                .collect(Collectors.toMap(Chapter::getTitle, Function.identity(), (o1, o2) -> o1));

        List<Chapter> updatedChapters = new ArrayList<>();

        for (ChapterSaveDto chapterDto : request.chapters()) {
            Chapter chapter = existingChapterMap.get(chapterDto.title());

            if (chapter != null) {
                chapter.update(chapterDto.title(), chapterDto.orderNo());
            } else {
                chapter = Chapter.builder()
                        .course(course)
                        .title(chapterDto.title())
                        .orderNo(chapterDto.orderNo())
                        .build();
                chapter.setCourse(course);
            }

            if (chapterDto.lectures() != null) {
                Map<String, Lecture> existingLectureMap = chapter.getLectures().stream()
                        .collect(Collectors.toMap(Lecture::getTitle, Function.identity(), (o1, o2) -> o1));

                List<Lecture> updatedLectures = new ArrayList<>();

                for (LectureSaveDto lectureDto : chapterDto.lectures()) {
                    Lecture lecture = existingLectureMap.get(lectureDto.title());

                    if (lecture != null) {
                        lecture.update(lectureDto.videoId(), lectureDto.title(), lectureDto.durationSeconds(), lectureDto.orderNo(), lectureDto.isFreePreview());
                    } else {
                        lecture = Lecture.builder()
                                .chapter(chapter)
                                .videoId(lectureDto.videoId())
                                .title(lectureDto.title())
                                .durationSeconds(lectureDto.durationSeconds())
                                .orderNo(lectureDto.orderNo())
                                .isFreePreview(lectureDto.isFreePreview())
                                .build();
                    }
                    updatedLectures.add(lecture);
                }

                chapter.getLectures().clear();
                chapter.getLectures().addAll(updatedLectures);
            } else {
                chapter.getLectures().clear();
            }

            updatedChapters.add(chapter);
        }

        course.getChapters().clear();
        course.getChapters().addAll(updatedChapters);
    }

    private Category getCategoryOrNull(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }
}