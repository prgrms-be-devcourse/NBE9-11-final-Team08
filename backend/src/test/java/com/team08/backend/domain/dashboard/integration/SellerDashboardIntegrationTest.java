package com.team08.backend.domain.dashboard.integration;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
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
 * 판매자 대시보드 "판매 분석" API 통합 테스트(Testcontainers MySQL).
 * 결정적인 소규모 시드로 본인 강좌 매출/수강생/카테고리/인기상품을 검증하고,
 * 타 판매자 데이터가 섞이지 않는지·비판매자 접근이 막히는지 확인한다.
 *
 * <p>로그인 판매자(instructorId = {@link #SELLER_ID})는 {@code @WithMockLoginUser(id=SELLER_ID)}로 고정한다.
 * 시드 구성:
 * <ul>
 *   <li>본인 ON_SALE 강좌 A(개발, 10,000원) — 수강생 2명, 결제완료 2건</li>
 *   <li>본인 DRAFT 강좌 B(디자인, 20,000원) — 수강생 1명, 결제완료 1건</li>
 *   <li>타 판매자 강좌 C(기타, 30,000원) — 집계에서 제외돼야 함</li>
 * </ul>
 * 기대값: 총매출 40,000 / 총판매 3건 / 총수강생 3명 / 전체강좌 2개 / 판매중 1개.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerDashboardIntegrationTest {

    private static final long SELLER_ID = 9001L;
    private static final long OTHER_SELLER_ID = 9002L;
    private static final LocalDateTime NOW = LocalDate.now().atTime(12, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private LearningEventRepository learningEventRepository;

    private Long courseAId;
    private Long courseBId;
    private Long courseCId;
    private Long lectureAId;

    @BeforeEach
    void seed() {
        User admin = userRepository.save(User.createAdmin("admin@test.com", "pw", "관리자", null));
        User learner1 = userRepository.save(User.createUser("l1@test.com", "pw", "수강생1", null));
        User learner2 = userRepository.save(User.createUser("l2@test.com", "pw", "수강생2", null));

        Category dev = categoryRepository.save(new Category(null, null, "개발", 1));
        Category design = categoryRepository.save(new Category(null, null, "디자인", 1));
        Category etc = categoryRepository.save(new Category(null, null, "기타", 1));

        // 본인 ON_SALE 강좌 A (개발, 10,000원) — 리뷰 요청을 위해 챕터/강의 1개씩 구성
        Course a = Course.createDraft(SELLER_ID, dev.getId(), "코스A", "설명", "thumb", 10000);
        Chapter chapter = Chapter.create("챕터1", 1, a);
        a.addChapter(chapter);
        Lecture lecture = Lecture.createDraft("강의1", "요약", 600, 1, false, chapter);
        chapter.addLecture(lecture);
        courseRepository.save(a);
        a.requestReview(SELLER_ID);
        a.approve(admin.getId());
        courseRepository.save(a);
        courseAId = a.getId();
        lectureAId = lecture.getId();

        // 본인 DRAFT 강좌 B (디자인, 20,000원) — 판매중은 아니지만 수강생/카테고리/인기상품 집계엔 포함
        Course b = Course.createDraft(SELLER_ID, design.getId(), "코스B", "설명", "thumb", 20000);
        courseRepository.save(b);
        courseBId = b.getId();

        // 타 판매자 강좌 C (기타) — 모든 집계에서 제외돼야 함
        Course c = Course.createDraft(OTHER_SELLER_ID, etc.getId(), "코스C", "설명", "thumb", 30000);
        courseRepository.save(c);
        courseCId = c.getId();

        // 결제완료 + 수강(ACTIVE)
        paidEnroll(learner1.getId(), courseAId, "코스A", 10000);
        paidEnroll(learner2.getId(), courseAId, "코스A", 10000);
        paidEnroll(learner1.getId(), courseBId, "코스B", 20000);
        paidEnroll(learner2.getId(), courseCId, "코스C", 30000); // 타 판매자 → 제외

        // 강의 A 멈춤(VIDEO_PAUSE) 이벤트 (길이 600초) — 멈춘 위치만 직접 집계
        // 90초 부근(어려운 구간)에 멈춤 집중: l1 두 번(90,100) + l2 한 번(90) = 3
        int t = 0;
        event(learner1.getId(), LearningEventType.VIDEO_PAUSE, 90, t++);
        event(learner1.getId(), LearningEventType.VIDEO_PAUSE, 100, t++);
        event(learner1.getId(), LearningEventType.VIDEO_PAUSE, 540, t++);
        event(learner2.getId(), LearningEventType.VIDEO_PAUSE, 90, t++);
        event(learner2.getId(), LearningEventType.VIDEO_PAUSE, 330, t++);

        // 완강 이벤트 2건 (completions=2)
        event(learner1.getId(), LearningEventType.LECTURE_COMPLETE, null, t++);
        event(learner2.getId(), LearningEventType.LECTURE_COMPLETE, null, t++);
    }

    private void event(Long userId, LearningEventType type, Integer pos, int secondOffset) {
        learningEventRepository.save(LearningEvent.create(
                userId, courseAId, null, lectureAId, type, pos, NOW.plusSeconds(secondOffset), null));
    }

    private void paidEnroll(Long userId, Long courseId, String title, int price) {
        Order order = Order.createPendingPayment(userId, "ORD-" + userId + "-" + courseId, NOW);
        order.addItem(courseId, title, price, NOW);
        order.markPaid(NOW);
        orderRepository.save(order);
        enrollmentRepository.save(Enrollment.createActive(userId, courseId, order, NOW));
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("analytics: 본인 강좌의 KPI를 집계한다(타 판매자 제외)")
    void analyticsKpi() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(40000))
                .andExpect(jsonPath("$.totalOrders").value(3))
                .andExpect(jsonPath("$.totalStudents").value(3))
                .andExpect(jsonPath("$.totalCourses").value(2))
                .andExpect(jsonPath("$.onSaleCourses").value(1));
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("analytics: 6개월 시계열을 0으로 채워 반환하고 이번 달 매출/건수를 담는다")
    void analyticsMonthlySeries() throws Exception {
        String thisMonth = LocalDate.now().getMonthValue() + "월";
        mockMvc.perform(get("/api/seller/dashboard/analytics").param("range", "6m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthly.length()").value(6))
                .andExpect(jsonPath("$.monthly[0].revenue").value(0))
                .andExpect(jsonPath("$.monthly[5].month").value(thisMonth))
                .andExpect(jsonPath("$.monthly[5].revenue").value(40000))
                .andExpect(jsonPath("$.monthly[5].orders").value(3))
                // 직전 달(0) 대비 이번 달 증가 → 100%
                .andExpect(jsonPath("$.revenueDelta").value(100.0))
                .andExpect(jsonPath("$.ordersDelta").value(100.0));
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("analytics: 카테고리별 수강생 비중을 집계한다(기타=타 판매자 제외)")
    void analyticsCategoryDistribution() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.categories[*].name", containsInAnyOrder("개발", "디자인")))
                .andExpect(jsonPath("$.categories[*].value", containsInAnyOrder(2, 1)));
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("analytics: 인기 상품을 수강생 수 기준으로 정렬한다(A 2명 > B 1명)")
    void analyticsTopCourses() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCourses.length()").value(2))
                .andExpect(jsonPath("$.topCourses[0].courseId").value(courseAId))
                .andExpect(jsonPath("$.topCourses[0].studentCount").value(2))
                .andExpect(jsonPath("$.topCourses[0].revenue").value(20000))
                .andExpect(jsonPath("$.topCourses[1].courseId").value(courseBId))
                .andExpect(jsonPath("$.topCourses[1].studentCount").value(1));
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("analytics: 강좌별 매출 내역을 매출 순으로 반환한다(B 20,000 > A 20,000, 타 판매자 제외)")
    void analyticsCourseBreakdown() throws Exception {
        // A: 결제 2건·매출 20,000·수강생 2, B: 결제 1건·매출 20,000·수강생 1
        // 매출 동률이면 수강생 내림차순 → A가 먼저
        mockMvc.perform(get("/api/seller/dashboard/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseBreakdown.length()").value(2))
                .andExpect(jsonPath("$.courseBreakdown[*].courseId", containsInAnyOrder(
                        courseAId.intValue(), courseBId.intValue())))
                .andExpect(jsonPath("$.courseBreakdown[0].courseId").value(courseAId))
                .andExpect(jsonPath("$.courseBreakdown[0].status").value("ON_SALE"))
                .andExpect(jsonPath("$.courseBreakdown[0].studentCount").value(2))
                .andExpect(jsonPath("$.courseBreakdown[0].orders").value(2))
                .andExpect(jsonPath("$.courseBreakdown[0].revenue").value(20000))
                .andExpect(jsonPath("$.courseBreakdown[1].courseId").value(courseBId))
                .andExpect(jsonPath("$.courseBreakdown[1].status").value("DRAFT"))
                .andExpect(jsonPath("$.courseBreakdown[1].orders").value(1))
                .andExpect(jsonPath("$.courseBreakdown[1].revenue").value(20000));
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("courseDetail: 단일 강좌 KPI·강의별 참여도를 집계한다")
    void courseDetail() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/courses/{courseId}", courseAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseAId))
                .andExpect(jsonPath("$.status").value("ON_SALE"))
                .andExpect(jsonPath("$.totalRevenue").value(20000))
                .andExpect(jsonPath("$.totalOrders").value(2))
                .andExpect(jsonPath("$.activeStudents").value(2))
                .andExpect(jsonPath("$.completions").value(2))
                .andExpect(jsonPath("$.monthly.length()").value(6))
                .andExpect(jsonPath("$.lectures.length()").value(1))
                .andExpect(jsonPath("$.lectures[0].lectureId").value(lectureAId))
                .andExpect(jsonPath("$.lectures[0].durationSeconds").value(600))
                .andExpect(jsonPath("$.lectures[0].completeCount").value(2))
                .andExpect(jsonPath("$.lectures[0].viewerCount").value(2))
                // VIDEO_PAUSE 위치 평균 (90,100,540,90,330) = 1150/5 = 230
                .andExpect(jsonPath("$.lectures[0].avgWatchSeconds").value(230));
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("courseDetail: 타 판매자 강좌를 조회하면 403")
    void courseDetailNotOwner() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/courses/{courseId}", courseCId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("courseDetail: 없는 강좌는 404")
    void courseDetailNotFound() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/courses/{courseId}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockLoginUser(id = SELLER_ID, role = "ROLE_SELLER")
    @DisplayName("lecturePauses: VIDEO_PAUSE 위치로 어려워서 멈춘 구간 히트맵을 만든다")
    void lecturePauses() throws Exception {
        // bins=10 → 60초 단위. 멈춤 위치 90,100,540,90,330 누적
        // bin1(60~120)=3(최대), bin5(300~360)=1, bin9(540~600)=1
        mockMvc.perform(get("/api/seller/dashboard/lectures/{lectureId}/pauses", lectureAId)
                        .param("bins", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lectureId").value(lectureAId))
                .andExpect(jsonPath("$.durationSeconds").value(600))
                .andExpect(jsonPath("$.binSeconds").value(60))
                .andExpect(jsonPath("$.totalPauses").value(5))
                .andExpect(jsonPath("$.viewerCount").value(2))
                .andExpect(jsonPath("$.bins.length()").value(10))
                .andExpect(jsonPath("$.bins[1].count").value(3))
                .andExpect(jsonPath("$.bins[1].heat").value(1.0))
                .andExpect(jsonPath("$.bins[5].count").value(1))
                .andExpect(jsonPath("$.bins[9].count").value(1))
                .andExpect(jsonPath("$.hotspots[0].startSeconds").value(60))
                .andExpect(jsonPath("$.hotspots[0].count").value(3));
    }

    @Test
    @WithMockLoginUser(role = "ROLE_USER")
    @DisplayName("비판매자(USER)는 판매 분석에 접근하면 403")
    void nonSellerForbidden() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/analytics"))
                .andExpect(status().isForbidden());
    }
}
