package com.team08.backend.domain.course.service;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.dto.CurriculumSaveRequest;
import com.team08.backend.domain.course.dto.ChapterSaveDto;
import com.team08.backend.domain.course.dto.LectureSaveDto;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;

    /**
     * 강의 상품 등록 (MVP 명세: SELLER만 등록 가능, 초기 상태 DRAFT)
     */
    @Transactional
    public Long createCourse(CourseCreateRequest request, User loginUser) {
        // 1. 권한 정책 검증
        if (!"SELLER".equals(loginUser.getRole())) {
            throw new AccessDeniedException("판매자(SELLER) 권한이 필요합니다.");
        }

        // 2. 카테고리 조회
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        }

        // 3. 빌더 패턴으로 Course 엔티티 생성
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

    /**
     * 강의 상품 상세 조회
     */
    public Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .filter(course -> course.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 강의입니다."));
    }

    /**
     * 강의 상품 수정 (MVP 명세: 본인이 등록한 강의만 가능, 타인 접근 시 403 차단)
     */
    @Transactional
    public void updateCourse(Long courseId, CourseUpdateRequest request, User loginUser) {
        // 1. 수정할 강의 존재 여부 확인
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의 상품입니다."));

        // 2. 본인 검증 로직
        if (!course.getInstructor().getId().equals(loginUser.getId())) {
            throw new AccessDeniedException("본인이 등록한 강의 상품만 수정할 수 있습니다.");
        }

        // 3. 카테고리 조회 및 수정 반영
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        }

        // 4. 엔티티 비즈니스 메서드로 정보 변경
        course.update(
                request.title(),
                request.description(),
                request.thumbnail(),
                request.price(),
                category
        );
    }

    /**
     * 강의 상품 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteCourse(Long courseId, User loginUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        // 본인만 삭제 가능
        if (!course.getInstructor().getId().equals(loginUser.getId())) {
            throw new AccessDeniedException("본인의 강의만 삭제할 수 있습니다.");
        }

        course.delete();
    }

    /**
     * 커리큘럼(Chapter - Lecture 계층) 일괄 저장 및 갱신 비즈니스 로직
     * (기획 명세: 본인이 등록한 강의만 수정 가능, 불일치 시 403 Forbidden 차단)
     */
    @Transactional
    public void saveCurriculum(Long courseId, CurriculumSaveRequest request, User loginUser) {
        // 1. 커리큘럼을 추가할 대상 강의 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의 상품입니다."));

        // 2. 본인 검증 로직
        if (!course.getInstructor().getId().equals(loginUser.getId())) {
            throw new AccessDeniedException("본인이 등록한 강의 상품의 커리큘럼만 관리할 수 있습니다.");
        }

        // 3. 기존에 해당 코스에 존재하던 챕터와 하위 영상들 깔끔하게 밀어버리기
        List<Chapter> oldChapters = chapterRepository.findByCourseIdOrderByOrderNoAsc(courseId);
        for (Chapter oldChapter : oldChapters) {
            List<Lecture> oldLectures = lectureRepository.findByChapterIdOrderByOrderNoAsc(oldChapter.getId());
            lectureRepository.deleteAllInBatch(oldLectures);
        }
        chapterRepository.deleteAllInBatch(oldChapters);

        // 4. 요청 스트림 데이터 바탕으로 새롭게 커리큘럼 저장 진행
        if (request.chapters() != null) {
            for (ChapterSaveDto chapterDto : request.chapters()) {

                Chapter newChapter = Chapter.builder()
                        .course(course)
                        .title(chapterDto.title())
                        .orderNo(chapterDto.orderNo())
                        .build();
                Chapter savedChapter = chapterRepository.save(newChapter);

                if (chapterDto.lectures() != null) {
                    for (LectureSaveDto lectureDto : chapterDto.lectures()) {

                        Lecture newLecture = Lecture.builder()
                                .chapter(savedChapter)
                                .youtubeVideoId(lectureDto.youtubeVideoId())
                                .title(lectureDto.title())
                                .durationSeconds(lectureDto.durationSeconds())
                                .orderNo(lectureDto.orderNo())
                                .isFreePreview(lectureDto.isFreePreview())
                                .build();
                        lectureRepository.save(newLecture);
                    }
                }
            }
        }
    }
}