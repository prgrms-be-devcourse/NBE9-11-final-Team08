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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class BulkDataInitializer {

    private static final int SELLER_COUNT      = 10;
    private static final int COURSES_PER_SELLER = 100;   // 총 1,000개
    private static final int CHAPTERS_PER_COURSE = 3;    // 총 3,000개
    private static final int LECTURES_PER_CHAPTER = 3;   // 총 9,000개
    private static final int USER_COUNT        = 100;
    private static final int BATCH_SIZE        = 500;

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
            log.info("[DataInit] 이미 데이터가 존재해 건너뜀");
            return;
        }

        String password = passwordEncoder.encode("Test1234!");

        List<Category> categories = saveCategories();
        List<User> sellers        = saveSellers(password);
        saveRegularUsers(password);
        saveCourses(sellers, categories, password);
    }

    private List<Category> saveCategories() {
        List<Category> roots = categoryRepository.saveAll(List.of(
                new Category(null, null, "개발",   0),
                new Category(null, null, "디자인", 0),
                new Category(null, null, "비즈니스", 0)
        ));
        List<Category> children = new ArrayList<>();
        children.addAll(List.of(
                new Category(null, roots.get(0).getId(), "백엔드",     1),
                new Category(null, roots.get(0).getId(), "프론트엔드", 1),
                new Category(null, roots.get(0).getId(), "DevOps",     1),
                new Category(null, roots.get(1).getId(), "UI/UX",      1),
                new Category(null, roots.get(2).getId(), "마케팅",     1)
        ));
        return categoryRepository.saveAll(children);
    }

    private List<User> saveSellers(String password) {
        List<User> sellers = new ArrayList<>();
        for (int i = 1; i <= SELLER_COUNT; i++) {
            User seller = userRepository.save(User.createSeller(
                    "seller" + i + "@test.com", password, "강사" + i, null));
            sellerRepository.save(new Seller(null, seller.getId(), LocalDateTime.now(), LocalDateTime.now()));
            sellers.add(seller);
        }
        log.info("[DataInit] 강사 {}명 생성", SELLER_COUNT);
        return sellers;
    }

    private void saveRegularUsers(String password) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= USER_COUNT; i++) {
            users.add(User.createUser("user" + i + "@test.com", password, "수강생" + i, null));
            if (users.size() == BATCH_SIZE) {
                userRepository.saveAll(users);
                users.clear();
            }
        }
        if (!users.isEmpty()) userRepository.saveAll(users);
        log.info("[DataInit] 수강생 {}명 생성", USER_COUNT);
    }

    private void saveCourses(List<User> sellers, List<Category> categories, String password) {
        CourseStatus[] statuses = {CourseStatus.ON_SALE, CourseStatus.ON_SALE, CourseStatus.ON_SALE, CourseStatus.DRAFT};
        int categoryCount = categories.size();

        List<Course> courseBatch   = new ArrayList<>();
        List<Chapter> chapterBatch = new ArrayList<>();
        List<Lecture> lectureBatch = new ArrayList<>();

        for (int s = 0; s < sellers.size(); s++) {
            User seller = sellers.get(s);
            for (int c = 0; c < COURSES_PER_SELLER; c++) {
                int idx = s * COURSES_PER_SELLER + c + 1;
                Course course = Course.builder()
                        .instructorId(seller.getId())
                        .categoryId(categories.get(idx % categoryCount).getId())
                        .title("강의 " + idx + " - " + categories.get(idx % categoryCount).getName())
                        .description("강의 " + idx + "에 대한 설명입니다.")
                        .thumbnail("thumb" + idx + ".jpg")
                        .price((idx % 5 + 1) * 10000)
                        .status(statuses[idx % statuses.length])
                        .build();
                courseBatch.add(course);
            }
        }

        List<Course> savedCourses = courseRepository.saveAll(courseBatch);
        log.info("[DataInit] 강의 {}개 생성", savedCourses.size());

        for (Course course : savedCourses) {
            for (int i = 1; i <= CHAPTERS_PER_COURSE; i++) {
                Chapter chapter = Chapter.builder()
                        .title(course.getTitle() + " - " + i + "장")
                        .orderNo(i)
                        .course(course)
                        .build();
                chapterBatch.add(chapter);

                if (chapterBatch.size() == BATCH_SIZE) {
                    List<Chapter> saved = chapterRepository.saveAll(chapterBatch);
                    addLectures(saved, lectureBatch);
                    chapterBatch.clear();
                    if (lectureBatch.size() >= BATCH_SIZE) {
                        lectureRepository.saveAll(lectureBatch);
                        lectureBatch.clear();
                    }
                }
            }
        }

        if (!chapterBatch.isEmpty()) {
            List<Chapter> saved = chapterRepository.saveAll(chapterBatch);
            addLectures(saved, lectureBatch);
        }
        if (!lectureBatch.isEmpty()) {
            lectureRepository.saveAll(lectureBatch);
        }

        log.info("[DataInit] 챕터 {}개, 강의영상 {}개 생성 완료",
                (long) savedCourses.size() * CHAPTERS_PER_COURSE,
                (long) savedCourses.size() * CHAPTERS_PER_COURSE * LECTURES_PER_CHAPTER);
    }

    private void addLectures(List<Chapter> chapters, List<Lecture> buffer) {
        for (Chapter chapter : chapters) {
            for (int j = 1; j <= LECTURES_PER_CHAPTER; j++) {
                buffer.add(Lecture.builder()
                        .title(chapter.getTitle() + " - " + j + "강")
                        .summary("강의 요약")
                        .durationSeconds(600 * j)
                        .orderNo(j)
                        .isFreePreview(j == 1)
                        .m3u8Path("/hls/" + chapter.getId() + "/" + j + "/index.m3u8")
                        .chapter(chapter)
                        .build());
            }
        }
    }
}
