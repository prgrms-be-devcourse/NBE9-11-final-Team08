package com.team08.backend.domain.course.repository;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 코스 상세 조회용 fetch 쿼리 회귀 테스트.
 * <p>
 * 과거 {@code findWithChaptersAndLecturesAsc} 는 chapters 와 lectures 두 List(bag)를
 * 한 쿼리에서 동시에 join fetch 해 {@code MultipleBagFetchException} 을 던졌다.
 * 목(Mock) 기반 서비스 단위테스트로는 JPQL 이 실제로 실행되지 않아 잡히지 않았으므로,
 * 실제 Hibernate 로 쿼리를 실행하는 @DataJpaTest 로 보강한다.
 */
@DataJpaTest
@Import(JpaConfig.class)
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("코스+챕터, 챕터+강의를 두 쿼리로 fetch 하면 예외 없이 orderNo 순으로 로드된다")
    void findChaptersAndLectures_inTwoQueries_loadsOrdered() {
        // given: orderNo 와 다른 순서로 추가해 정렬이 쿼리에서 보장되는지 확인
        Course course = Course.createDraft(1L, 2L, "코스", "설명", "thumb.jpg", 10_000);
        Chapter ch2 = Chapter.create("2장", 2, course);
        Chapter ch1 = Chapter.create("1장", 1, course);
        course.addChapter(ch2);
        course.addChapter(ch1);
        ch1.addLecture(Lecture.createDraft("1-2강", "s", 120, 2, false, ch1));
        ch1.addLecture(Lecture.createDraft("1-1강", "s", 60, 1, true, ch1));
        ch2.addLecture(Lecture.createDraft("2-1강", "s", 90, 1, false, ch2));

        Long courseId = courseRepository.save(course).getId();
        em.flush();
        em.clear();

        // when: (1) 코스+챕터 fetch → (2) 챕터+강의 fetch (같은 영속성 컨텍스트라 챕터에 강의가 채워진다)
        Course loaded = courseRepository.findWithChaptersAsc(courseId).orElseThrow();
        List<Chapter> chaptersWithLectures = courseRepository.findChaptersWithLecturesAsc(courseId);

        // then: 챕터가 orderNo 순으로 정렬
        assertThat(loaded.getChapters())
                .extracting(Chapter::getOrderNo)
                .containsExactly(1, 2);

        // 두 번째 쿼리가 같은 챕터 인스턴스에 강의를 채웠고, 강의도 orderNo 순으로 정렬
        assertThat(loaded.getChapters().get(0).getLectures())
                .extracting(Lecture::getOrderNo)
                .containsExactly(1, 2);
        assertThat(loaded.getChapters().get(1).getLectures())
                .extracting(Lecture::getOrderNo)
                .containsExactly(1);

        // findChaptersWithLecturesAsc 도 독립적으로 정렬된 결과를 반환
        assertThat(chaptersWithLectures)
                .extracting(Chapter::getOrderNo)
                .containsExactly(1, 2);
    }
}
