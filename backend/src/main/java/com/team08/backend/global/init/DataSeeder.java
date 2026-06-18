package com.team08.backend.global.init;

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
import com.team08.backend.domain.enrollment.entity.Enrollment;
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
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.team08.backend.domain.studyreport.repository.StudyReportRepository;
import com.team08.backend.domain.user.entity.User;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final AiFeedbackRepository aiFeedbackRepository;
    private final PasswordEncoder passwordEncoder;

    /** 강의(Course)별 대표 Lecture 를 묶어 카탈로그 결과를 전달한다. */
    private record CatalogData(List<Course> courses, Map<Long, Lecture> sampleLectureByCourse) {}

    @Transactional
    public void seed(SeedConfig cfg) {
        if (userRepository.count() > 0) {
            log.info("[DataInit] 이미 데이터가 존재해 건너뜀");
            return;
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
        CourseStatus[] statuses = {CourseStatus.ON_SALE, CourseStatus.ON_SALE, CourseStatus.ON_SALE, CourseStatus.DRAFT};
        int categoryCount = categories.size();
        int batchSize = cfg.batchSize();

        List<Course> courseBatch   = new ArrayList<>();
        List<Chapter> chapterBatch = new ArrayList<>();
        List<Lecture> lectureBatch = new ArrayList<>();
        Map<Long, Lecture> sampleLectureByCourse = new HashMap<>();

        for (int s = 0; s < sellers.size(); s++) {
            User seller = sellers.get(s);
            for (int c = 0; c < cfg.coursesPerSeller(); c++) {
                int idx = s * cfg.coursesPerSeller() + c + 1;
                courseBatch.add(Course.builder()
                        .instructorId(seller.getId())
                        .categoryId(categories.get(idx % categoryCount).getId())
                        .title("강의 " + idx + " - " + categories.get(idx % categoryCount).getName())
                        .description("강의 " + idx + "에 대한 설명입니다.")
                        .thumbnail("thumb" + idx + ".jpg")
                        .price((idx % 5 + 1) * 10000)
                        .status(statuses[idx % statuses.length])
                        .build());
            }
        }

        List<Course> savedCourses = courseRepository.saveAll(courseBatch);
        log.info("[DataInit] 강의 {}개 생성", savedCourses.size());

        for (Course course : savedCourses) {
            for (int i = 1; i <= cfg.chaptersPerCourse(); i++) {
                chapterBatch.add(Chapter.builder()
                        .title(course.getTitle() + " - " + i + "장")
                        .orderNo(i)
                        .course(course)
                        .build());

                if (chapterBatch.size() >= batchSize) {
                    addLectures(cfg, chapterRepository.saveAll(chapterBatch), lectureBatch);
                    chapterBatch.clear();
                    if (lectureBatch.size() >= batchSize) {
                        persistLectures(lectureBatch, sampleLectureByCourse);
                    }
                }
            }
        }

        if (!chapterBatch.isEmpty()) {
            addLectures(cfg, chapterRepository.saveAll(chapterBatch), lectureBatch);
        }
        if (!lectureBatch.isEmpty()) {
            persistLectures(lectureBatch, sampleLectureByCourse);
        }

        log.info("[DataInit] 챕터 {}개, 강의영상 {}개 생성 완료",
                (long) savedCourses.size() * cfg.chaptersPerCourse(),
                (long) savedCourses.size() * cfg.chaptersPerCourse() * cfg.lecturesPerChapter());

        return new CatalogData(savedCourses, sampleLectureByCourse);
    }

    private void addLectures(SeedConfig cfg, List<Chapter> chapters, List<Lecture> buffer) {
        for (Chapter chapter : chapters) {
            for (int j = 1; j <= cfg.lecturesPerChapter(); j++) {
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

    /** 강의 영상 저장 + 강의(Course)별 대표 Lecture 캡처 (in-memory 참조라 추가 조회 없음) */
    private void persistLectures(List<Lecture> buffer, Map<Long, Lecture> sampleLectureByCourse) {
        for (Lecture lecture : lectureRepository.saveAll(buffer)) {
            sampleLectureByCourse.putIfAbsent(lecture.getChapter().getCourse().getId(), lecture);
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
                            CouponType.AUTO,
                            10, // totalQuantity
                            CouponUsageType.SINGLE_USE,
                            false,

                            DiscountType.PERCENT,
                            10,     // discountValue
                            5000,   // maxDiscountAmount
                            0,      // minOrderAmount
                            30,     // validDays

                            now.minusDays(1),
                            now.plusDays(30),

                            null, // categoryIds
                            List.of(courses.get(p).getId()) // courseIds
                    )
            );
        }
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

    /** 학습 진행 / 이벤트 / 출석 / 회고 (수강생 1명당 1건) */
    private void seedLearning(List<User> users, CatalogData catalog) {
        List<Course> courses = catalog.courses();
        Map<Long, Lecture> sample = catalog.sampleLectureByCourse();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        int count = Math.min(users.size(), courses.size());

        List<LectureProgress> progresses = new ArrayList<>();
        List<LearningEvent> events = new ArrayList<>();
        // TODO: 학습 도메인이아니라 커머스/쿠폰 도메인으로 옮겨야함
        // TODO: 비기능적 요구사항에 의거한 데이터 규모를 채택해야해서 수치적인 부분은 추가 결정 필요
        List<Attendance> attendances = new ArrayList<>();
        List<LectureReflection> reflections = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Course course = courses.get(i);
            Lecture lecture = sample.get(course.getId());
            Long userId = users.get(i).getId();

            progresses.add(new LectureProgress(
                    null, lecture.getId(), userId, 300, 300, new BigDecimal("50.00"), false, null, now, now));
            events.add(LearningEvent.create(
                    userId, course.getId(), lecture.getChapter().getId(), lecture.getId(),
                    LearningEventType.LECTURE_ENTER, 0, now, "evt-seed-" + i));
            attendances.add(Attendance.record(userId, today, Optional.empty(), 0, now));
            reflections.add(LectureReflection.create(userId, lecture.getId(), "학습 회고 " + i));
        }
        lectureProgressRepository.saveAll(progresses);
        learningEventRepository.saveAll(events);
        attendanceRepository.saveAll(attendances);
        lectureReflectionRepository.saveAll(reflections);

        log.info("[DataInit] 학습 데이터 생성 ({}건)", count);
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

            questions.add(QnaQuestion.create(users.get(i).getId(), lecture.getId(), "질문 " + (i + 1), "질문 내용 " + (i + 1)));
            modRequests.add(new LectureModificationRequest(
                    null, lecture, course.getInstructorId(), "영상 교체 요청 " + (i + 1),
                    lecture.getM3u8Path(), "/hls/new/" + i + "/index.m3u8", RequestStatus.PENDING, null, null));
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
            studyReportRepository.save(new StudyReport(
                    null, users.get(s % users.size()).getId(), study.getId(), 3600, 5, new BigDecimal("80.00")));
            aiFeedbackRepository.save(AiFeedback.startProcessing(
                    seller.getId(), study.getId(), activity.getId(), "활동 스냅샷", "claude-opus-4-8", "v1"));
        }
        log.info("[DataInit] 스터디 데이터 생성 ({}건)", sellers.size());
    }
}
