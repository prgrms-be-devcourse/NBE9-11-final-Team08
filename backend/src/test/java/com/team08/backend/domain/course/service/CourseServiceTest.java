package com.team08.backend.domain.course.service;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.dto.CurriculumSaveRequest;
import com.team08.backend.domain.course.dto.ChapterSaveDto;
import com.team08.backend.domain.course.dto.LectureSaveDto;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void 강의등록_성공() {
        User seller = mock(User.class);
        given(seller.isSeller()).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(seller));

        Category category = mock(Category.class);
        given(categoryRepository.findById(10L)).willReturn(Optional.of(category));

        CourseCreateRequest request = new CourseCreateRequest(10L, "스프링 마스터", "설명", "thumb.png", 50000);
        Course course = mock(Course.class);
        given(course.getId()).willReturn(100L);
        given(courseRepository.save(any(Course.class))).willReturn(course);

        courseService.createCourse(request, 1L);

        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void 강의등록_실패_판매자권한없음() {
        User buyer = mock(User.class);
        given(buyer.isSeller()).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(buyer));

        CourseCreateRequest request = new CourseCreateRequest(10L, "스프링 마스터", "설명", "thumb.png", 50000);

        assertThrows(AccessDeniedException.class, () -> courseService.createCourse(request, 1L));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void 강의수정_실패_본인아님() {
        User instructor = newInstance(User.class);
        ReflectionTestUtils.setField(instructor, "id", 1L);

        Course course = Course.builder()
                .instructor(instructor)
                .title("기존 강의")
                .price(10000)
                .build();

        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        CourseUpdateRequest request = new CourseUpdateRequest(10L, "수정 제목", "수정 설명", "new.png", 20000);

        assertThrows(AccessDeniedException.class, () -> courseService.updateCourse(100L, request, 2L));
    }

    @Test
    void 강의삭제_실패_본인아님() {
        User instructor = newInstance(User.class);
        ReflectionTestUtils.setField(instructor, "id", 1L);

        Course course = Course.builder()
                .instructor(instructor)
                .title("삭제할 강의")
                .price(10000)
                .build();

        given(courseRepository.findById(100L)).willReturn(Optional.of(course));

        assertThrows(AccessDeniedException.class, () -> courseService.deleteCourse(100L, 2L));
    }

    @Test
    void 커리큘럼_저장_및_변경감지_성공() {
        User instructor = newInstance(User.class);
        ReflectionTestUtils.setField(instructor, "id", 1L);

        Course course = Course.builder()
                .instructor(instructor)
                .title("테스트 강의")
                .price(10000)
                .build();
        ReflectionTestUtils.setField(course, "id", 100L);

        given(courseRepository.findWithCurriculumById(100L)).willReturn(Optional.of(course));

        List<LectureSaveDto> lectures = List.of(new LectureSaveDto(null, "vid-1", "첫 번째 영상", 600, 1, true));
        List<ChapterSaveDto> chapters = List.of(new ChapterSaveDto(null, "새로운 챕터", 1, lectures));
        CurriculumSaveRequest request = new CurriculumSaveRequest(chapters);

        courseService.saveCurriculum(100L, request, 1L);

        assertEquals(1, course.getChapters().size());
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test entity.", e);
        }
    }
}