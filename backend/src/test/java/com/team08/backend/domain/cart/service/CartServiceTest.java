package com.team08.backend.domain.cart.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CART_ID = 10L;
    private static final Long COURSE_ID = 100L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void onSaleCourseCanBeAddedToCart() {
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        Cart cart = cart(CART_ID, USER_ID);
        CartItem savedItem = cartItem(1L, CART_ID, COURSE_ID);

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE))
                .willReturn(false);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(cartRepository.save(any(Cart.class))).willReturn(cart);
        given(cartItemRepository.existsByCartIdAndCourseId(CART_ID, COURSE_ID)).willReturn(false);
        given(cartItemRepository.findAllByCartId(CART_ID)).willReturn(List.of(savedItem));
        given(courseRepository.findAllById(any())).willReturn(List.of(course));

        CartResponse response = cartService.addItem(USER_ID, COURSE_ID);

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).saveAndFlush(cartItemCaptor.capture());

        CartItem cartItem = cartItemCaptor.getValue();
        assertThat(cartItem.getCartId()).isEqualTo(CART_ID);
        assertThat(cartItem.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).courseId()).isEqualTo(COURSE_ID);
        assertThat(response.items().get(0).title()).isEqualTo("Spring");
        assertThat(response.items().get(0).price()).isEqualTo(30_000);
        assertThat(response.totalPrice()).isEqualTo(30_000);
    }

    @Test
    void nonOnSaleCourseCannotBeAddedToCart() {
        Course course = course(COURSE_ID, "Draft", 30_000, CourseStatus.DRAFT);
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, COURSE_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_ON_SALE));

        verifyNoInteractions(cartRepository, cartItemRepository, enrollmentRepository);
    }

    @Test
    void activeEnrollmentPreventsAddingCourseToCart() {
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE))
                .willReturn(true);

        assertThatThrownBy(() -> cartService.addItem(USER_ID, COURSE_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ALREADY_ENROLLED));

        verifyNoInteractions(cartRepository, cartItemRepository);
    }

    @Test
    void duplicateCourseCannotBeAddedToCart() {
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        Cart cart = cart(CART_ID, USER_ID);

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE))
                .willReturn(false);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(cartItemRepository.existsByCartIdAndCourseId(CART_ID, COURSE_ID)).willReturn(true);

        assertThatThrownBy(() -> cartService.addItem(USER_ID, COURSE_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ALREADY_IN_CART));
    }

    @Test
    void duplicateCourseByConcurrentRequestIsConvertedToCustomException() {
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        Cart cart = cart(CART_ID, USER_ID);

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE))
                .willReturn(false);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(cartItemRepository.existsByCartIdAndCourseId(CART_ID, COURSE_ID)).willReturn(false);
        given(cartItemRepository.saveAndFlush(any(CartItem.class)))
                .willThrow(new DataIntegrityViolationException("duplicate cart item"));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, COURSE_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ALREADY_IN_CART));
    }

    @Test
    void getCartReturnsItemsAndTotalPrice() {
        Cart cart = cart(CART_ID, USER_ID);
        CartItem firstItem = cartItem(1L, CART_ID, 100L);
        CartItem secondItem = cartItem(2L, CART_ID, 200L);
        Course firstCourse = course(100L, "Spring", 30_000, CourseStatus.ON_SALE);
        Course secondCourse = course(200L, "JPA", 20_000, CourseStatus.ON_SALE);

        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(cartItemRepository.findAllByCartId(CART_ID)).willReturn(List.of(firstItem, secondItem));
        given(courseRepository.findAllById(any())).willReturn(List.of(firstCourse, secondCourse));

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalPrice()).isEqualTo(50_000);
    }

    @Test
    void removeMyCartItemDeletesItem() {
        Cart cart = cart(CART_ID, USER_ID);
        CartItem cartItem = cartItem(1L, CART_ID, COURSE_ID);

        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByIdAndCartId(1L, CART_ID)).willReturn(Optional.of(cartItem));
        given(cartItemRepository.findAllByCartId(CART_ID)).willReturn(List.of());

        CartResponse response = cartService.removeItem(USER_ID, 1L);

        verify(cartItemRepository).delete(cartItem);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isZero();
    }

    @Test
    void removingCartItemNotInMyCartFails() {
        Cart myCart = cart(CART_ID, USER_ID);

        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(myCart));
        given(cartItemRepository.findByIdAndCartId(1L, CART_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(USER_ID, 1L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Test
    void clearCartDeletesOnlyCartItems() {
        Cart cart = cart(CART_ID, USER_ID);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(USER_ID);

        verify(cartItemRepository).deleteAllByCartId(CART_ID);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isZero();
    }

    @Test
    void getCartReturnsEmptyResponseWhenCartDoesNotExist() {
        given(cartRepository.findByUserId(OTHER_USER_ID)).willReturn(Optional.empty());

        CartResponse response = cartService.getCart(OTHER_USER_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isZero();
    }

    private Cart cart(Long cartId, Long userId) {
        return new Cart(cartId, userId);
    }

    private CartItem cartItem(Long cartItemId, Long cartId, Long courseId) {
        return new CartItem(cartItemId, cartId, courseId);
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
