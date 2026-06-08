package com.team08.backend.domain.order.service;

import com.team08.backend.domain.cart.entity.CartItem;
import com.team08.backend.domain.cart.repository.CartItemRepository;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.dto.OrderDetailResponse;
import com.team08.backend.domain.order.dto.OrderItemResponse;
import com.team08.backend.domain.order.dto.OrderSummaryResponse;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderItem;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderItemRepository;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional
    public OrderDetailResponse createFromCart(Long userId) {
        User user = getUser(userId);
        List<CartItem> cartItems = cartItemRepository.findAllByCartUserIdOrderByCreatedAtDesc(userId);

        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다.");
        }

        List<Course> courses = cartItems.stream()
                .map(CartItem::getCourse)
                .toList();
        Integer totalPrice = cartItems.stream()
                .mapToInt(CartItem::getPrice)
                .sum();
        Order order = createOrder(user, courses, totalPrice);

        cartItemRepository.deleteAllByCartUserId(userId);
        cartRepository.findByUserId(userId).ifPresent(cart -> cart.touch(clock));

        return toDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse createDirect(Long userId, Long courseId) {
        User user = getUser(userId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."));

        // TODO: Course 도메인의 판매 가능/삭제/무료 강의 정책이 확정되면 바로 주문 검증에 반영한다.
        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 수강 중인 강의입니다.");
        }

        Order order = createOrder(user, List.of(course), course.getPrice());
        return toDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getMyOrders(Long userId) {
        return orderRepository.findAllByUserIdOrderByOrderedAtDesc(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrder(Long userId, Long orderId) {
        Order order = getOrder(userId, orderId);
        return toDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse cancel(Long userId, Long orderId) {
        Order order = getOrder(userId, orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            // TODO: 실제 PG 환불 정책이 확정되면 결제 취소와 수강권 회수를 함께 처리한다.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 완료 주문은 MVP에서 취소할 수 없습니다.");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "취소할 수 없는 주문 상태입니다.");
        }

        order.cancel(clock);
        // 주문과 결제 상태가 엇갈리지 않도록 미완료 결제는 함께 취소한다.
        paymentRepository.findByOrderId(order.getId())
                .filter(payment -> payment.getStatus() == PaymentStatus.READY || payment.getStatus() == PaymentStatus.FAILED)
                .ifPresent(payment -> payment.cancel(clock));
        return toDetailResponse(order);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Order getOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    private Order createOrder(User user, List<Course> courses, Integer totalPrice) {
        // TODO: Coupon 도메인의 할인 정책이 확정되면 주문 생성 시 쿠폰 검증과 할인 계산을 연동한다.
        Order order = orderRepository.save(Order.create(user, generateOrderNumber(), totalPrice, 0, clock));
        List<OrderItem> orderItems = courses.stream()
                .map(course -> OrderItem.create(order, course, 0, clock))
                .toList();
        orderItemRepository.saveAll(orderItems);
        return order;
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findAllByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderDetailResponse.of(order, items);
    }

    private String generateOrderNumber() {
        String timestamp = ORDER_NUMBER_DATE_FORMAT.format(java.time.LocalDateTime.now(clock));
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + timestamp + "-" + suffix;
    }
}
