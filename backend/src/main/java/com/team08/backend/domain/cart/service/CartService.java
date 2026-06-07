package com.team08.backend.domain.cart.service;

import com.team08.backend.domain.cart.dto.CartItemResponse;
import com.team08.backend.domain.cart.dto.CartResponse;
import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.entity.CartItem;
import com.team08.backend.domain.cart.repository.CartItemRepository;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Transactional
    public CartResponse addItem(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."));

        // TODO: Course 도메인 정책 확정 후 판매중/삭제 여부 검증을 Course 서비스로 위임합니다.
        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 수강 중인 강의입니다.");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.create(user, clock)));

        if (cartItemRepository.existsByCartIdAndCourseId(cart.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 장바구니에 담긴 강의입니다.");
        }

        cartItemRepository.save(CartItem.create(cart, course, clock));
        cart.touch(clock);

        return getMyCart(userId);
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart(Long userId) {
        List<CartItemResponse> items = cartItemRepository.findAllByCartUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CartItemResponse::from)
                .toList();

        return CartResponse.from(items);
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."));

        cartItemRepository.delete(cartItem);
        cartRepository.findByUserId(userId).ifPresent(cart -> cart.touch(clock));
    }

    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteAllByCartUserId(userId);
        cartRepository.findByUserId(userId).ifPresent(cart -> cart.touch(clock));
    }
}
