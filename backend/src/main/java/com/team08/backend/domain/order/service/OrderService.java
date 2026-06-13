package com.team08.backend.domain.order.service;

import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.cartitem.entity.CartItem;
import com.team08.backend.domain.cartitem.repository.CartItemRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.dto.OrderDetailResponse;
import com.team08.backend.domain.order.dto.OrderSummaryResponse;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public OrderDetailResponse createOrderFromCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPTY_CART));

        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_CART);
        }

        Map<Long, Course> courseMap = findCourseMap(cartItems);
        List<Course> orderCourses = cartItems.stream()
                .map(cartItem -> getCourse(courseMap, cartItem.getCourseId()))
                .toList();

        OrderDetailResponse response = createPendingPaymentOrder(userId, orderCourses);

        // MVP 정책상 장바구니 주문이 성공하면 재주문 방지를 위해 주문에 사용한 장바구니 항목을 비웁니다.
        cartItemRepository.deleteAllByCartId(cart.getId());

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
        Order order = findMyOrder(userId, orderId);

        order.cancel(LocalDateTime.now());

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        return OrderDetailResponse.from(order, orderItems);
    }

    private OrderDetailResponse createPendingPaymentOrder(Long userId, List<Course> courses) {
        courses.forEach(course -> validateOrderableCourse(userId, course));

        int totalPrice = courses.stream()
                .mapToInt(Course::getPrice)
                .sum();
        int discountPrice = 0;
        int finalPrice = totalPrice - discountPrice;
        LocalDateTime orderedAt = LocalDateTime.now();

        // 이번 이슈는 결제 연동 전 단계이므로 주문은 결제 대기 상태까지만 생성합니다.
        Order order = orderRepository.save(Order.createPendingPayment(
                userId,
                createOrderNumber(orderedAt),
                totalPrice,
                discountPrice,
                finalPrice,
                orderedAt
        ));

        List<OrderItem> orderItems = courses.stream()
                .map(course -> toOrderItem(order.getId(), course, orderedAt))
                .toList();
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        return OrderDetailResponse.from(order, savedItems);
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

    private void validateOrderableCourse(Long userId, Course course) {
        // 장바구니에 담은 뒤 판매 상태가 바뀔 수 있으므로 주문 생성 직전에 Course 상태를 다시 검증합니다.
        if (course.getStatus() != CourseStatus.ON_SALE) {
            throw new CustomException(ErrorCode.COURSE_NOT_ON_SALE);
        }

        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, course.getId(), EnrollmentStatus.ACTIVE)) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_ENROLLED);
        }
    }

    private OrderItem toOrderItem(Long orderId, Course course, LocalDateTime createdAt) {
        int discountPrice = 0;
        int finalPrice = course.getPrice() - discountPrice;

        // 강의명과 가격은 이후 Course가 수정되어도 주문 내역이 변하지 않도록 주문 시점 값으로 저장합니다.
        return OrderItem.createSnapshot(
                orderId,
                course.getId(),
                course.getTitle(),
                course.getPrice(),
                discountPrice,
                finalPrice,
                createdAt
        );
    }

    private String createOrderNumber(LocalDateTime orderedAt) {
        return "ORD-" + orderedAt.format(ORDER_NUMBER_TIME_FORMATTER) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Order findMyOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }
}
