package com.team08.backend.global.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import com.team08.backend.domain.aifeedback.repository.AiFeedbackRepository;
import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.couponpolicy.entity.*;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.domain.coursestatushistory.repository.CourseStatusHistoryRepository;
import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.entity.RequestStatus;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.domain.lectureqna.repository.QnaAnswerRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.domain.lecturereflection.entity.LectureReflection;
import com.team08.backend.domain.lecturereflection.repository.LectureReflectionRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.ordercouponusage.entity.OrderCouponUsage;
import com.team08.backend.domain.ordercouponusage.repository.OrderCouponUsageRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.domain.seller.entity.Seller;
import com.team08.backend.domain.seller.repository.SellerRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.studyreport.entity.StudyDailyStat;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.team08.backend.domain.studyreport.repository.StudyDailyStatRepository;
import com.team08.backend.domain.studyreport.repository.StudyReportRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 더미 데이터 생성 로직을 한곳에 모은 공유 시더.
 * <p>
 * SimpleDataInitializer / BulkDataInitializer 는 {@link SeedConfig} 의 "수량"만 다르게 넘기고,
 * 실제 엔티티 생성 코드는 모두 이 클래스에서만 관리한다. (엔티티 변경 시 수정 지점이 한 곳)
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder {

    /** 생성 규모 설정. 값만 바꾸면 simple/bulk 가 동일 로직으로 동작한다. */
    public record SeedConfig(
            int sellerCount,
            int coursesPerSeller,
            int chaptersPerCourse,
            int lecturesPerChapter,
            int userCount,
            int couponPolicyCount,
            int batchSize
    ) {
        public int courseCount() {
            return sellerCount * coursesPerSeller;
        }
    }

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final CourseStatusHistoryRepository courseStatusHistoryRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final CartRepository cartRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final OrderRepository orderRepository;
    private final OrderCouponUsageRepository orderCouponUsageRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LearningEventRepository learningEventRepository;
    private final AttendanceRepository attendanceRepository;
    private final LectureReflectionRepository lectureReflectionRepository;
    private final QnaQuestionRepository qnaQuestionRepository;
    private final QnaAnswerRepository qnaAnswerRepository;
    private final LectureModificationRequestRepository lectureModificationRequestRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final StudyReportRepository studyReportRepository;
    private final StudyDailyStatRepository studyDailyStatRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final PasswordEncoder passwordEncoder;

    /** 강의(Course)별 대표 Lecture + 전체 Lecture 목록을 묶어 카탈로그 결과를 전달한다. */
    private record CatalogData(List<Course> courses,
                              Map<Long, Lecture> sampleLectureByCourse,
                              Map<Long, List<Lecture>> lecturesByCourse) {}

    /** @return 실제로 데이터를 새로 생성했으면 true, 이미 데이터가 있어 건너뛰면 false */
    @Transactional
    public boolean seed(SeedConfig cfg) {
        if (userRepository.count() > 0) {
            log.info("[DataInit] 이미 데이터가 존재해 건너뜀");
            return false;
        }

        String password = passwordEncoder.encode("Test1234!");

        userRepository.save(User.createAdmin("admin@test.com", password, "관리자", null));

        List<Category> categories = saveCategories();
        List<User> sellers        = saveSellers(cfg, password);
        List<User> users          = saveRegularUsers(cfg, password);
        CatalogData catalog       = saveCourses(cfg, sellers, categories);

        seedCommerce(cfg, users, catalog);
        seedLearning(users, catalog);
        seedQna(users, catalog);
        seedStudies(sellers, users, catalog);
        return true;
    }

    private List<Category> saveCategories() {
        List<Category> roots = categoryRepository.saveAll(List.of(
                new Category(null, null, "개발",   0),
                new Category(null, null, "디자인", 0),
                new Category(null, null, "비즈니스", 0)
        ));
        List<Category> children = new ArrayList<>(List.of(
                new Category(null, roots.get(0).getId(), "백엔드",     1),
                new Category(null, roots.get(0).getId(), "프론트엔드", 1),
                new Category(null, roots.get(0).getId(), "DevOps",     1),
                new Category(null, roots.get(1).getId(), "UI/UX",      1),
                new Category(null, roots.get(2).getId(), "마케팅",     1)
        ));
        return categoryRepository.saveAll(children);
    }

    private List<User> saveSellers(SeedConfig cfg, String password) {
        List<User> sellers = new ArrayList<>();
        for (int i = 1; i <= cfg.sellerCount(); i++) {
            User seller = userRepository.save(User.createSeller(
                    "seller" + i + "@test.com", password, "강사" + i, null));
            sellerRepository.save(new Seller(null, seller.getId(), LocalDateTime.now(), LocalDateTime.now()));
            sellers.add(seller);
        }
        log.info("[DataInit] 강사 {}명 생성", cfg.sellerCount());
        return sellers;
    }

    private List<User> saveRegularUsers(SeedConfig cfg, String password) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= cfg.userCount(); i++) {
            users.add(User.createUser("user" + i + "@test.com", password, "수강생" + i, null));
        }
        List<User> saved = userRepository.saveAll(users);
        log.info("[DataInit] 수강생 {}명 생성", cfg.userCount());
        return saved;
    }

    private CatalogData saveCourses(SeedConfig cfg, List<User> sellers, List<Category> categories) {
        int categoryCount = categories.size();
        int batchSize = cfg.batchSize();

        List<Course> courseBatch   = new ArrayList<>();
        List<Chapter> chapterBatch = new ArrayList<>();
        List<Lecture> lectureBatch = new ArrayList<>();
        Map<Long, Lecture> sampleLectureByCourse = new HashMap<>();
        Map<Long, List<Lecture>> lecturesByCourse = new HashMap<>();

        for (int s = 0; s < sellers.size(); s++) {
            User seller = sellers.get(s);
            for (int c = 0; c < cfg.coursesPerSeller(); c++) {
                int idx = s * cfg.coursesPerSeller() + c + 1;
                courseBatch.add(Course.createDraft(
                        seller.getId(),
                        categories.get(idx % categoryCount).getId(),
                        "강의 " + idx + " - " + categories.get(idx % categoryCount).getName(),
                        "강의 " + idx + "에 대한 설명입니다.",
//                        "thumb" + idx + ".jpg",
                        "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800&auto=format&fit=crop",
                        (idx % 5 + 1) * 10000
                ));
            }
        }

        List<Course> savedCourses = courseRepository.saveAll(courseBatch);
        log.info("[DataInit] 강의 {}개 생성", savedCourses.size());

        for (Course course : savedCourses) {
            for (int i = 1; i <= cfg.chaptersPerCourse(); i++) {
                chapterBatch.add(Chapter.create(
                        course.getTitle() + " - " + i + "장",
                        i,
                        course
                ));

                if (chapterBatch.size() >= batchSize) {
                    addLectures(cfg, chapterRepository.saveAll(chapterBatch), lectureBatch);
                    chapterBatch.clear();
                    if (lectureBatch.size() >= batchSize) {
                        persistLectures(lectureBatch, sampleLectureByCourse, lecturesByCourse);
                    }
                }
            }
        }

        if (!chapterBatch.isEmpty()) {
            addLectures(cfg, chapterRepository.saveAll(chapterBatch), lectureBatch);
        }
        if (!lectureBatch.isEmpty()) {
            persistLectures(lectureBatch, sampleLectureByCourse, lecturesByCourse);
        }

        log.info("[DataInit] 챕터 {}개, 강의영상 {}개 생성 완료",
                (long) savedCourses.size() * cfg.chaptersPerCourse(),
                (long) savedCourses.size() * cfg.chaptersPerCourse() * cfg.lecturesPerChapter());

        return new CatalogData(savedCourses, sampleLectureByCourse, lecturesByCourse);
    }

    private void addLectures(SeedConfig cfg, List<Chapter> chapters, List<Lecture> buffer) {
        for (Chapter chapter : chapters) {
            for (int j = 1; j <= cfg.lecturesPerChapter(); j++) {
                String mockUuid = UUID.randomUUID().toString();
                buffer.add(Lecture.createWithStream(
                        "/hls/" + chapter.getId() + "/" + mockUuid + "/index.m3u8",
                        mockUuid,
                        chapter.getTitle() + " - " + j + "강",
                        "강의 요약",
                        600 * j,
                        j,
                        j == 1,
                        chapter
                ));
            }
        }
    }

    /** 강의 영상 저장 + 강의(Course)별 대표/전체 Lecture 캡처 (in-memory 참조라 추가 조회 없음) */
    private void persistLectures(List<Lecture> buffer, Map<Long, Lecture> sampleLectureByCourse,
                                 Map<Long, List<Lecture>> lecturesByCourse) {
        for (Lecture lecture : lectureRepository.saveAll(buffer)) {
            Long courseId = lecture.getChapter().getCourse().getId();
            sampleLectureByCourse.putIfAbsent(courseId, lecture);
            lecturesByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(lecture);
        }
        buffer.clear();
    }

    /** 장바구니 → 쿠폰 → 주문/결제/수강 (수강생 1명당 1건) */
    private void seedCommerce(SeedConfig cfg, List<User> users, CatalogData catalog) {
        List<Course> courses = catalog.courses();
        LocalDateTime now = LocalDateTime.now();
        int count = Math.min(users.size(), courses.size());
        int policyCount = Math.max(1, Math.min(cfg.couponPolicyCount(), courses.size()));

        // 장바구니 (CartItem 은 cascade 로 함께 저장)
        List<Cart> carts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Cart cart = Cart.create(users.get(i).getId());
            cart.addItem(courses.get(i).getId());
            carts.add(cart);
        }
        cartRepository.saveAll(carts);

        // 쿠폰 정책 (CouponPolicyCourse 는 cascade 로 함께 저장)
        List<CouponPolicy> policies = new ArrayList<>();
        for (int p = 0; p < policyCount; p++) {
            policies.add(
                    CouponPolicy.createPolicy(
                            "쿠폰 정책 " + (p + 1),
                            CouponTarget.COURSE,
                            CouponType.ADMIN,
                            null, // totalQuantity
                            CouponUsageType.SINGLE_USE,
                            false,

                            DiscountType.PERCENT,
                            10,     // discountValue
                            5000,   // maxDiscountAmount
                            null,   // minOrderAmount
                            30,     // validDays

                            now.minusDays(1),
                            now.plusDays(30),

                            null, // categoryIds
                            List.of(courses.get(p).getId()) // courseIds
                    )
            );
        }
        policies.addAll(autoRewardPolicies(now));
        List<CouponPolicy> savedPolicies = couponPolicyRepository.saveAll(policies);

        // 쿠폰 발급 (수강생 1명당 1장)
        List<IssuedCoupon> coupons = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coupons.add(IssuedCoupon.create(savedPolicies.get(i % policyCount), users.get(i).getId(), now));
        }
        List<IssuedCoupon> savedCoupons = issuedCouponRepository.saveAll(coupons);

        // 주문 (결제 완료 상태) — OrderItem 은 addItem 으로 추가되어 cascade 저장된다.
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Course course = courses.get(i);
            Order order = Order.createPendingPayment(users.get(i).getId(), "ORD-SEED-" + (i + 1), now);
            order.addItem(course.getId(), course.getTitle(), course.getPrice(), now);
            order.markPaid(now);
            orders.add(order);
        }
        List<Order> savedOrders = orderRepository.saveAll(orders);

        // 결제 / 수강등록 / 쿠폰사용
        List<Payment> payments = new ArrayList<>();
        List<Enrollment> enrollments = new ArrayList<>();
        List<OrderCouponUsage> usages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Course course = courses.get(i);
            Order order = savedOrders.get(i);
            int discount = savedPolicies.get(i % policyCount).calculateDiscountAmount(course.getPrice());

            Payment payment = Payment.createReady(order, now);
            payment.markProcessing(LocalDateTime.now());
            payment.succeed("PAY-SEED-" + (i + 1), "CARD", now);
            payments.add(payment);

            enrollments.add(Enrollment.createActive(users.get(i).getId(), course.getId(), order, now));
            usages.add(new OrderCouponUsage(null, order.getId(), savedCoupons.get(i).getId(), discount));
        }
        paymentRepository.saveAll(payments);
        enrollmentRepository.saveAll(enrollments);
        orderCouponUsageRepository.saveAll(usages);

        log.info("[DataInit] 커머스 데이터 생성 (주문 {}건)", savedOrders.size());
    }

    /** 학습자 1인이 시청하는 최대 강의 수(진행도 분포용). */
    private static final int LECTURES_PER_LEARNER = 5;
    /** 학습 활동을 흩뿌릴 날짜 범위(잔디/일별진도용). */
    private static final int SPREAD_DAYS = 14;

    /**
     * 학습 연쇄 데이터: 모든 수강생이 강좌의 여러 강의를 여러 날에 걸쳐 시청한 것으로
     * lecture_progresses · learning_events · learning_daily_stats 를 서로 일관되게 생성한다.
     * 일부는 완강(COMPLETE), 일부는 중도 이탈로 진행도 분포를 만든다.
     * (study_reports 는 이 데이터로부터 리포트 조회 시점에 재집계된다.)
     */
    private void seedLearning(List<User> users, CatalogData catalog) {
        List<Course> courses = catalog.courses();
        Map<Long, List<Lecture>> lecturesByCourse = catalog.lecturesByCourse();
        if (courses.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        List<LectureProgress> progresses = new ArrayList<>();
        List<LearningEvent> events = new ArrayList<>();
        List<Attendance> attendances = new ArrayList<>();
        List<LectureReflection> reflections = new ArrayList<>();
        // (userId|courseId|date) → [eventCount, completedCount] 일별 롤업 누적
        Map<String, int[]> dailyAgg = new LinkedHashMap<>();
        Map<String, Object[]> dailyKey = new HashMap<>();

        for (int i = 0; i < users.size(); i++) {
            Long userId = users.get(i).getId();
            Course course = courses.get(i % courses.size()); // 강좌당 여러 수강생 분포
            Long courseId = course.getId();
            List<Lecture> lectures = lecturesByCourse.getOrDefault(courseId, List.of());
            if (lectures.isEmpty()) continue;

            int watchCount = 1 + (i % Math.min(lectures.size(), LECTURES_PER_LEARNER));
            int completedCount = switch (i % 3) {
                case 0 -> watchCount;                    // 완강형
                case 1 -> Math.max(1, watchCount - 1);   // 거의 완강
                default -> Math.max(0, watchCount - 2);  // 초반 이탈형
            };
            int userOffset = i % SPREAD_DAYS; // 수강생마다 활동 시작일을 흩뿌림

            for (int j = 0; j < watchCount; j++) {
                Lecture lecture = lectures.get(j);
                Long chapterId = lecture.getChapter().getId();
                int duration = lecture.getDurationSeconds();
                boolean completed = j < completedCount;
                LocalDate date = today.minusDays(userOffset + (watchCount - j));
                LocalDateTime base = date.atTime(20, 0).plusMinutes(j);

                int watched = completed ? duration : Math.max(60, duration * 6 / 10);
                int rate = completed ? 100 : Math.min(89, watched * 100 / Math.max(1, duration));
                progresses.add(new LectureProgress(
                        null, lecture.getId(), userId, watched, watched, rate,
                        completed, completed ? base.plusSeconds(duration) : null, base, base));

                String k = "evt-seed-" + userId + "-" + lecture.getId() + "-";
                int evCount = 2; // ENTER + EXIT 는 항상
                events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                        LearningEventType.LECTURE_ENTER, 0, base, k + "enter"));
                events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                        LearningEventType.VIDEO_PAUSE, watched, base.plusSeconds(2), k + "pause"));
                evCount++;
                if (completed) {
                    events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                            LearningEventType.LECTURE_COMPLETE, duration, base.plusSeconds(duration), k + "complete"));
                    evCount++;
                }
                events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                        LearningEventType.LECTURE_EXIT, watched, base.plusSeconds(watched), k + "exit"));

                String dk = userId + "|" + courseId + "|" + date;
                int[] agg = dailyAgg.computeIfAbsent(dk, x -> new int[2]);
                agg[0] += evCount;
                agg[1] += completed ? 1 : 0;
                dailyKey.putIfAbsent(dk, new Object[]{userId, courseId, date});
            }

            attendances.add(Attendance.record(userId, today, Optional.empty(), 0, now));
            reflections.add(LectureReflection.create(userId, lectures.get(0).getId(), "학습 회고 " + (i + 1)));
        }

        List<StudyDailyStat> dailyStats = new ArrayList<>();
        for (Map.Entry<String, int[]> e : dailyAgg.entrySet()) {
            Object[] kv = dailyKey.get(e.getKey());
            dailyStats.add(StudyDailyStat.of(
                    (Long) kv[0], (Long) kv[1], (LocalDate) kv[2], e.getValue()[0], e.getValue()[1]));
        }

        lectureProgressRepository.saveAll(progresses);
        learningEventRepository.saveAll(events);
        studyDailyStatRepository.saveAll(dailyStats);
        attendanceRepository.saveAll(attendances);
        lectureReflectionRepository.saveAll(reflections);

        log.info("[DataInit] 학습 연쇄 데이터 — 진행 {}건, 이벤트 {}건, 일별롤업 {}건",
                progresses.size(), events.size(), dailyStats.size());
    }

    /** Q&A 질문/답변 + 강의 수정 요청 */
    private void seedQna(List<User> users, CatalogData catalog) {
        List<Course> courses = catalog.courses();
        Map<Long, Lecture> sample = catalog.sampleLectureByCourse();
        int count = Math.min(users.size(), courses.size());

        List<QnaQuestion> questions = new ArrayList<>();
        List<LectureModificationRequest> modRequests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Course course = courses.get(i);
            Lecture lecture = sample.get(course.getId());
            String mockUuid = UUID.randomUUID().toString();

            questions.add(QnaQuestion.create(users.get(i).getId(), lecture.getId(), "질문 " + (i + 1), "질문 내용 " + (i + 1)));
            modRequests.add(new LectureModificationRequest(
                    null, lecture, course.getInstructorId(), "영상 교체 요청 " + (i + 1),
                    lecture.getM3u8Path(), "/hls/new/" + mockUuid + "/index.m3u8", mockUuid, RequestStatus.PENDING, null, null));
        }
        List<QnaQuestion> savedQuestions = qnaQuestionRepository.saveAll(questions);

        List<QnaAnswer> answers = new ArrayList<>();
        for (int i = 0; i < savedQuestions.size(); i++) {
            answers.add(QnaAnswer.create(savedQuestions.get(i).getId(), courses.get(i).getInstructorId(), "답변 내용 " + (i + 1)));
        }
        qnaAnswerRepository.saveAll(answers);
        lectureModificationRequestRepository.saveAll(modRequests);

        log.info("[DataInit] Q&A/수정요청 데이터 생성 ({}건)", count);
    }

    /** 스터디 / 멤버 / 활동 / 리포트 / AI 피드백 (강사 1명당 1건) */
    private void seedStudies(List<User> sellers, List<User> users, CatalogData catalog) {
        List<Course> courses = catalog.courses();
        int coursesPerSeller = courses.size() / Math.max(1, sellers.size());
        for (int s = 0; s < sellers.size(); s++) {
            User seller = sellers.get(s);
            Course course = courses.get(s * coursesPerSeller);

            Study study = studyRepository.save(Study.createForCourse(seller, course, "스터디 " + (s + 1), "스터디 설명"));
            study.activate();
            studyMemberRepository.save(StudyMember.owner(seller, study));
            StudyActivity activity = studyActivityRepository.save(
                    StudyActivity.create(study.getId(), seller.getId(), "활동 내용 " + (s + 1)));
            StudyReport report = StudyReport.create(users.get(s % users.size()).getId(), study.getId());
            report.update(3600, 5, 8, 10, 10,
                    "[{\"lectureId\":1,\"title\":\"Spring Core\",\"watchTimeSeconds\":1800}]",
                    "[{\"date\":\"2026-06-07\",\"progressRate\":40.00},{\"date\":\"2026-06-14\",\"progressRate\":80.00}]",
                    "{\"2026-06-07\":3,\"2026-06-08\":5,\"2026-06-14\":2}");
            studyReportRepository.save(report);
            aiFeedbackRepository.save(AiFeedback.startProcessing(
                    seller.getId(), study.getId(), activity.getId(), "활동 스냅샷", "claude-opus-4-8", "v1"));
        }
        log.info("[DataInit] 스터디 데이터 생성 ({}건)", sellers.size());
    }

    // =========================================================================
    // 데모 전용 시나리오 보강 (DemoDataInitializer 에서만 호출)
    // -------------------------------------------------------------------------
    // seed() 가 만든 "단일 상태" 데이터를, 기능/정책 요구사항이 시연에서 드러나도록
    // 다양한 상태·흐름으로 펼친다. simple/bulk 는 호출하지 않으므로 영향 없음.
    // 강좌-수강 인덱스 매핑(seedCommerce): user[i] ↔ course[i]
    // 스터디-강좌 매핑(seedStudies): study[s] ↔ course[s*2]  (강좌 10개 → 스터디 5개)
    // =========================================================================
    @Transactional
    public void seedDemoScenarios() {
        Map<Long, User> usersById = new HashMap<>();
        userRepository.findAll().forEach(u -> usersById.put(u.getId(), u));
        Long adminId = userRepository.findByEmail("admin@test.com").orElseThrow().getId();

        List<Course> courses = courseRepository.findAll();
        courses.sort(Comparator.comparing(Course::getId));
        List<Study> studies = studyRepository.findAll();
        studies.sort(Comparator.comparing(Study::getId));
        List<User> learners = usersById.values().stream()
                .filter(u -> u.getRole() == UserRole.ROLE_USER)
                .sorted(Comparator.comparing(User::getId))
                .toList();

        if (courses.size() < 10 || studies.size() < 5 || learners.size() < 11) {
            log.warn("[DataInit] 데모 시나리오 보강 스킵 (데이터 부족: 강좌 {}, 스터디 {}, 수강생 {})",
                    courses.size(), studies.size(), learners.size());
            return;
        }

        demoCourseLifecycle(courses, adminId);
        demoStudyStates(courses, studies, usersById);
        demoRefund(courses, learners);
        demoLearningActivityFeed(studies, learners);
        demoAiFeedback(studies, learners);
        demoLearningFlow(courses, learners);
        demoQna(courses, learners);
        demoLectureModificationRequests(courses, adminId);
        demoAttendanceStreak(learners);
        demoCoupons();
        demoReports(studies);
        demoBulkEnrollStudyCourses(courses, studies, learners);

        log.info("[DataInit] 데모 시나리오 보강 완료");
    }

    /**
     * 데모: ACTIVE/READONLY 스터디의 강좌에 수강생을 추가로 등록해 스터디 멤버 수를 늘린다.
     * <p>
     * 멤버는 {@code linkLearnersAsStudyMembers}(DemoDataInitializer)가 "ACTIVE 수강생 → 멤버"로
     * 연결하므로, 멤버를 늘리려면 해당 강좌의 수강 등록을 늘려야 한다. seedCommerce 와 동일하게
     * 주문(PAID)·결제·수강(ACTIVE)을 함께 생성해 일관성을 유지한다.
     * (user, course) 유니크 제약이 있어 기존 수강 조합은 건너뛴다.
     */
    private void demoBulkEnrollStudyCourses(List<Course> courses, List<Study> studies, List<User> learners) {
        final int perCourse = 12; // 스터디(강좌)당 추가 수강생 목표 수

        LocalDateTime now = LocalDateTime.now();
        Map<Long, Course> courseById = new HashMap<>();
        for (Course c : courses) courseById.put(c.getId(), c);

        // 이미 존재하는 (userId:courseId) 수강 조합 — 유니크 충돌/중복 방지
        Set<String> existing = new HashSet<>();
        enrollmentRepository.findAll().forEach(e -> existing.add(e.getUserId() + ":" + e.getCourseId()));

        List<Long> targetCourseIds = studies.stream()
                .filter(s -> s.getStatus() == StudyStatus.ACTIVE || s.getStatus() == StudyStatus.READONLY)
                .map(s -> s.getCourse().getId())
                .distinct()
                .toList();

        List<Order> orders = new ArrayList<>();
        List<Payment> payments = new ArrayList<>();
        List<Enrollment> enrollments = new ArrayList<>();
        int seq = 0;

        for (Long courseId : targetCourseIds) {
            Course course = courseById.get(courseId);
            if (course == null) continue;
            int added = 0;
            for (User learner : learners) {
                if (added >= perCourse) break;
                String key = learner.getId() + ":" + courseId;
                if (existing.contains(key)) continue;

                Order order = Order.createPendingPayment(
                        learner.getId(), "ORD-DEMO-MEM-" + courseId + "-" + learner.getId(), now);
                order.addItem(course.getId(), course.getTitle(), course.getPrice(), now);
                order.markPaid(now);
                orders.add(order);

                Payment payment = Payment.createReady(order, now);
                payment.markProcessing(now);
                payment.succeed("PAY-DEMO-MEM-" + (seq++), "CARD", now);
                payments.add(payment);

                enrollments.add(Enrollment.createActive(learner.getId(), courseId, order, now));
                existing.add(key);
                added++;
            }
        }

        orderRepository.saveAll(orders);
        paymentRepository.saveAll(payments);
        enrollmentRepository.saveAll(enrollments);
        log.info("[DataInit] 데모 스터디 멤버 보강용 수강 {}건 생성 (대상 강좌 {}개)",
                enrollments.size(), targetCourseIds.size());
    }

    // =========================================================================
    // demo 모드 전용: "의미있는 이름" 페르소나로 엣지/예외 케이스를 또렷이 보여주는 쇼케이스.
    // 파라미터 기반 seed() 와 별개로, 적은 수의 계정을 손으로 구성한다. (비밀번호 모두 Test1234!)
    // =========================================================================
    @Transactional
    public boolean seedPersonaShowcase() {
        if (userRepository.count() > 0) {
            log.info("[DataInit] 이미 데이터가 존재해 페르소나 쇼케이스를 건너뜀");
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        String pw = passwordEncoder.encode("Test1234!");

        User admin = userRepository.save(User.createAdmin("admin@test.com", pw, "관리자", null));

        List<Category> cats = saveCategories(); // 백엔드/프론트엔드/DevOps/UI/UX/마케팅
        Long catBackend = cats.get(0).getId();
        Long catFront = cats.get(1).getId();
        Long catDevops = cats.get(2).getId();
        Long catUiux = cats.get(3).getId();
        Long catMkt = cats.get(4).getId();

        // ── 판매자 + 강좌(생명주기 상태) ──────────────────────────────────────
        List<CourseStatusHistory> hist = new ArrayList<>();

        User sellerPopular = saveShowcaseSeller("seller1@test.com", "인기강사", pw);
        Course coursePopular = buildShowcaseCourse(sellerPopular, catBackend, "스프링 부트 실전", 30000);
        hist.add(coursePopular.requestReview(sellerPopular.getId()));
        hist.add(coursePopular.approve(admin.getId())); // ON_SALE

        User sellerPending = saveShowcaseSeller("seller2@test.com", "승인대기판매자", pw);
        Course coursePending = buildShowcaseCourse(sellerPending, catFront, "React 입문", 20000);
        hist.add(coursePending.requestReview(sellerPending.getId())); // IN_REVIEW(승인 대기)

        User sellerRejected = saveShowcaseSeller("seller3@test.com", "반려당한판매자", pw);
        Course courseRejected = buildShowcaseCourse(sellerRejected, catDevops, "도커 기초", 15000);
        hist.add(courseRejected.requestReview(sellerRejected.getId()));
        hist.add(courseRejected.reject(admin.getId(), "커리큘럼 구성이 미흡하여 반려합니다.")); // SUSPENDED(반려)

        User sellerClosed = saveShowcaseSeller("seller4@test.com", "판매중지판매자", pw);
        Course courseClosed = buildShowcaseCourse(sellerClosed, catUiux, "피그마 마스터", 25000);
        hist.add(courseClosed.requestReview(sellerClosed.getId()));
        hist.add(courseClosed.approve(admin.getId()));
        hist.add(courseClosed.close(sellerClosed.getId())); // SUSPENDED(판매중지)

        User sellerDraft = saveShowcaseSeller("seller5@test.com", "초안작성자", pw);
        Course courseDraft = buildShowcaseCourse(sellerDraft, catMkt, "퍼포먼스 마케팅", 18000); // DRAFT 유지

        Course courseJpa = buildShowcaseCourse(sellerPopular, catBackend, "JPA 성능 튜닝", 35000);
        hist.add(courseJpa.requestReview(sellerPopular.getId()));
        hist.add(courseJpa.approve(admin.getId()));

        Course courseTypeScript = buildShowcaseCourse(sellerPending, catFront, "TypeScript 실무", 28000);
        hist.add(courseTypeScript.requestReview(sellerPending.getId()));
        hist.add(courseTypeScript.approve(admin.getId()));

        Course courseKubernetes = buildShowcaseCourse(sellerRejected, catDevops, "쿠버네티스 운영", 42000);
        hist.add(courseKubernetes.requestReview(sellerRejected.getId()));
        hist.add(courseKubernetes.approve(admin.getId()));

        Course courseDesignSystem = buildShowcaseCourse(sellerClosed, catUiux, "디자인 시스템 구축", 32000);
        hist.add(courseDesignSystem.requestReview(sellerClosed.getId()));
        hist.add(courseDesignSystem.approve(admin.getId()));

        Course courseGrowth = buildShowcaseCourse(sellerDraft, catMkt, "그로스 마케팅 분석", 26000);
        hist.add(courseGrowth.requestReview(sellerDraft.getId()));
        hist.add(courseGrowth.approve(admin.getId()));

        List<Course> showcaseCourses = List.of(
                coursePopular, coursePending, courseRejected, courseClosed, courseDraft,
                courseJpa, courseTypeScript, courseKubernetes, courseDesignSystem, courseGrowth);

        courseStatusHistoryRepository.saveAll(hist);

        // ── 스터디: 인기강좌(ACTIVE) + 마감강좌(READONLY) ──────────────────────
        Study activeStudy = studyRepository.save(
                Study.createForCourse(sellerPopular, coursePopular, "스프링 실전 스터디", "함께 완주해요"));
        activeStudy.activate();
        studyMemberRepository.save(StudyMember.owner(sellerPopular, activeStudy));

        Study readonlyStudy = studyRepository.save(
                Study.createForCourse(sellerClosed, courseClosed, "마감된 스터디", "기록 열람만 가능"));
        readonlyStudy.activate();
        readonlyStudy.changeToReadOnly(); // ACTIVE → READONLY
        studyMemberRepository.save(StudyMember.owner(sellerClosed, readonlyStudy));

        List<Study> showcaseStudies = new ArrayList<>(List.of(activeStudy, readonlyStudy));
        List<User> extraStudyOwners = List.of(sellerPopular, sellerPending, sellerRejected, sellerClosed, sellerDraft);
        for (Course course : List.of(courseJpa, courseTypeScript, courseKubernetes, courseDesignSystem, courseGrowth)) {
            User owner = extraStudyOwners.stream()
                    .filter(seller -> seller.getId().equals(course.getInstructorId()))
                    .findFirst()
                    .orElse(sellerPopular);
            Study study = studyRepository.save(Study.createForCourse(
                    owner, course, course.getTitle() + " 스터디", "6개월 학습 기록이 쌓인 스터디"));
            study.activate();
            studyMemberRepository.save(StudyMember.owner(owner, study));
            showcaseStudies.add(study);
        }

        List<Lecture> popularLectures = lecturesOf(coursePopular);
        Lecture freeLecture = popularLectures.get(0); // 1장 1강 = 무료 미리보기
        Lecture firstPaidLecture = popularLectures.get(1);

        List<LectureProgress> progresses = new ArrayList<>();
        List<LearningEvent> events = new ArrayList<>();

        // ── 수강생 페르소나 ──────────────────────────────────────────────────
        // 완강수강생: 인기강좌 수강 + 전 강의 100% + LECTURE_COMPLETE
        User learnerComplete = saveLearner("user1@test.com", "완강수강생", pw);
        enroll(learnerComplete, coursePopular, now, "COMPLETE");
        for (Lecture lec : popularLectures) {
            progresses.add(completedProgress(learnerComplete.getId(), lec, now));
            events.add(LearningEvent.create(learnerComplete.getId(), coursePopular.getId(),
                    lec.getChapter().getId(), lec.getId(), LearningEventType.LECTURE_COMPLETE,
                    lec.getDurationSeconds(), now, "evt-complete-" + lec.getId()));
        }

        // 진행중수강생: 수강 + 일부 진행
        User learnerInProgress = saveLearner("learner-inprogress@test.com", "진행중수강생", pw);
        enroll(learnerInProgress, coursePopular, now, "INPROG");
        LectureProgress partial = LectureProgress.start(learnerInProgress.getId(), firstPaidLecture.getId(), 0, now);
        partial.applyProgress(firstPaidLecture.getDurationSeconds() / 2,
                firstPaidLecture.getDurationSeconds() / 2, firstPaidLecture.getDurationSeconds(), now);
        progresses.add(partial);
        events.add(LearningEvent.create(learnerInProgress.getId(), coursePopular.getId(),
                firstPaidLecture.getChapter().getId(), firstPaidLecture.getId(),
                LearningEventType.LECTURE_ENTER, 0, now, "evt-inprog-enter"));

        // 환불수강생: 수강 후 환불 → CANCELED (스터디 멤버에서 자연 제외)
        User learnerRefund = saveLearner("learner-refunded@test.com", "환불수강생", pw);
        Enrollment refundEnrollment = enroll(learnerRefund, coursePopular, now, "REFUND");
        refundEnrollment.cancel(now);

        // 맛보기시청자: 미구매·미수강. 무료 미리보기 강의 시청 이벤트만 존재
        User learnerPreview = saveLearner("learner-preview@test.com", "맛보기시청자", pw);
        events.add(LearningEvent.create(learnerPreview.getId(), coursePopular.getId(),
                freeLecture.getChapter().getId(), freeLecture.getId(),
                LearningEventType.VIDEO_PAUSE, freeLecture.getDurationSeconds(), now, "evt-preview-pause"));

        // 회고작성자: 수강 + 회고
        User learnerReflection = saveLearner("learner-reflection@test.com", "회고작성자", pw);
        enroll(learnerReflection, coursePopular, now, "REFLECT");
        lectureReflectionRepository.save(LectureReflection.create(
                learnerReflection.getId(), firstPaidLecture.getId(),
                "오늘 영속성 개념을 확실히 잡았다. 다음엔 트랜잭션을 복습하자."));

        // 개근수강생: 수강 + 3일 연속 출석
        User learnerAttendance = saveLearner("learner-attendance@test.com", "개근수강생", pw);
        enroll(learnerAttendance, coursePopular, now, "ATTEND");
        LocalDate today = LocalDate.now();
        Attendance a1 = Attendance.record(learnerAttendance.getId(), today.minusDays(2), Optional.empty(), 0, now);
        Attendance a2 = Attendance.record(learnerAttendance.getId(), today.minusDays(1), Optional.of(a1), 1, now);
        Attendance a3 = Attendance.record(learnerAttendance.getId(), today, Optional.of(a2), 2, now);
        attendanceRepository.saveAll(List.of(a1, a2, a3));

        // 읽기전용스터디원: 마감(READONLY) 스터디 강좌 수강 → 멤버로 연결됨(작성 불가 확인용)
        User learnerReadonly = saveLearner("learner-readonly@test.com", "읽기전용스터디원", pw);
        enroll(learnerReadonly, courseClosed, now, "READONLY");

        lectureProgressRepository.saveAll(progresses);
        learningEventRepository.saveAll(events);

        // 질문많은수강생: 수강 + QnA(답변된 질문 + 미답변 질문)
        User learnerQna = saveLearner("learner-qna@test.com", "질문많은수강생", pw);
        enroll(learnerQna, coursePopular, now, "QNA");
        QnaQuestion answered = qnaQuestionRepository.save(QnaQuestion.create(
                learnerQna.getId(), firstPaidLecture.getId(), "영속성 컨텍스트가 뭔가요?",
                "1차 캐시랑 같은 건가요?"));
        qnaAnswerRepository.save(QnaAnswer.create(
                answered.getId(), sellerPopular.getId(), "네, 1차 캐시를 포함한 더 큰 개념입니다."));
        qnaQuestionRepository.save(QnaQuestion.create(
                learnerQna.getId(), freeLecture.getId(), "예제 코드는 어디서 받나요?",
                "깃허브 링크가 있을까요?")); // 미답변

        // 쿠폰수집가: 사용/만료/보유 쿠폰
        User learnerCoupon = saveLearner("learner-coupon@test.com", "쿠폰수집가", pw);
        CouponPolicy welcome = couponPolicyRepository.save(newDemoPolicy("신규 환영 10% 쿠폰", coursePopular.getId(), now));
        CouponPolicy weekly = couponPolicyRepository.save(newDemoPolicy("주간 특가 10% 쿠폰", coursePopular.getId(), now));
        CouponPolicy ending = couponPolicyRepository.save(newDemoPolicy("만료 임박 10% 쿠폰", coursePopular.getId(), now));
        IssuedCoupon usedCoupon = IssuedCoupon.create(welcome, learnerCoupon.getId(), now);
        IssuedCoupon expiredCoupon = IssuedCoupon.create(weekly, learnerCoupon.getId(), now);
        IssuedCoupon activeCoupon = IssuedCoupon.create(ending, learnerCoupon.getId(), now);
        usedCoupon.use(now);
        expiredCoupon.expire();
        issuedCouponRepository.saveAll(List.of(usedCoupon, expiredCoupon, activeCoupon));

        seedSixMonthDemoUsage(showcaseCourses, showcaseStudies, List.of(
                learnerComplete, learnerInProgress, learnerRefund, learnerPreview, learnerReflection,
                learnerAttendance, learnerReadonly, learnerQna, learnerCoupon), now);

        log.info("[DataInit] 페르소나 쇼케이스 생성 완료 (판매자 5 · 강좌 10 · 수강생 9 · 관리자 1)");
        return true;
    }

    // ── 페르소나 쇼케이스 헬퍼 ────────────────────────────────────────────────
    private User saveShowcaseSeller(String email, String nickname, String pw) {
        User user = userRepository.save(User.createSeller(email, pw, nickname, null));
        sellerRepository.save(new Seller(null, user.getId(), LocalDateTime.now(), LocalDateTime.now()));
        return user;
    }

    private User saveLearner(String email, String nickname, String pw) {
        return userRepository.save(User.createUser(email, pw, nickname, null));
    }

    /** 챕터 2개 × 강의 2개(1강은 무료 미리보기)를 가진 강좌를 만들어 저장한다. */
    private Course buildShowcaseCourse(User seller, Long categoryId, String title, int price) {
        Course course = Course.createDraft(seller.getId(), categoryId, title,
                title + " 강좌입니다.",
//                "thumb-demo.jpg",
                "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800&auto=format&fit=crop",
                price);
        for (int ch = 1; ch <= 2; ch++) {
            Chapter chapter = Chapter.create(ch + "장", ch, course);
            course.addChapter(chapter);
            for (int j = 1; j <= 2; j++) {
                String uuid = UUID.randomUUID().toString();
                chapter.addLecture(Lecture.createWithStream(
                        "/hls/demo/" + uuid + "/index.m3u8", uuid,
                        ch + "장 " + j + "강", "강의 요약", 600 * j, j, j == 1, chapter));
            }
        }
        return courseRepository.save(course); // chapters/lectures cascade
    }

    private List<Lecture> lecturesOf(Course course) {
        List<Lecture> result = new ArrayList<>();
        course.getChapters().forEach(c -> result.addAll(c.getLectures()));
        return result;
    }

    /** 주문(PAID)·결제(SUCCESS)·수강(ACTIVE)을 한 번에 생성한다. */
    private Enrollment enroll(User learner, Course course, LocalDateTime now, String tag) {
        Order order = Order.createPendingPayment(
                learner.getId(), "ORD-DEMO-" + tag + "-" + learner.getId(), now);
        order.addItem(course.getId(), course.getTitle(), course.getPrice(), now);
        order.markPaid(now);
        orderRepository.save(order);

        Payment payment = Payment.createReady(order, now);
        payment.markProcessing(now);
        payment.succeed("PAY-DEMO-" + tag + "-" + learner.getId(), "CARD", now);
        paymentRepository.save(payment);

        return enrollmentRepository.save(Enrollment.createActive(learner.getId(), course.getId(), order, now));
    }

    private LectureProgress completedProgress(Long userId, Lecture lec, LocalDateTime now) {
        LectureProgress p = LectureProgress.start(userId, lec.getId(), 0, now);
        p.applyProgress(lec.getDurationSeconds(), lec.getDurationSeconds(), lec.getDurationSeconds(), now);
        return p;
    }

    private CouponPolicy newDemoPolicy(String name, Long courseId, LocalDateTime now) {
        return CouponPolicy.createPolicy(
                name, CouponTarget.COURSE, CouponType.AUTO, 100, CouponUsageType.SINGLE_USE, false,
                DiscountType.PERCENT, 10, 5000, null, 30,
                now.minusDays(1), now.plusDays(30), null, List.of(courseId));
    }

    /**
     * 6개월 사용 흔적: 수강생마다 3~5개 강좌를 듣고, 강의별 시청 이벤트/진행도/일별롤업/리포트를 남긴다.
     * 쇼케이스의 단일 엣지 케이스 위에 실제 서비스처럼 누적된 데이터를 얹는 데모 전용 보강이다.
     */
    private void seedSixMonthDemoUsage(List<Course> courses, List<Study> studies, List<User> learners, LocalDateTime now) {
        List<Course> learnableCourses = courses.stream()
                .filter(course -> course.getStatus() == CourseStatus.ON_SALE || course.getId().equals(studies.get(1).getCourse().getId()))
                .toList();
        Map<Long, Study> studyByCourseId = new HashMap<>();
        for (Study study : studies) {
            studyByCourseId.put(study.getCourse().getId(), study);
        }

        Set<String> existingEnrollments = new HashSet<>();
        Set<String> activeEnrollments = new HashSet<>();
        enrollmentRepository.findAll().forEach(e -> {
            String key = e.getUserId() + ":" + e.getCourseId();
            existingEnrollments.add(key);
            if (e.getStatus() == EnrollmentStatus.ACTIVE) {
                activeEnrollments.add(key);
            }
        });

        Set<String> existingProgresses = new HashSet<>();
        lectureProgressRepository.findAll().forEach(p -> existingProgresses.add(p.getUserId() + ":" + p.getLectureId()));

        Set<String> existingReflections = new HashSet<>();
        lectureReflectionRepository.findAll()
                .forEach(r -> existingReflections.add(r.getUserId() + ":" + r.getLectureId()));

        List<LectureProgress> progresses = new ArrayList<>();
        List<LearningEvent> events = new ArrayList<>();
        List<LectureReflection> reflections = new ArrayList<>();
        List<Attendance> attendances = new ArrayList<>();
        List<StudyDailyStat> dailyStats = new ArrayList<>();
        List<StudyReport> reports = new ArrayList<>();
        Map<String, int[]> dailyAgg = new LinkedHashMap<>();

        LocalDate startDate = now.toLocalDate().minusMonths(6).plusDays(3);
        int coursePool = learnableCourses.size();
        for (int userIndex = 0; userIndex < learners.size(); userIndex++) {
            User learner = learners.get(userIndex);
            int targetCourseCount = 3 + (userIndex % 3); // 3~5개
            int enrolledCourseCount = 0;

            for (int offset = 0; offset < coursePool && enrolledCourseCount < targetCourseCount; offset++) {
                Course course = learnableCourses.get((userIndex + offset * 2) % coursePool);
                String enrollmentKey = learner.getId() + ":" + course.getId();
                if (existingEnrollments.contains(enrollmentKey)) {
                    continue;
                }

                enroll(learner, course, now.minusMonths(6).plusDays(userIndex * 3L + offset), "6M-" + userIndex + "-" + offset);
                existingEnrollments.add(enrollmentKey);
                activeEnrollments.add(enrollmentKey);
                enrolledCourseCount++;
            }

            List<Course> learnerCourses = learnableCourses.stream()
                    .filter(course -> activeEnrollments.contains(learner.getId() + ":" + course.getId()))
                    .limit(targetCourseCount)
                    .toList();

            Attendance previous = null;
            int attendanceSeq = 0;
            for (int month = 5; month >= 0; month--) {
                LocalDate date = now.toLocalDate().minusMonths(month).withDayOfMonth(Math.min(7 + userIndex, 24));
                Attendance attendance = Attendance.record(
                        learner.getId(), date, Optional.ofNullable(previous), attendanceSeq, date.atTime(8, 30));
                attendances.add(attendance);
                previous = attendance;
                attendanceSeq++;
            }

            for (int courseIndex = 0; courseIndex < learnerCourses.size(); courseIndex++) {
                Course course = learnerCourses.get(courseIndex);
                List<Lecture> lectures = orderedLectures(course);
                if (lectures.isEmpty()) continue;

                int completedLectures = 1 + ((userIndex + courseIndex) % lectures.size());
                int eventCount = 0;
                int completedCount = 0;
                int totalWatchSeconds = 0;

                for (int lectureIndex = 0; lectureIndex < lectures.size(); lectureIndex++) {
                    Lecture lecture = lectures.get(lectureIndex);
                    boolean complete = lectureIndex < completedLectures;
                    int watchRate = complete ? 100 : 35 + ((userIndex + lectureIndex) % 4) * 10;
                    int watchedSeconds = lecture.getDurationSeconds() * watchRate / 100;
                    LocalDateTime watchedAt = startDate
                            .plusDays((long) courseIndex * 23 + (long) lectureIndex * 11 + userIndex)
                            .atTime(19, 0)
                            .plusMinutes(lectureIndex * 12L);

                    String progressKey = learner.getId() + ":" + lecture.getId();
                    if (!existingProgresses.contains(progressKey)) {
                        progresses.add(new LectureProgress(
                                null, lecture.getId(), learner.getId(), watchedSeconds, watchedSeconds,
                                watchRate, complete, complete ? watchedAt.plusMinutes(9) : null, watchedAt, watchedAt));
                        existingProgresses.add(progressKey);
                    }

                    String reflectionKey = learner.getId() + ":" + lecture.getId();
                    if (complete && lectureIndex % 2 == 0 && !existingReflections.contains(reflectionKey)) {
                        reflections.add(LectureReflection.create(
                                learner.getId(), lecture.getId(), sixMonthReflection(courseIndex, lectureIndex)));
                        existingReflections.add(reflectionKey);
                    }

                    eventCount += appendSixMonthWatchEvents(events, learner.getId(), lecture, watchedAt, complete, watchedSeconds);
                    completedCount += complete ? 1 : 0;
                    totalWatchSeconds += watchedSeconds;

                    String dailyKey = learner.getId() + "|" + course.getId() + "|" + watchedAt.toLocalDate();
                    int[] daily = dailyAgg.computeIfAbsent(dailyKey, ignored -> new int[2]);
                    daily[0] += complete ? 6 : 5;
                    daily[1] += complete ? 1 : 0;
                }

                Study study = studyByCourseId.get(course.getId());
                if (study != null) {
                    reports.add(sixMonthReport(
                            learner.getId(), study.getId(), lectures, totalWatchSeconds, courseIndex,
                            completedCount, lectures.size(), eventCount, startDate.plusDays((long) userIndex + courseIndex * 17L)));
                }
            }
        }

        for (Map.Entry<String, int[]> entry : dailyAgg.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            int[] value = entry.getValue();
            dailyStats.add(StudyDailyStat.of(
                    Long.valueOf(parts[0]), Long.valueOf(parts[1]), LocalDate.parse(parts[2]), value[0], value[1]));
        }

        lectureProgressRepository.saveAll(progresses);
        learningEventRepository.saveAll(events);
        lectureReflectionRepository.saveAll(reflections);
        attendanceRepository.saveAll(attendances);
        studyDailyStatRepository.saveAll(dailyStats);
        studyReportRepository.saveAll(reports);

        log.info("[DataInit] 6개월 데모 사용 흔적 생성 — 진행 {}건, 이벤트 {}건, 출석 {}건, 일별롤업 {}건, 리포트 {}건",
                progresses.size(), events.size(), attendances.size(), dailyStats.size(), reports.size());
    }

    private int appendSixMonthWatchEvents(List<LearningEvent> out, Long userId, Lecture lec,
                                          LocalDateTime base, boolean complete, int watchedSeconds) {
        Long courseId = lec.getChapter().getCourse().getId();
        Long chapterId = lec.getChapter().getId();
        Long lecId = lec.getId();
        int duration = lec.getDurationSeconds();
        String key = "sixm-" + userId + "-" + lecId + "-" + base.toLocalDate() + "-";

        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_ENTER, 0, base, key + "enter"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.VIDEO_PAUSE, watchedSeconds, base.plusMinutes(8), key + "pause"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_EXIT, watchedSeconds, base.plusMinutes(9), key + "exit"));
        if (complete) {
            out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_COMPLETE, duration, base.plusMinutes(9).plusSeconds(1), key + "complete"));
            return 6;
        }
        return 5;
    }

    private StudyReport sixMonthReport(Long userId, Long studyId, List<Lecture> lectures, int totalWatchSeconds,
                                       int qnaSeed, int completedLectures, int totalLectures, int eventCount,
                                       LocalDate firstDate) {
        StudyReport report = StudyReport.create(userId, studyId);
        report.update(
                totalWatchSeconds,
                1 + (qnaSeed % 4),
                completedLectures,
                totalLectures,
                6,
                topLecturesJson(lectures),
                sixMonthDailyProgressJson(firstDate, completedLectures, totalLectures),
                sixMonthActivityMapJson(firstDate, eventCount));
        return report;
    }

    private String topLecturesJson(List<Lecture> lectures) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < Math.min(3, lectures.size()); i++) {
            Lecture lecture = lectures.get(i);
            if (i > 0) json.append(",");
            json.append("{\"lectureId\":").append(lecture.getId())
                    .append(",\"title\":\"").append(lecture.getTitle())
                    .append("\",\"watchTimeSeconds\":").append(lecture.getDurationSeconds())
                    .append("}");
        }
        return json.append("]").toString();
    }

    private String sixMonthDailyProgressJson(LocalDate firstDate, int completedLectures, int totalLectures) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 6; i++) {
            if (i > 0) json.append(",");
            int cumulative = Math.min(completedLectures, Math.max(1, i * completedLectures / 5));
            double rate = totalLectures == 0 ? 0.0 : cumulative * 100.0 / totalLectures;
            json.append("{\"date\":\"").append(firstDate.plusMonths(i))
                    .append("\",\"progressRate\":").append(String.format(Locale.US, "%.2f", rate))
                    .append("}");
        }
        return json.append("]").toString();
    }

    private String sixMonthActivityMapJson(LocalDate firstDate, int eventCount) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < 6; i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(firstDate.plusMonths(i)).append("\":")
                    .append(Math.max(2, eventCount / 6 + (i % 3)));
        }
        return json.append("}").toString();
    }

    private String sixMonthReflection(int courseIndex, int lectureIndex) {
        String[] notes = {
                "이번 달에는 핵심 개념을 실제 프로젝트에 적용했다.",
                "헷갈렸던 부분을 다시 듣고 예제 코드를 정리했다.",
                "스터디 피드백을 반영해서 복습 루틴을 조정했다.",
                "강의 내용을 업무 시나리오에 맞춰 체크리스트로 만들었다."
        };
        return notes[(courseIndex + lectureIndex) % notes.length];
    }

    /** 강좌 생명주기: ON_SALE 5 / IN_REVIEW 1 / SUSPENDED(판매중지) 2 / SUSPENDED(반려) 1 / DRAFT 1 */
    private void demoCourseLifecycle(List<Course> courses, Long adminId) {
        List<CourseStatusHistory> histories = new ArrayList<>();

        // 0~4: 심사요청 → 승인 → ON_SALE
        for (int i = 0; i <= 4; i++) {
            Course c = courses.get(i);
            histories.add(c.requestReview(c.getInstructorId()));
            histories.add(c.approve(adminId));
        }
        // 5: 심사 중(IN_REVIEW)
        Course inReview = courses.get(5);
        histories.add(inReview.requestReview(inReview.getInstructorId()));
        // 6, 8: ON_SALE 후 판매자 판매중지(close) → SUSPENDED
        for (int i : List.of(6, 8)) {
            Course c = courses.get(i);
            histories.add(c.requestReview(c.getInstructorId()));
            histories.add(c.approve(adminId));
            histories.add(c.close(c.getInstructorId()));
        }
        // 7: 관리자 반려(reject) → SUSPENDED
        Course rejected = courses.get(7);
        histories.add(rejected.requestReview(rejected.getInstructorId()));
        histories.add(rejected.reject(adminId, "커리큘럼 구성이 미흡하여 반려합니다."));
        // 9: DRAFT 유지

        courseStatusHistoryRepository.saveAll(histories);
    }

    /** 스터디 상태: ACTIVE 3 / READONLY 2(판매중지 강좌) + 신규 INACTIVE 1(반려) / DRAFT 1 */
    private void demoStudyStates(List<Course> courses, List<Study> studies, Map<Long, User> usersById) {
        // study[3] ↔ course6, study[4] ↔ course8 : 판매중지 → READONLY
        studies.get(3).changeToReadOnly();
        studies.get(4).changeToReadOnly();
        // study[0,1,2] (course 0,2,4) 는 ACTIVE 유지

        // 신규: 반려 강좌(course7) → INACTIVE 스터디
        Course rejected = courses.get(7);
        User rOwner = usersById.get(rejected.getInstructorId());
        Study inactive = studyRepository.save(
                Study.createForCourse(rOwner, rejected, "반려된 스터디", "반려된 강좌에 연결된 스터디"));
        studyMemberRepository.save(StudyMember.owner(rOwner, inactive));
        inactive.changeToInactive(); // DRAFT → INACTIVE

        Set<Long> studyCourseIds = studies.stream()
                .map(study -> study.getCourse().getId())
                .collect(java.util.stream.Collectors.toSet());

        // 신규: DRAFT 강좌 → DRAFT 스터디 (생성자/관리자만 조회 가능)
        Course draftCourse = courses.stream()
                .filter(course -> course.getStatus() == CourseStatus.DRAFT)
                .filter(course -> !studyCourseIds.contains(course.getId()))
                .findFirst()
                .orElseThrow();
        User dOwner = usersById.get(draftCourse.getInstructorId());
        Study draft = studyRepository.save(
                Study.createForCourse(dOwner, draftCourse, "작성중 스터디", "DRAFT 강좌에 연결된 스터디"));
        studyMemberRepository.save(StudyMember.owner(dOwner, draft));
    }

    /** 환불 시나리오: user[4] 수강 취소(CANCELED) → ACTIVE 스터디 멤버 연결에서 제외된다. */
    private void demoRefund(List<Course> courses, List<User> learners) {
        Long courseId = courses.get(4).getId();
        Long userId = learners.get(4).getId();
        enrollmentRepository.findAll().stream()
                .filter(e -> courseId.equals(e.getCourseId()) && userId.equals(e.getUserId()))
                .findFirst()
                .ifPresent(e -> e.cancel(LocalDateTime.now()));
    }

    /** 학습 활동 피드: 멤버가 작성, 최신순 노출, 삭제 글은 제외. */
    private void demoLearningActivityFeed(List<Study> studies, List<User> learners) {
        Study active0 = studies.get(0); // course0, member = user[0]
        Long member0 = learners.get(0).getId();
        studyActivityRepository.save(StudyActivity.create(active0.getId(), member0, "1챕터 완강 후기: 핵심 개념을 정리했습니다."));
        studyActivityRepository.save(StudyActivity.create(active0.getId(), member0, "2챕터 정리: 개념 A 와 B 의 차이를 이해함."));
        StudyActivity deleted = studyActivityRepository.save(
                StudyActivity.create(active0.getId(), member0, "오타가 있어 삭제할 글입니다."));
        deleted.delete(); // 삭제된 학습 활동은 피드에 노출되지 않음

        Study active1 = studies.get(1); // course2, member = user[2]
        studyActivityRepository.save(
                StudyActivity.create(active1.getId(), learners.get(2).getId(), "QnA 답변 감사합니다. 덕분에 이해했어요."));
    }

    /** AI 코치: 피드백 상태 분산(COMPLETED/PROCESSING/FAILED/STALE) + 일일 3회 한도 도달 케이스. */
    private void demoAiFeedback(List<Study> studies, List<User> learners) {
        List<AiFeedback> seeded = aiFeedbackRepository.findAll();
        seeded.sort(Comparator.comparing(AiFeedback::getId));
        if (seeded.size() >= 5) {
            seeded.get(0).complete(feedbackJson(
                    "학습 기록이 구체적입니다.",
                    "핵심 개념을 자신의 언어로 정리한 점이 좋습니다.",
                    "개념 설명을 한 줄 더 보강해 보세요.",
                    "다음 학습에서 적용할 예시를 하나 메모해 보세요."));
            // seeded.get(1): PROCESSING 유지
            seeded.get(2).fail();                       // 실패 → 사용자가 재요청 가능
            seeded.get(3).complete(feedbackJson(
                    "좋은 회고입니다.",
                    "학습 과정을 솔직하게 돌아본 점이 인상적입니다.",
                    "다음에 시도할 구체적인 행동을 덧붙여 보세요.",
                    "배운 내용을 동료에게 한 번 설명해 보세요."));
            seeded.get(3).markStale();                  // COMPLETED → STALE
            // seeded.get(4): PROCESSING 유지
        }

        // 일일 3회 제한 데모: user[0] 가 같은 날 study0 활동에 3회 요청
        Study study0 = studies.get(0);
        Long user0 = learners.get(0).getId();
        List<AiFeedback> today = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            StudyActivity act = studyActivityRepository.save(
                    StudyActivity.create(study0.getId(), user0, "AI 코칭 요청용 활동 " + i));
            today.add(AiFeedback.startProcessing(
                    user0, study0.getId(), act.getId(), "활동 스냅샷 " + i, "claude-opus-4-8", "v1"));
        }
        today.get(0).complete(feedbackJson(
                "구체성이 좋습니다.",
                "활동 내용을 구체적으로 작성했습니다.",
                "근거나 출처를 함께 적으면 더 좋습니다.",
                "오늘 배운 내용을 짧게 요약해 보세요."));
        today.get(1).complete(feedbackJson(
                "잘 정리된 활동입니다.",
                "흐름이 명확하게 정리되어 있습니다.",
                "예시를 한 가지 추가해 보세요.",
                "관련 개념을 하나 더 찾아 연결해 보세요."));
        // today.get(2): PROCESSING — 오늘 3번째까지 사용(일일 한도 도달)
        aiFeedbackRepository.saveAll(today);
    }

    /**
     * 데모 AI 피드백을 실제 응답과 동일한 형식(StructuredFeedback JSON)으로 직렬화한다.
     * 과거 시드는 평문 문자열을 저장해 대시보드 조회 시 역직렬화 오류(500)를 유발했다.
     */
    private String feedbackJson(String summary, String strength, String improvement, String nextStep) {
        try {
            return new ObjectMapper().writeValueAsString(new StructuredFeedback(
                    summary, List.of(strength), List.of(improvement), List.of(nextStep)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("데모 AI 피드백 직렬화 실패", e);
        }
    }

    private List<CouponPolicy> autoRewardPolicies(LocalDateTime now) {
        return List.of(
                CouponPolicy.createPolicy(
                        "신규 가입 축하 쿠폰",
                        CouponTarget.ALL,
                        CouponType.AUTO,
                        AutoIssueType.SIGNUP,
                        null,
                        CouponUsageType.SINGLE_USE,
                        false,
                        DiscountType.AMOUNT,
                        1000,
                        null,
                        null,
                        30,
                        now.minusDays(1),
                        null,
                        null,
                        null
                ),
                CouponPolicy.createPolicy(
                        "연속 출석 보상 쿠폰",
                        CouponTarget.ALL,
                        CouponType.AUTO,
                        AutoIssueType.ATTENDANCE_STREAK,
                        null,
                        CouponUsageType.SINGLE_USE,
                        false,
                        DiscountType.AMOUNT,
                        1000,
                        null,
                        null,
                        30,
                        now.minusDays(1),
                        null,
                        null,
                        null
                ),
                CouponPolicy.createPolicy(
                        "월간 출석 보상 쿠폰",
                        CouponTarget.ALL,
                        CouponType.AUTO,
                        AutoIssueType.MONTHLY_ATTENDANCE,
                        null,
                        CouponUsageType.SINGLE_USE,
                        false,
                        DiscountType.AMOUNT,
                        1000,
                        null,
                        null,
                        30,
                        now.minusDays(1),
                        null,
                        null,
                        null
                )
        );
    }

    /**
     * 학습 흐름: 이벤트(ENTER~COMPLETE) + 완강/진행중 진행도 + 회고.
     * <p>
     * lecture_progresses·lecture_reflections 는 (user_id, lecture_id) 유니크 제약이 있고
     * seedLearning() 이 각 강좌 1강에 user[i] 진행도/회고를 이미 만들어 두었다. 충돌을 피하려고
     * 1강은 기존 행을 완강으로 끌어올리고(in-place), 2강 이후만 새 행으로 추가한다.
     */
    private void demoLearningFlow(List<Course> courses, List<User> learners) {
        LocalDateTime now = LocalDateTime.now();

        Course course0 = courses.get(0);
        Long user0 = learners.get(0).getId();
        List<Lecture> lectures0 = orderedLectures(course0);

        List<LearningEvent> events = new ArrayList<>();
        List<LectureProgress> newProgresses = new ArrayList<>();
        List<LectureReflection> reflections = new ArrayList<>();

        // 1강: seed 가 만든 진행도를 완강으로 끌어올림(회고도 이미 존재 → 추가하지 않음)
        if (!lectures0.isEmpty()) {
            Lecture lec0 = lectures0.get(0);
            int dur0 = lec0.getDurationSeconds();
            appendWatchEvents(events, user0, lec0, now.minusDays(4), true);
            lectureProgressRepository.findByUserIdAndLectureId(user0, lec0.getId())
                    .ifPresent(p -> p.applyProgress(dur0, dur0, dur0, now)); // 누적 시청 → 100% 완강
        }
        // 2·3강: 신규 완강 + 회고
        for (int i = 1; i <= 2 && i < lectures0.size(); i++) {
            Lecture lec = lectures0.get(i);
            LocalDateTime base = now.minusDays(3 - i); // 날짜 분산(히트맵용)
            appendWatchEvents(events, user0, lec, base, true);
            newProgresses.add(completedProgress(user0, lec.getId(), lec.getDurationSeconds(), base));
            reflections.add(LectureReflection.create(user0, lec.getId(), (i + 1) + "강 회고: 핵심을 정리했습니다."));
        }
        // 4강: 신규 진행중(40%)
        if (lectures0.size() > 3) {
            Lecture lec = lectures0.get(3);
            appendWatchEvents(events, user0, lec, now, false);
            newProgresses.add(partialProgress(user0, lec.getId(), lec.getDurationSeconds(), 40, now));
        }

        // user[2] / course2 : seed 진행도(진행중) 위에 시청 이벤트만 보강
        Course course2 = courses.get(2);
        Long user2 = learners.get(2).getId();
        List<Lecture> lectures2 = orderedLectures(course2);
        if (!lectures2.isEmpty()) {
            appendWatchEvents(events, user2, lectures2.get(0), now, false);
        }

        learningEventRepository.saveAll(events);
        lectureProgressRepository.saveAll(newProgresses);
        lectureReflectionRepository.saveAll(reflections);
    }

    /** QnA: 답변완료 / 미답변 / 작성자 본인 삭제 케이스. 답변은 강좌 판매자만 작성. */
    private void demoQna(List<Course> courses, List<User> learners) {
        Course course0 = courses.get(0);
        Long instructor0 = course0.getInstructorId();
        List<Lecture> lectures0 = orderedLectures(course0);
        if (lectures0.isEmpty()) {
            return;
        }
        Long lec0 = lectures0.get(0).getId();
        Long lec1 = lectures0.size() > 1 ? lectures0.get(1).getId() : lec0;

        QnaQuestion answered = qnaQuestionRepository.save(
                QnaQuestion.create(learners.get(0).getId(), lec0, "1강 질문", "초반 설명 속도가 빠른데 보충 가능할까요?"));
        qnaAnswerRepository.save(QnaAnswer.create(answered.getId(), instructor0, "네, 보충 자료를 첨부했습니다."));

        qnaQuestionRepository.save(
                QnaQuestion.create(learners.get(2).getId(), lec1, "2강 질문", "예제 코드는 어디서 받을 수 있나요?")); // 미답변

        QnaQuestion removed = qnaQuestionRepository.save(
                QnaQuestion.create(learners.get(0).getId(), lec0, "삭제할 질문", "잘못 올린 질문입니다."));
        removed.delete(); // 작성자 본인 삭제 → 조회 제외
    }

    /** 영상 수정 요청: PENDING / APPROVED / REJECTED 각 1건. */
    private void demoLectureModificationRequests(List<Course> courses, Long adminId) {
        Course course0 = courses.get(0);
        Long instructor = course0.getInstructorId();
        List<Lecture> lectures = orderedLectures(course0);
        if (lectures.size() < 3) {
            return;
        }

        lectureModificationRequestRepository.save(
                pendingModRequest(lectures.get(0), instructor, "오디오 음질 개선본 재업로드"));

        LectureModificationRequest approved = lectureModificationRequestRepository.save(
                pendingModRequest(lectures.get(1), instructor, "자막 추가본 재업로드"));
        approved.approve(adminId);

        LectureModificationRequest rejected = lectureModificationRequestRepository.save(
                pendingModRequest(lectures.get(2), instructor, "화질 변경본 재업로드"));
        rejected.reject("기존 영상보다 화질이 낮아 반려합니다.", adminId);
    }

    private LectureModificationRequest pendingModRequest(Lecture lecture, Long instructorId, String description) {
        String uuid = UUID.randomUUID().toString();
        String afterPath = "/hls/" + lecture.getChapter().getId() + "/" + uuid + "/index.m3u8";
        return LectureModificationRequest.createPending(lecture, instructorId, description, afterPath, uuid);
    }

    /** 출석: 시드 출석이 없는 user[10] 에게 최근 5일 연속 출석 스트릭 생성. */
    private void demoAttendanceStreak(List<User> learners) {
        Long userId = learners.get(10).getId();
        LocalDate today = LocalDate.now();
        LocalDate streakStart = today.minusDays(4);

        List<Attendance> existingStreak = attendanceRepository
                .findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(userId, streakStart, today);
        if (!existingStreak.isEmpty()) {
            attendanceRepository.deleteAllInBatch(existingStreak);
            attendanceRepository.flush();
        }

        List<Attendance> streak = new ArrayList<>();
        Attendance prev = null;
        for (int d = 4; d >= 0; d--) {
            LocalDate date = today.minusDays(d);
            Attendance a = Attendance.record(
                    userId, date, Optional.ofNullable(prev), streak.size(), date.atTime(9, 0));
            streak.add(a);
            prev = a;
        }
        attendanceRepository.saveAll(streak);
    }

    /** 쿠폰: 발급분 일부를 USED / EXPIRED 로 전환(나머지 ISSUED 유지). */
    private void demoCoupons() {
        List<IssuedCoupon> coupons = issuedCouponRepository.findAll();
        coupons.sort(Comparator.comparing(IssuedCoupon::getId));
        if (coupons.size() >= 3) {
            coupons.get(0).use(LocalDateTime.now()); // USED
            coupons.get(1).expire();                  // EXPIRED
            // 나머지: ISSUED 유지
        }
    }

    /** 레포트: study0 레포트를 학습 흐름과 일관된 집계값으로 갱신. */
    private void demoReports(List<Study> studies) {
        Long studyId = studies.get(0).getId();
        LocalDate today = LocalDate.now();
        studyReportRepository.findAll().stream()
                .filter(r -> studyId.equals(r.getStudyId()))
                .findFirst()
                .ifPresent(r -> r.update(
                        5400, 1, 17, 20, 4,
                        "[{\"lectureId\":1,\"title\":\"1강\",\"watchTimeSeconds\":3600}]",
                        "[{\"date\":\"" + today.minusDays(3) + "\",\"progressRate\":40.00},"
                                + "{\"date\":\"" + today + "\",\"progressRate\":85.00}]",
                        "{\"" + today.minusDays(3) + "\":3,\"" + today.minusDays(1) + "\":2,\"" + today + "\":1}"));
    }

    // --- 데모 보강 헬퍼 ---------------------------------------------------------

    /** 강좌의 강의영상을 (챕터 순서 → 강의 순서)로 평탄화. 트랜잭션 내 lazy 로딩 전제. */
    private List<Lecture> orderedLectures(Course course) {
        List<Lecture> result = new ArrayList<>();
        course.getChapters().stream()
                .sorted(Comparator.comparingInt(Chapter::getOrderNo))
                .forEach(ch -> ch.getLectures().stream()
                        .sorted(Comparator.comparingInt(Lecture::getOrderNo))
                        .forEach(result::add));
        return result;
    }

    /** 강의 시청 이벤트 시퀀스: ENTER → START → END → EXIT (완강 시 COMPLETE). */
    private void appendWatchEvents(List<LearningEvent> out, Long userId, Lecture lec,
                                   LocalDateTime base, boolean complete) {
        Long courseId = lec.getChapter().getCourse().getId();
        Long chapterId = lec.getChapter().getId();
        Long lecId = lec.getId();
        int dur = lec.getDurationSeconds();
        int endPos = complete ? dur : dur * 4 / 10;
        String k = userId + "-" + lecId + "-";

        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_ENTER, 0, base, k + "enter"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.VIDEO_PAUSE, endPos, base.plusMinutes(8), k + "pause"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_EXIT, endPos, base.plusMinutes(9), k + "exit"));
        if (complete) {
            out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_COMPLETE, dur, base.plusMinutes(9).plusSeconds(1), k + "complete"));
        }
    }

    private LectureProgress completedProgress(Long userId, Long lectureId, int dur, LocalDateTime when) {
        return new LectureProgress(null, lectureId, userId, dur, dur, 100, true, when.plusMinutes(9), when, when);
    }

    private LectureProgress partialProgress(Long userId, Long lectureId, int dur, int rate, LocalDateTime when) {
        int pos = dur * rate / 100;
        return new LectureProgress(null, lectureId, userId, pos, pos, rate, false, null, when, when);
    }
}
