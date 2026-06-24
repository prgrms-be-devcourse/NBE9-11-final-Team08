package com.team08.backend.domain.order.service;

import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.dto.OrderDetailResponse;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CART_ID = 10L;
    private static final Long ORDER_ID = 100L;
    private static final Long COURSE_ID = 1000L;
    private static final LocalDateTime FIXED_NOW = LocalDateTime.parse("2026-06-17T12:00:00");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                FIXED_NOW.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                cartRepository,
                courseRepository,
                enrollmentRepository,
                paymentRepository,
                fixedClock
        );
    }

    @Test
    void cartOrderCanBeCreated() {
        Cart cart = cart(CART_ID, USER_ID, COURSE_ID, COURSE_ID + 1);
        Course firstCourse = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        Course secondCourse = course(COURSE_ID + 1, "JPA", 20_000, CourseStatus.ON_SALE);

        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));
        given(courseRepository.findAllById(any())).willReturn(List.of(firstCourse, secondCourse));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID, COURSE_ID + 1)
        )).willReturn(List.of());
        stubOrderSave();

        OrderDetailResponse response = orderService.createOrderFromCart(USER_ID);

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.totalPrice()).isEqualTo(50_000);
        assertThat(response.discountPrice()).isZero();
        assertThat(response.finalPrice()).isEqualTo(50_000);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.orderedAt()).isEqualTo(FIXED_NOW);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items()).extracting("courseTitle").containsExactly("Spring", "JPA");
        assertThat(response.items()).extracting("price").containsExactly(30_000, 20_000);
        assertThat(cart.getItems()).isEmpty();
        verify(enrollmentRepository, never())
                .existsByUserIdAndCourseIdAndStatus(any(), any(), any());
    }

    @Test
    void emptyCartCannotCreateOrder() {
        Cart cart = cart(CART_ID, USER_ID);
        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrderFromCart(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMPTY_CART));

        verifyNoInteractions(orderRepository, orderItemRepository, courseRepository, enrollmentRepository);
    }

    @Test
    void nonOnSaleCourseCannotCreateOrder() {
        Cart cart = cart(CART_ID, USER_ID, COURSE_ID);
        Course course = course(COURSE_ID, "Draft", 30_000, CourseStatus.DRAFT);

        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));
        given(courseRepository.findAllById(any())).willReturn(List.of(course));

        assertThatThrownBy(() -> orderService.createOrderFromCart(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_ON_SALE));

        verifyNoInteractions(orderRepository, orderItemRepository, enrollmentRepository);
    }

    @Test
    void activeEnrollmentCannotCreateOrder() {
        Cart cart = cart(CART_ID, USER_ID, COURSE_ID);
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);

        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));
        given(courseRepository.findAllById(any())).willReturn(List.of(course));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID)
        )).willReturn(List.of(COURSE_ID));

        assertThatThrownBy(() -> orderService.createOrderFromCart(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ALREADY_ENROLLED));

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    @Test
    void directOrderCanBeCreated() {
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubOrderSave();

        OrderDetailResponse response = orderService.createDirectOrder(USER_ID, COURSE_ID);

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.totalPrice()).isEqualTo(30_000);
        assertThat(response.finalPrice()).isEqualTo(30_000);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.orderedAt()).isEqualTo(FIXED_NOW);
        assertThat(response.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.courseId()).isEqualTo(COURSE_ID);
                    assertThat(item.courseTitle()).isEqualTo("Spring");
                    assertThat(item.price()).isEqualTo(30_000);
                });
    }

    @Test
    void cartOrderChecksActiveEnrollmentsWithSingleCourseIdLookup() {
        Cart cart = cart(CART_ID, USER_ID, COURSE_ID, COURSE_ID + 1);
        Course firstCourse = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        Course secondCourse = course(COURSE_ID + 1, "JPA", 20_000, CourseStatus.ON_SALE);

        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));
        given(courseRepository.findAllById(any())).willReturn(List.of(firstCourse, secondCourse));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID, COURSE_ID + 1)
        )).willReturn(List.of());
        stubOrderSave();

        orderService.createOrderFromCart(USER_ID);

        ArgumentCaptor<List<Long>> courseIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(enrollmentRepository).findCourseIdsByUserIdAndStatusAndCourseIdIn(
                eq(USER_ID),
                eq(EnrollmentStatus.ACTIVE),
                courseIdsCaptor.capture()
        );
        assertThat(courseIdsCaptor.getValue()).containsExactly(COURSE_ID, COURSE_ID + 1);
        verify(enrollmentRepository, never())
                .existsByUserIdAndCourseIdAndStatus(any(), any(), any());
    }

    @Test
    void myOrderDetailCanBeFound() {
        Order order = order(ORDER_ID, USER_ID, OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, ORDER_ID, COURSE_ID, "Spring", 30_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));

        OrderDetailResponse response = orderService.getMyOrder(USER_ID, ORDER_ID);

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.items()).singleElement()
                .satisfies(item -> assertThat(item.courseTitle()).isEqualTo("Spring"));
    }

    @Test
    void otherUsersOrderDetailCannotBeFound() {
        given(orderRepository.findByIdAndUserId(ORDER_ID, OTHER_USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder(OTHER_USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void pendingPaymentOrderCanBeCanceled() {
        Order order = order(ORDER_ID, USER_ID, OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, ORDER_ID, COURSE_ID, "Spring", 30_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));

        OrderDetailResponse response = orderService.cancelMyOrder(USER_ID, ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(response.canceledAt()).isEqualTo(FIXED_NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void pendingPaymentOrderWithDeclinedPaymentCancelsPaymentTogether() {
        Order order = order(ORDER_ID, USER_ID, OrderStatus.PENDING_PAYMENT);
        Payment payment = payment(order, PaymentStatus.DECLINED);
        OrderItem orderItem = orderItem(1L, ORDER_ID, COURSE_ID, "Spring", 30_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));

        OrderDetailResponse response = orderService.cancelMyOrder(USER_ID, ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void pendingPaymentOrderWithReadyPaymentCancelsPaymentTogether() {
        Order order = order(ORDER_ID, USER_ID, OrderStatus.PENDING_PAYMENT);
        Payment payment = payment(order, PaymentStatus.READY);
        OrderItem orderItem = orderItem(1L, ORDER_ID, COURSE_ID, "Spring", 30_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));

        OrderDetailResponse response = orderService.cancelMyOrder(USER_ID, ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void nonPendingPaymentOrderCannotBeCanceled() {
        Order order = order(ORDER_ID, USER_ID, OrderStatus.PAID);
        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));

        verifyNoInteractions(orderItemRepository, paymentRepository);
    }

    private void stubOrderSave() {
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", ORDER_ID);
            long id = 1L;
            for (OrderItem item : order.getItems()) {
                ReflectionTestUtils.setField(item, "id", id++);
            }
            return order;
        });
    }

    private Order order(Long orderId, Long userId, OrderStatus status) {
        LocalDateTime now = LocalDateTime.parse("2026-06-12T10:00:00");
        Order order = Order.createPendingPayment(userId, "ORD-20260612100000-ABC12345", now);
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "totalPrice", 30_000);
        ReflectionTestUtils.setField(order, "finalPrice", 30_000);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    private Payment payment(Order order, PaymentStatus status) {
        Payment payment = Payment.createReady(order, FIXED_NOW.minusDays(1));
        if (status == PaymentStatus.DECLINED) {
            payment.markProcessing(FIXED_NOW.minusDays(1));
            payment.decline("payment-key", "CARD", "결제 거절", FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.READY) {
            return payment;
        } else if (status == PaymentStatus.SUCCESS) {
            payment.markProcessing(FIXED_NOW.minusDays(1));
            payment.succeed("payment-key", "CARD", FIXED_NOW.minusDays(1));
        }
        return payment;
    }

    private OrderItem orderItem(Long orderItemId, Long orderId, Long courseId, String courseTitle, int price) {
        LocalDateTime now = LocalDateTime.parse("2026-06-12T10:00:00");
        Order order = order(orderId, USER_ID, OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = OrderItem.createSnapshot(order, courseId, courseTitle, price, 0, price, now);
        ReflectionTestUtils.setField(orderItem, "id", orderItemId);
        return orderItem;
    }

    private Cart cart(Long cartId, Long userId, Long... courseIds) {
        Cart cart = Cart.create(userId);
        ReflectionTestUtils.setField(cart, "id", cartId);
        for (Long courseId : courseIds) {
            cart.addItem(courseId);
        }
        return cart;
    }

    private Course course(Long courseId, String title, int price, CourseStatus status) {
        Course course = newInstance(Course.class);
        ReflectionTestUtils.setField(course, "id", courseId);
        ReflectionTestUtils.setField(course, "instructorId", 1L);
        ReflectionTestUtils.setField(course, "categoryId", 1L);
        ReflectionTestUtils.setField(course, "title", title);
        ReflectionTestUtils.setField(course, "description", title + " description");
        ReflectionTestUtils.setField(course, "thumbnail", title + ".png");
        ReflectionTestUtils.setField(course, "price", price);
        ReflectionTestUtils.setField(course, "status", status);
        return course;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create test entity: " + type.getSimpleName(), e);
        }
    }
}
