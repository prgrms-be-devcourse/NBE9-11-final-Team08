package com.team08.backend.domain.order.service;

import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.cartitem.entity.CartItem;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.dto.OrderDetailResponse;
import com.team08.backend.domain.order.dto.OrderSummaryResponse;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional
    public OrderDetailResponse createOrderFromCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPTY_CART));

        List<CartItem> cartItems = cart.getItems();
        if (cartItems.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_CART);
        }

        Map<Long, Course> courseMap = findCourseMap(cartItems);
        List<Course> orderCourses = cartItems.stream()
                .map(cartItem -> getCourse(courseMap, cartItem.getCourseId()))
                .toList();

        OrderDetailResponse response = createPendingPaymentOrder(userId, orderCourses);

        // 장바구니 주문 성공 후 동일 항목으로 재주문되지 않도록 Cart 도메인 메서드로 비운다.
        cart.clearItems();

        return response;
    }

    @Transactional
    public OrderDetailResponse createDirectOrder(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        return createPendingPaymentOrder(userId, List.of(course));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getMyOrders(Long userId) {
        return orderRepository.findAllByUserIdOrderByOrderedAtDescIdDesc(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrder(Long userId, Long orderId) {
        Order order = findMyOrder(userId, orderId);
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());

        return OrderDetailResponse.from(order, orderItems);
    }

    @Transactional
    public OrderDetailResponse cancelMyOrder(Long userId, Long orderId) {
        Order order = findMyOrderForUpdate(userId, orderId);
        Optional<Payment> payment = paymentRepository.findByOrder_Id(order.getId());
        validateCancelablePayment(payment);

        LocalDateTime canceledAt = LocalDateTime.now(clock);
        order.cancel(canceledAt);
        payment.ifPresent(cancelablePayment -> cancelablePayment.cancel(canceledAt));

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        return OrderDetailResponse.from(order, orderItems);
    }

    private OrderDetailResponse createPendingPaymentOrder(Long userId, List<Course> courses) {
        courses.forEach(this::validateOrderableCourse);
        validateNotAlreadyEnrolled(userId, courses);

        LocalDateTime orderedAt = LocalDateTime.now(clock);
        Order order = Order.createPendingPayment(
                userId,
                createOrderNumber(orderedAt),
                orderedAt
        );

        courses.forEach(course -> order.addItem(
                course.getId(),
                course.getTitle(),
                course.getThumbnail(),
                course.getPrice(),
                orderedAt
        ));
        Order savedOrder = orderRepository.save(order);

        return OrderDetailResponse.from(savedOrder, savedOrder.getItems());
    }

    private Map<Long, Course> findCourseMap(List<CartItem> cartItems) {
        List<Long> courseIds = cartItems.stream()
                .map(CartItem::getCourseId)
                .distinct()
                .toList();

        List<Course> courses = new ArrayList<>();
        courseRepository.findAllById(courseIds).forEach(courses::add);
        return courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
    }

    private Course getCourse(Map<Long, Course> courseMap, Long courseId) {
        Course course = courseMap.get(courseId);
        if (course == null) {
            throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void validateOrderableCourse(Course course) {
        // 장바구니에 담은 뒤 Course 상태가 바뀔 수 있으므로 주문 생성 직전에 다시 검증한다.
        if (course.getStatus() != CourseStatus.ON_SALE) {
            throw new CustomException(ErrorCode.COURSE_NOT_ON_SALE);
        }
    }

    private void validateNotAlreadyEnrolled(Long userId, List<Course> courses) {
        List<Long> courseIds = courses.stream()
                .map(Course::getId)
                .distinct()
                .toList();

        List<Long> existingCourseIds = enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                userId,
                courseIds
        );
        if (!existingCourseIds.isEmpty()) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_ENROLLED);
        }
    }

    private String createOrderNumber(LocalDateTime orderedAt) {
        return "ORD-" + orderedAt.format(ORDER_NUMBER_TIME_FORMATTER) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Order findMyOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Order findMyOrderForUpdate(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateCancelablePayment(Optional<Payment> payment) {
        payment.ifPresent(existingPayment -> {
            if (!existingPayment.canCancelBeforePaid()) {
                throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
            }
        });
    }
}
