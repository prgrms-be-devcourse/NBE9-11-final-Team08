package com.team08.backend.domain.order.service;

import com.team08.backend.domain.cart.entity.CartItem;
import com.team08.backend.domain.cart.repository.CartItemRepository;
import com.team08.backend.domain.cart.repository.CartRepository;
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
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional
    public OrderDetailResponse createFromCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        List<CartItem> cartItems = cartItemRepository.findAllByCartUserIdOrderByCreatedAtDesc(userId);

        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty.");
        }

        Integer totalPrice = cartItems.stream()
                .mapToInt(CartItem::getPrice)
                .sum();

        // TODO: Apply coupons after Coupon domain policies are finalized.
        Order order = orderRepository.save(Order.create(user, generateOrderNumber(), totalPrice, 0, clock));
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> OrderItem.create(order, cartItem.getCourse(), 0, clock))
                .toList();
        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteAllByCartUserId(userId);
        cartRepository.findByUserId(userId).ifPresent(cart -> cart.touch(clock));

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
            // TODO: Implement refund flow after PG/refund policies are finalized.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paid orders cannot be canceled in this MVP.");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order status cannot be canceled.");
        }

        order.cancel(clock);
        paymentRepository.findByOrderId(order.getId())
                .filter(payment -> payment.getStatus() == PaymentStatus.READY || payment.getStatus() == PaymentStatus.FAILED)
                .ifPresent(payment -> payment.cancel(clock));
        return toDetailResponse(order);
    }

    private Order getOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
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

    // TODO: Add direct single-course order creation if this remains in Issue #14 scope after API policy review.
}
