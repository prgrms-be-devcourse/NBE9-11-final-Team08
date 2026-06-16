package com.team08.backend.global.init;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.seller.entity.Seller;
import com.team08.backend.domain.seller.repository.SellerRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class SimpleDataInitializer {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void init() {
        if (userRepository.count() > 0) {
            return;
        }

        String password = passwordEncoder.encode("Test1234!");

        // 사용자
        User admin  = userRepository.save(User.createUser("admin@test.com",  password, "관리자", null));
        User seller = userRepository.save(User.createSeller("seller@test.com", password, "강사",  null));
        User user1  = userRepository.save(User.createUser("user1@test.com",  password, "수강생1", null));
        User user2  = userRepository.save(User.createUser("user2@test.com",  password, "수강생2", null));

        sellerRepository.save(new Seller(null, seller.getId(), LocalDateTime.now(), LocalDateTime.now()));

        // 카테고리
        Category dev     = categoryRepository.save(new Category(null, null, "개발",       0));
        Category backend = categoryRepository.save(new Category(null, dev.getId(), "백엔드", 1));
        Category front   = categoryRepository.save(new Category(null, dev.getId(), "프론트엔드", 1));

        // 강의
        Course course1 = courseRepository.save(Course.builder()
                .instructorId(seller.getId()).categoryId(backend.getId())
                .title("Spring Boot 완성반").description("Spring Boot REST API 강의")
                .thumbnail("thumb1.jpg").price(50000).status(CourseStatus.ON_SALE).build());

        Course course2 = courseRepository.save(Course.builder()
                .instructorId(seller.getId()).categoryId(front.getId())
                .title("React 기초").description("React 기초부터 실전까지")
                .thumbnail("thumb2.jpg").price(30000).status(CourseStatus.ON_SALE).build());

        // 챕터 & 강의 영상
        saveChaptersAndLectures(course1, List.of("환경 설정", "REST API 만들기", "배포"));
        saveChaptersAndLectures(course2, List.of("React 기초", "상태 관리", "프로젝트"));
    }

    private void saveChaptersAndLectures(Course course, List<String> chapterTitles) {
        for (int i = 0; i < chapterTitles.size(); i++) {
            Chapter chapter = chapterRepository.save(Chapter.builder()
                    .title(chapterTitles.get(i)).orderNo(i + 1).course(course).build());

            for (int j = 1; j <= 3; j++) {
                lectureRepository.save(Lecture.builder()
                        .title(chapterTitles.get(i) + " - 강의 " + j)
                        .summary("요약").durationSeconds(600 * j).orderNo(j)
                        .isFreePreview(j == 1).m3u8Path("/hls/" + course.getId() + "/" + chapter.getId() + "/" + j + "/index.m3u8")
                        .chapter(chapter).build());
            }
        }
    }
}
