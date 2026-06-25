package com.team08.backend.domain.dashboard.integration;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 대시보드 집계 API 통합 테스트(Testcontainers MySQL).
 * 결정적인 소규모 시드로 KPI/이탈률/진행률을 검증하고, 비관리자 접근이 막히는지 확인한다.
 *
 * <p>시드 구성: 강사(SELLER) 1, 수강생(USER) 2, 관리자(ADMIN) 1, ON_SALE 강좌 1(강의 2개).
 * 수강생1은 두 강의 모두 완료(완강), 수강생2는 한 강의만 완료 → 이탈률 50%.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminDashboardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private LearningEventRepository learningEventRepository;
    @Autowired private LectureProgressRepository lectureProgressRepository;

    private static final LocalDateTime TODAY_NOON = LocalDate.now().atTime(12, 0);

    private Long courseId;
    private Long chapterId;
    private Long lecture1Id;
    private Long lecture2Id;

    @BeforeEach
    void seed() {
        User admin = userRepository.save(User.createAdmin("admin@test.com", "pw", "관리자", null));
        User seller = userRepository.save(User.createSeller("seller@test.com", "pw", "강사", null));
        User learner1 = userRepository.save(User.createUser("l1@test.com", "pw", "수강생1", null));
        User learner2 = userRepository.save(User.createUser("l2@test.com", "pw", "수강생2", null));

        // ON_SALE 강좌 + 강의 2개 (in-memory 그래프 구성 후 cascade 저장)
        Course course = Course.createDraft(seller.getId(), 1L, "코스", "설명", "thumb", 10000);
        Chapter chapter = Chapter.create("챕터1", 1, course);
        course.addChapter(chapter);
        Lecture lecture1 = Lecture.createDraft("강의1", "요약", 600, 1, false, chapter);
        Lecture lecture2 = Lecture.createDraft("강의2", "요약", 600, 2, false, chapter);
        chapter.addLecture(lecture1);
        chapter.addLecture(lecture2);
        courseRepository.save(course); // DRAFT 저장: id 부여 + 챕터/강의 cascade 영속
        course.requestReview(seller.getId()); // 상태 이력은 course.id 필요 → 저장 후 전환
        course.approve(admin.getId());
        courseRepository.save(course); // ON_SALE 상태 반영

        courseId = course.getId();
        chapterId = chapter.getId();
        lecture1Id = lecture1.getId();
        lecture2Id = lecture2.getId();

        enroll(learner1.getId());
        enroll(learner2.getId());

        // 수강생1: 두 강의 모두 완료 → 완강
        completeProgress(learner1.getId(), lecture1Id);
        completeProgress(learner1.getId(), lecture2Id);
        // 수강생2: 한 강의만 완료 → 미완강(이탈 집계 대상)
        completeProgress(learner2.getId(), lecture1Id);

        // 오늘 학습 이벤트: 입장(세션) 3, 완료 3, 순 학습자 2
        event(learner1.getId(), lecture1Id, LearningEventType.LECTURE_ENTER);
        event(learner1.getId(), lecture1Id, LearningEventType.LECTURE_COMPLETE);
        event(learner1.getId(), lecture2Id, LearningEventType.LECTURE_ENTER);
        event(learner1.getId(), lecture2Id, LearningEventType.LECTURE_COMPLETE);
        event(learner2.getId(), lecture1Id, LearningEventType.LECTURE_ENTER);
        event(learner2.getId(), lecture1Id, LearningEventType.LECTURE_COMPLETE);
    }

    private void enroll(Long userId) {
        Order order = Order.createPendingPayment(userId, "ORD-" + userId + "-" + courseId, TODAY_NOON);
        orderRepository.save(order);
        enrollmentRepository.save(Enrollment.createActive(userId, courseId, order, TODAY_NOON));
    }

    private void completeProgress(Long userId, Long lectureId) {
        lectureProgressRepository.save(new LectureProgress(
                null, lectureId, userId, 600, 600, 100, true, TODAY_NOON, TODAY_NOON, TODAY_NOON));
    }

    private void event(Long userId, Long lectureId, LearningEventType type) {
        learningEventRepository.save(LearningEvent.create(
                userId, courseId, chapterId, lectureId, type, 0, TODAY_NOON, null));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("overview: 플랫폼 전체 KPI를 집계한다")
    void overview() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(4))
                .andExpect(jsonPath("$.sellerCount").value(1))
                .andExpect(jsonPath("$.regularUserCount").value(2))
                .andExpect(jsonPath("$.onSaleCourseCount").value(1))
                .andExpect(jsonPath("$.activeEnrollmentCount").value(2))
                .andExpect(jsonPath("$.totalLearningEvents").value(6))
                .andExpect(jsonPath("$.totalCompletions").value(3))
                .andExpect(jsonPath("$.todaySessions").value(3))
                .andExpect(jsonPath("$.todayActiveLearners").value(2));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("courses: 이탈률을 단일 쿼리로 계산한다(완강 1/2 → 50%)")
    void courseStatsDropoutRate() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].courseId").value(courseId))
                .andExpect(jsonPath("$.content[0].enrollees").value(2))
                .andExpect(jsonPath("$.content[0].enterCount").value(3))
                .andExpect(jsonPath("$.content[0].completionCount").value(3))
                .andExpect(jsonPath("$.content[0].dropoutRate").value(50.0));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("enrollees: 수강자별 진행률을 계산한다(2/2=100%, 1/2=50%)")
    void enrolleeProgressRate() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/courses/{courseId}/enrollees", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].totalLectures", containsInAnyOrder(2, 2)))
                .andExpect(jsonPath("$.content[*].progressRate", containsInAnyOrder(100.0, 50.0)));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_ADMIN")
    @DisplayName("lectures: 강의별 입장/완료 수를 집계한다")
    void lectureStats() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/courses/{courseId}/lectures", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lectureId").value(lecture1Id))
                .andExpect(jsonPath("$[0].enterCount").value(2))
                .andExpect(jsonPath("$[0].completeCount").value(2))
                .andExpect(jsonPath("$[1].enterCount").value(1));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_USER")
    @DisplayName("비관리자(USER)는 대시보드에 접근하면 403")
    void nonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isForbidden());
    }
}
