package com.team08.backend.domain.course.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.fixture.CourseFixture;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.course.service.CourseService.CourseViewCountManager;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseViewCountManager courseViewCountManager;

    @InjectMocks
    private CourseService courseService;

    @Test
    void 강좌를_성공적으로_생성하고_ID를_반환한다() {
        Long instructorId = 1L;
        CourseCreateRequest request = new CourseCreateRequest(
                "테스트 강좌",
                "강좌 설명입니다.",
                10L,
                15000,
                "thumbnail/path.png"
        );

        Course savedCourse = CourseFixture.course(100L, instructorId, request);

        given(courseRepository.save(any(Course.class))).willReturn(savedCourse);

        Long courseId = courseService.createCourse(instructorId, request);

        assertThat(courseId).isEqualTo(100L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void 강좌를_상세_조회하면_조회수가_1_증가하고_커리큘럼과_함께_반환한다() {
        Long courseId = 100L;
        Course course = TestEntityFactory.course(courseId);

        Chapter chapter = ChapterFixture.chapter(1L, "첫 번째 챕터", 1, course);
        Lecture freeLecture = LectureFixture.lecture(10L, "무료 맛보기 강의", "videos/free.m3u8", 600, 1, chapter);

        course.addChapter(chapter);
        chapter.addLecture(freeLecture);

        given(courseRepository.findWithChaptersAndLecturesAsc(courseId)).willReturn(Optional.of(course));

        CourseDetailResponse response = courseService.getCourseDetail(courseId);

        assertThat(response.id()).isEqualTo(courseId);
        assertThat(response.chapters()).hasSize(1);
        assertThat(response.chapters().get(0).lectures()).hasSize(1);
        verify(courseRepository).findWithChaptersAndLecturesAsc(courseId);
        verify(courseViewCountManager).increaseViewCountRequiresNew(courseId);
    }

    @Test
    void 무료_미리보기가_아닌_강의는_상세_조회_시_영상_주소가_노출되지_않는다() {
        Long courseId = 100L;
        Course course = TestEntityFactory.course(courseId);
        Chapter chapter = ChapterFixture.chapter(1L, "첫 번째 챕터", 1, course);

        Lecture paidLecture = Lecture.builder()
                .title("유료 본 강의")
                .m3u8Path("videos/paid.m3u8")
                .summary("요약")
                .durationSeconds(1200)
                .orderNo(1)
                .isFreePreview(false)
                .chapter(chapter)
                .build();

        course.addChapter(chapter);
        chapter.addLecture(paidLecture);

        given(courseRepository.findWithChaptersAndLecturesAsc(courseId)).willReturn(Optional.of(course));

        CourseDetailResponse response = courseService.getCourseDetail(courseId);

        assertThat(response.chapters().get(0).lectures().get(0).m3u8Path()).isNull();
        verify(courseRepository).findWithChaptersAndLecturesAsc(courseId);
        verify(courseViewCountManager).increaseViewCountRequiresNew(courseId);
    }

    @Test
    void 존재하지_않는_강좌_ID로_상세_조회_시_예외가_발생한다() {
        Long invalidCourseId = 999L;

        given(courseRepository.findWithChaptersAndLecturesAsc(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseDetail(invalidCourseId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(courseRepository).findWithChaptersAndLecturesAsc(invalidCourseId);
        verify(courseViewCountManager, never()).increaseViewCountRequiresNew(invalidCourseId);
    }

    @Test
    void 강좌_목록을_조회하면_판매_중인_강좌만_지정된_정렬_조건과_2순위_최신순_조건으로_반환한다() {
        Course course1 = TestEntityFactory.course(1L);
        Course course2 = TestEntityFactory.course(2L);
        List<Course> courses = List.of(course1, course2);

        Sort expectedSort = Sort.by(Sort.Direction.DESC, "viewCount")
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        given(courseRepository.findAllByStatus(eq(CourseStatus.ON_SALE), any(Sort.class))).willReturn(courses);

        List<CourseCardResponse> response = courseService.getCourses(CourseSortType.VIEW_DESC);

        assertThat(response).hasSize(2);
        verify(courseRepository).findAllByStatus(CourseStatus.ON_SALE, expectedSort);
    }

    @Test
    void 강좌_목록_조회_시_지정된_정렬_조건의_값이_동일하면_2순위인_최신순으로_정렬_조건이_체이닝된다() {
        Course course1 = TestEntityFactory.course(1L);
        Course course2 = TestEntityFactory.course(2L);
        given(courseRepository.findAllByStatus(eq(CourseStatus.ON_SALE), any(Sort.class))).willReturn(List.of(course1, course2));

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        courseService.getCourses(CourseSortType.VIEW_DESC);

        verify(courseRepository).findAllByStatus(eq(CourseStatus.ON_SALE), sortCaptor.capture());
        Sort capturedSort = sortCaptor.getValue();

        Sort.Order primaryOrder = capturedSort.getOrderFor("viewCount");
        Sort.Order secondaryOrder = capturedSort.getOrderFor("createdAt");

        assertThat(primaryOrder).isNotNull();
        assertThat(primaryOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(secondaryOrder).isNotNull();
        assertThat(secondaryOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}