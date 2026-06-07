package com.team08.backend.domain.orderpayment;

import com.team08.backend.domain.cart.dto.CartResponse;
import com.team08.backend.domain.cart.service.CartService;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.dto.CourseAccessResponse;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.enrollment.service.EnrollmentService;
import com.team08.backend.domain.order.dto.OrderDetailResponse;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.service.OrderService;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.service.PaymentService;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderPaymentMvpIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void addCartItemPreventsDuplicateCourse() {
        User user = saveUser("cart@example.com");
        Course course = saveCourse(user, "Spring MVP", 30000);

        CartResponse response = cartService.addItem(user.getId(), course.getId());

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalPrice()).isEqualTo(30000);
        assertThatThrownBy(() -> cartService.addItem(user.getId(), course.getId()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createOrderFromCartStoresSnapshotAndClearsCart() {
        User user = saveUser("order@example.com");
        Course course = saveCourse(user, "Order Course", 45000);
        cartService.addItem(user.getId(), course.getId());

        OrderDetailResponse order = orderService.createFromCart(user.getId());

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.totalPrice()).isEqualTo(45000);
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).courseTitle()).isEqualTo("Order Course");
        assertThat(order.items().get(0).price()).isEqualTo(45000);
        assertThat(cartService.getMyCart(user.getId()).items()).isEmpty();
    }

    @Test
    void mockPaymentSuccessIssuesEnrollmentOnlyOnce() {
        User user = saveUser("payment@example.com");
        Course course = saveCourse(user, "Payment Course", 55000);
        cartService.addItem(user.getId(), course.getId());
        OrderDetailResponse order = orderService.createFromCart(user.getId());

        PaymentResponse firstPayment = paymentService.mockSuccess(user.getId(), order.orderId(), "pay-1", "CARD");
        PaymentResponse secondPayment = paymentService.mockSuccess(user.getId(), order.orderId(), "pay-2", "CARD");
        CourseAccessResponse access = enrollmentService.canAccess(user.getId(), course.getId());

        assertThat(firstPayment.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(secondPayment.paymentId()).isEqualTo(firstPayment.paymentId());
        assertThat(access.accessible()).isTrue();
        assertThat(enrollmentRepository.findAll()).hasSize(1);
    }

    @Test
    void mockPaymentFailKeepsOrderPending() {
        User user = saveUser("fail@example.com");
        Course course = saveCourse(user, "Fail Course", 15000);
        cartService.addItem(user.getId(), course.getId());
        OrderDetailResponse order = orderService.createFromCart(user.getId());

        PaymentResponse payment = paymentService.mockFail(user.getId(), order.orderId(), "카드 승인 실패");
        OrderDetailResponse currentOrder = orderService.getMyOrder(user.getId(), order.orderId());

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.failedReason()).isEqualTo("카드 승인 실패");
        assertThat(currentOrder.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    private User saveUser(String email) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", email);
        ReflectionTestUtils.setField(user, "role", "USER");
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return userRepository.save(user);
    }

    private Course saveCourse(User instructor, String title, Integer price) {
        Course course = newInstance(Course.class);
        ReflectionTestUtils.setField(course, "instructor", instructor);
        ReflectionTestUtils.setField(course, "title", title);
        ReflectionTestUtils.setField(course, "price", price);
        ReflectionTestUtils.setField(course, "status", CourseStatus.ON_SALE);
        ReflectionTestUtils.setField(course, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(course, "updatedAt", LocalDateTime.now());
        return courseRepository.save(course);
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트 엔티티 생성에 실패했습니다.", e);
        }
    }
}
