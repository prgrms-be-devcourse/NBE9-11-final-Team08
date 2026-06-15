package com.team08.backend.global.config;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.domain.coursestatushistory.repository.CourseStatusHistoryRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.seller.entity.Seller;
import com.team08.backend.domain.seller.repository.SellerRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * dev 프로파일에서만 실행되는 초기 데이터 시딩 컴포넌트.
 * 애플리케이션 기동 시 users 테이블이 비어있는 경우에만 실행된다.
 *
 * 생성 계정:
 *   - admin@playlearn.com  / admin1234!  (관리자)
 *   - seller@playlearn.com / seller1234! (판매자 / 강사)
 *   - user1@playlearn.com  / user1234!   (학습자)
 *   - user2@playlearn.com  / user1234!   (학습자)
 */
@Slf4j
@Component
@Profile({"dev","prod"})
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final CourseStatusHistoryRepository courseStatusHistoryRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("[DataInitializer] 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("[DataInitializer] 초기 데이터 생성을 시작합니다.");

        // ── 1. 카테고리 ──────────────────────────────────────────────────
        Category devCategory      = categoryRepository.save(new Category(null, null, "개발",       0));
        Category backendCategory   = categoryRepository.save(new Category(null, devCategory.getId(),  "백엔드",    1));
        Category frontendCategory  = categoryRepository.save(new Category(null, devCategory.getId(),  "프론트엔드", 1));
        Category designCategory    = categoryRepository.save(new Category(null, null, "디자인",     0));
        Category businessCategory  = categoryRepository.save(new Category(null, null, "비즈니스",   0));

        // ── 2. 관리자 계정 (ROLE_ADMIN – 직접 SQL) ───────────────────────
        // User.createUser/createSeller 만 제공되므로 JDBC로 직접 삽입
        String adminPw = passwordEncoder.encode("admin1234!");
        jdbcTemplate.update(
                "INSERT INTO users (email, password, nickname, profile_image, role, created_at, updated_at) " +
                "VALUES (?, ?, ?, NULL, 'ROLE_ADMIN', NOW(), NOW())",
                "admin@playlearn.com", adminPw, "관리자"
        );
        User admin = userRepository.findByEmail("admin@playlearn.com").orElseThrow();

        // ── 3. 판매자(강사) 계정 ─────────────────────────────────────────
        User sellerUser = userRepository.save(
                User.createSeller(
                        "seller@playlearn.com",
                        passwordEncoder.encode("seller1234!"),
                        "김강사",
                        null
                )
        );
        // Seller 엔티티는 JPA Auditing이 없으므로 직접 시각 지정
        sellerRepository.save(new Seller(null, sellerUser.getId(), LocalDateTime.now(), LocalDateTime.now()));

        // ── 4. 학습자 계정 ───────────────────────────────────────────────
        User learner1 = userRepository.save(
                User.createUser("user1@playlearn.com", passwordEncoder.encode("user1234!"), "홍길동", null)
        );
        User learner2 = userRepository.save(
                User.createUser("user2@playlearn.com", passwordEncoder.encode("user1234!"), "김철수", null)
        );

        // ── 5. 쿠폰 정책 ─────────────────────────────────────────────────
        // 신규 가입 자동 발급 쿠폰 (10% 할인)
        // CouponPolicyCreateRequest(name, discountType, discountValue, validDays,
        //   totalQuantity, categoryId, couponType, couponTarget, usageType,
        //   isStackable, issueStartDate, issueEndDate)
        couponPolicyRepository.save(CouponPolicy.create(new CouponPolicyCreateRequest(
                "신규가입 웰컴 쿠폰",
                DiscountType.PERCENT,
                10,
                30,       // validDays
                null,     // totalQuantity (무제한)
                null,     // categoryId
                CouponType.AUTO,
                CouponTarget.ALL,
                CouponUsageType.SINGLE_USE,
                false,
                null,
                null
        )));

        // 일반 다운로드 쿠폰 (5,000원 할인)
        couponPolicyRepository.save(CouponPolicy.create(new CouponPolicyCreateRequest(
                "5천원 할인 쿠폰",
                DiscountType.AMOUNT,
                5000,
                60,       // validDays
                null,     // totalQuantity (무제한)
                null,     // categoryId
                CouponType.NORMAL,
                CouponTarget.ALL,
                CouponUsageType.SINGLE_USE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(3)
        )));

        // 선착순 쿠폰 (20% 할인, 100장 한정)
        couponPolicyRepository.save(CouponPolicy.create(new CouponPolicyCreateRequest(
                "선착순 20% 할인 쿠폰",
                DiscountType.PERCENT,
                20,
                14,       // validDays
                100,      // totalQuantity (선착순 필수)
                null,     // categoryId
                CouponType.FCFS,
                CouponTarget.ALL,
                CouponUsageType.SINGLE_USE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
        )));

        // ── 6. 강좌 ─────────────────────────────────────────────────────
        Course springCourse = saveCourse(
                sellerUser.getId(), backendCategory.getId(), admin.getId(),
                "Spring Boot 완전 정복",
                "Spring Boot 3.x 기반 REST API 설계부터 배포까지, 실전 프로젝트로 배우는 백엔드 개발",
                "https://placehold.co/640x360/3b82f6/white?text=Spring+Boot",
                49000
        );

        Course reactCourse = saveCourse(
                sellerUser.getId(), frontendCategory.getId(), admin.getId(),
                "React 완전 정복",
                "React 18 + TypeScript로 배우는 현대적인 프론트엔드 개발, 상태 관리부터 성능 최적화까지",
                "https://placehold.co/640x360/06b6d4/white?text=React",
                39000
        );

        Course javaBasicCourse = saveCourse(
                sellerUser.getId(), backendCategory.getId(), admin.getId(),
                "Java 기초부터 심화까지",
                "Java의 기초 문법부터 OOP, 컬렉션, 스트림, 멀티스레딩까지 완벽하게 다루는 강의",
                "https://placehold.co/640x360/f59e0b/white?text=Java",
                29000
        );

        // ── 7. 챕터 & 강의 ───────────────────────────────────────────────
        addCurriculum(springCourse, new String[][]{
                {"환경 설정 및 프로젝트 구조",    "Spring Boot란?", "프로젝트 생성", "의존성 관리"},
                {"REST API 설계",                  "HTTP 메서드와 상태코드", "컨트롤러 작성", "DTO 패턴"},
                {"데이터 접근 계층 (JPA)",         "Entity 설계", "Repository 패턴", "JPQL & QueryDSL"},
                {"인증 & 보안 (Spring Security)",  "JWT 인증 흐름", "필터 체인 구성", "권한 관리"},
                {"테스트 & 배포",                  "단위 테스트", "통합 테스트", "Docker 배포"}
        });

        addCurriculum(reactCourse, new String[][]{
                {"React 기초",        "JSX & 컴포넌트", "Props & State", "이벤트 처리"},
                {"Hooks 심화",        "useState & useEffect", "useContext", "커스텀 훅"},
                {"상태 관리",         "Context API", "Zustand 입문", "서버 상태 관리"},
                {"상태 관리",         "Context API", "Zustand 입문", "서버 상태 관리"},
                {"라우팅 & 성능",     "React Router v6", "코드 스플리팅", "메모이제이션"}
        });

        addCurriculum(javaBasicCourse, new String[][]{
                {"Java 기초 문법",  "변수와 타입", "제어문", "배열"},
                {"객체지향 프로그래밍", "클래스와 객체", "상속과 다형성", "인터페이스"},
                {"객체지향 프로그래밍", "클래스와 객체", "상속과 다형성", "인터페이스"},
                {"객체지향 프로그래밍", "클래스와 객체", "상속과 다형성", "인터페이스"},
                {"컬렉션 & 스트림",  "List & Map", "Stream API", "람다 표현식"}
        });

        // ── 8. 스터디 ────────────────────────────────────────────────────
        saveStudy(sellerUser, springCourse, "Spring Boot 스터디",
                "Spring Boot를 함께 공부하며 백엔드 개발자로 성장하는 스터디");
        saveStudy(sellerUser, reactCourse, "React 스터디",
                "React를 함께 학습하고 실전 프로젝트를 완성하는 스터디");

        log.info("[DataInitializer] 초기 데이터 생성 완료.");
        log.info("[DataInitializer] ─────────────────────────────────────");
        log.info("[DataInitializer] 계정 정보:");
        log.info("[DataInitializer]   관리자  : admin@playlearn.com  / admin1234!");
        log.info("[DataInitializer]   판매자  : seller@playlearn.com / seller1234!");
        log.info("[DataInitializer]   학습자1 : user1@playlearn.com  / user1234!");
        log.info("[DataInitializer]   학습자2 : user2@playlearn.com  / user1234!");
        log.info("[DataInitializer] ─────────────────────────────────────");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * ON_SALE 상태의 강좌를 생성하고, 상태 이력(DRAFT→IN_REVIEW→ON_SALE)을 기록한다.
     */
    private Course saveCourse(Long instructorId, Long categoryId, Long adminId,
                               String title, String description, String thumbnail, int price) {
        Course course = Course.builder()
                .instructorId(instructorId)
                .categoryId(categoryId)
                .title(title)
                .description(description)
                .thumbnail(thumbnail)
                .price(price)
                .status(CourseStatus.ON_SALE)
                .build();
        courseRepository.save(course);

        courseStatusHistoryRepository.save(
                CourseStatusHistory.of(course.getId(), CourseStatus.DRAFT, CourseStatus.IN_REVIEW, instructorId));
        courseStatusHistoryRepository.save(
                CourseStatusHistory.of(course.getId(), CourseStatus.IN_REVIEW, CourseStatus.ON_SALE, adminId));

        return course;
    }

    /**
     * 챕터와 강의를 일괄 생성한다.
     * curriculum 형식: { {"챕터제목", "강의1", "강의2", ...}, ... }
     */
    private void addCurriculum(Course course, String[][] curriculum) {
        for (int ci = 0; ci < curriculum.length; ci++) {
            String[] row = curriculum[ci];

            Chapter chapter = Chapter.builder()
                    .title(row[0])
                    .orderNo(ci + 1)
                    .course(course)
                    .build();
            chapterRepository.save(chapter);

            for (int li = 1; li < row.length; li++) {
                Lecture lecture = Lecture.builder()
                        .title(row[li])
                        .m3u8Path("")          // 실제 HLS 경로는 영상 업로드 후 설정
                        .summary(null)
                        .durationSeconds((li) * 600) // 10분 단위 임시값
                        .orderNo(li)
                        .isFreePreview(li == 1)       // 각 챕터 첫 강의는 무료 미리보기
                        .chapter(chapter)
                        .build();
                lectureRepository.save(lecture);
            }
        }
    }

    /**
     * ACTIVE 상태의 스터디를 생성하고 owner 멤버를 등록한다.
     */
    private void saveStudy(User owner, Course course, String title, String description) {
        Study study = Study.createForCourse(owner, course, title, description);
        study.activate();
        studyRepository.save(study);
        studyMemberRepository.save(StudyMember.owner(owner, study));
    }
}
