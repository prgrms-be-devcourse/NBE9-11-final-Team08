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
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
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
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
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
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final CartRepository cartRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
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

    @Transactional
    public void init() {
        if (userRepository.count() > 0) {
            return;
        }

        String password = passwordEncoder.encode("Test1234!");

        // 사용자
        userRepository.save(User.createAdmin("admin@test.com", password, "관리자", null));
        User seller = userRepository.save(User.createSeller("seller@test.com", password, "강사", null));
        User user1  = userRepository.save(User.createUser("user1@test.com", password, "수강생1", null));
        User user2  = userRepository.save(User.createUser("user2@test.com", password, "수강생2", null));

        sellerRepository.save(new Seller(null, seller.getId(), LocalDateTime.now(), LocalDateTime.now()));

        // 카테고리
        Category dev     = categoryRepository.save(new Category(null, null, "개발", 0));
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
        List<Lecture> course1Lectures = saveChaptersAndLectures(course1, List.of("환경 설정", "REST API 만들기", "배포"));
        saveChaptersAndLectures(course2, List.of("React 기초", "상태 관리", "프로젝트"));

        // 나머지 도메인 샘플 (엔티티별 1건씩)
        Lecture sampleLecture = course1Lectures.get(0);
        seedCommerce(user1, user2, course1, sampleLecture);
        seedLearning(user1, user2, course1, sampleLecture);
        seedQna(seller, user2, sampleLecture);
        seedStudy(seller, user1, course1, sampleLecture);
    }

    private List<Lecture> saveChaptersAndLectures(Course course, List<String> chapterTitles) {
        List<Lecture> lectures = new ArrayList<>();
        for (int i = 0; i < chapterTitles.size(); i++) {
            Chapter chapter = chapterRepository.save(Chapter.builder()
                    .title(chapterTitles.get(i)).orderNo(i + 1).course(course).build());

            for (int j = 1; j <= 3; j++) {
                lectures.add(lectureRepository.save(Lecture.builder()
                        .title(chapterTitles.get(i) + " - 강의 " + j)
                        .summary("요약").durationSeconds(600 * j).orderNo(j)
                        .isFreePreview(j == 1).m3u8Path("/hls/" + course.getId() + "/" + chapter.getId() + "/" + j + "/index.m3u8")
                        .chapter(chapter).build()));
            }
        }
        return lectures;
    }

    /** 장바구니 → 쿠폰 → 주문/결제/수강 흐름 */
    private void seedCommerce(User user1, User user2, Course course, Lecture lecture) {
        LocalDateTime now = LocalDateTime.now();

        // 장바구니 (CartItem 은 cascade 로 함께 저장)
        Cart cart = Cart.create(user1.getId());
        cart.addItem(course.getId());
        cartRepository.save(cart);

        // 쿠폰 정책 (CouponPolicyCourse 는 cascade 로 함께 저장) + 발급
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.createNormalPolicy(
                "10% 할인 쿠폰", DiscountType.PERCENT, 10, 5000, 0, 30, null,
                List.of(course.getId()), CouponTarget.COURSE, CouponUsageType.SINGLE_USE,
                false, now.minusDays(1), now.plusDays(30)));
        IssuedCoupon coupon = issuedCouponRepository.save(IssuedCoupon.create(policy, user2.getId(), now));

        // 주문 → 주문상품 → 쿠폰사용 → 결제 → 수강등록
        int price = course.getPrice();
        int discount = policy.calculateDiscountAmount(price);
        int finalPrice = price - discount;

        Order order = orderRepository.save(
                Order.createPendingPayment(user2.getId(), "ORD-SEED-0001", price, discount, finalPrice, now));
        order.markPaid(now);
        orderItemRepository.save(
                OrderItem.createSnapshot(order.getId(), course.getId(), course.getTitle(), price, discount, finalPrice, now));
        orderCouponUsageRepository.save(new OrderCouponUsage(null, order.getId(), coupon.getId(), discount));
        coupon.applyUsage(CouponUsageType.SINGLE_USE, now);

        Payment payment = Payment.createReady(order.getId(), finalPrice, now);
        payment.succeed("PAYKEY-SEED-0001", "CARD", now);
        paymentRepository.save(payment);

        enrollmentRepository.save(Enrollment.createActive(user2.getId(), course.getId(), order.getId(), now));
    }

    /** 학습 진행 / 이벤트 / 출석 / 회고 흐름 */
    private void seedLearning(User user1, User user2, Course course, Lecture lecture) {
        LocalDateTime now = LocalDateTime.now();

        lectureProgressRepository.save(new LectureProgress(
                null, lecture.getId(), user2.getId(), 300, 300, new BigDecimal("50.00"), false, null, now, now));
        learningEventRepository.save(LearningEvent.create(
                user2.getId(), course.getId(), lecture.getChapter().getId(), lecture.getId(),
                LearningEventType.LECTURE_ENTER, 0, now, "evt-seed-0001"));
        attendanceRepository.save(Attendance.record(user1.getId(), LocalDate.now(), Optional.empty(), 0, now));
        lectureReflectionRepository.save(LectureReflection.create(user2.getId(), lecture.getId(), "오늘 학습 회고"));
    }

    /** Q&A / 강의 수정 요청 흐름 */
    private void seedQna(User seller, User user2, Lecture lecture) {
        QnaQuestion question = qnaQuestionRepository.save(
                QnaQuestion.create(user2.getId(), lecture.getId(), "질문 있습니다", "이 부분이 이해가 안 돼요"));
        qnaAnswerRepository.save(QnaAnswer.create(question.getId(), seller.getId(), "이렇게 이해하시면 됩니다"));

        lectureModificationRequestRepository.save(new LectureModificationRequest(
                null, lecture, seller.getId(), "영상 교체 요청",
                lecture.getM3u8Path(), "/hls/new/index.m3u8", RequestStatus.PENDING, null, null));
    }

    /** 스터디 / 멤버 / 활동 / 리포트 / AI 피드백 흐름 */
    private void seedStudy(User seller, User user1, Course course, Lecture lecture) {
        Study study = studyRepository.save(Study.createForCourse(seller, course, "함께하는 스터디", "스터디 설명"));
        study.activate();
        studyMemberRepository.save(StudyMember.owner(seller, study));
        StudyActivity activity = studyActivityRepository.save(
                StudyActivity.create(study.getId(), seller.getId(), "첫 활동 내용"));
        studyReportRepository.save(new StudyReport(null, user1.getId(), study.getId(), 3600, 5, new BigDecimal("80.00")));
        aiFeedbackRepository.save(AiFeedback.startProcessing(
                seller.getId(), study.getId(), activity.getId(), "활동 스냅샷", "claude-opus-4-8", "v1"));
    }
}
