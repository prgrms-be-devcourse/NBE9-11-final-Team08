package com.team08.backend.domain.cart.service;

import com.team08.backend.domain.cart.dto.CartItemResponse;
import com.team08.backend.domain.cart.dto.CartResponse;
import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.cartitem.entity.CartItem;
import com.team08.backend.domain.cartitem.repository.CartItemRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public CartResponse addItem(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.ON_SALE) {
            throw new CustomException(ErrorCode.COURSE_NOT_ON_SALE);
        }

        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE)) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_ENROLLED);
        }

        Cart cart = getOrCreateCart(userId);

        if (cartItemRepository.existsByCartIdAndCourseId(cart.getId(), courseId)) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_IN_CART);
        }

        saveCartItem(cart.getId(), courseId);

        return toResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(cartItem);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> {
                    cartItemRepository.deleteAllByCartId(cart.getId());
                    return CartResponse.empty();
                })
                .orElseGet(CartResponse::empty);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(null, userId)));
    }

    private void saveCartItem(Long cartId, Long courseId) {
        try {
            cartItemRepository.saveAndFlush(new CartItem(null, cartId, courseId));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_IN_CART);
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            return CartResponse.empty();
        }

        List<Long> courseIds = cartItems.stream()
                .map(CartItem::getCourseId)
                .distinct()
                .toList();

        List<Course> courses = new ArrayList<>();
        courseRepository.findAllById(courseIds).forEach(courses::add);

        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        List<CartItemResponse> items = cartItems.stream()
                .map(cartItem -> toItemResponse(cartItem, courseMap.get(cartItem.getCourseId())))
                .filter(item -> item != null)
                .toList();

        int totalPrice = items.stream()
                .mapToInt(CartItemResponse::price)
                .sum();

        return new CartResponse(items, totalPrice);
    }

    private CartItemResponse toItemResponse(CartItem cartItem, Course course) {
        if (course == null) {
            return null;
        }

        return new CartItemResponse(
                cartItem.getId(),
                course.getId(),
                course.getTitle(),
                course.getPrice()
        );
    }
}
