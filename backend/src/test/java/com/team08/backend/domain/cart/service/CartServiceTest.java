package com.team08.backend.domain.cart.service;

import com.team08.backend.domain.cart.dto.CartResponse;
import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.cartitem.entity.CartItem;
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
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void onSaleCourseCanBeAddedToCart() {
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        Cart cart = cart(CART_ID, USER_ID);

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE))
                .willReturn(false);
        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.empty());
        given(courseRepository.findAllById(any())).willReturn(List.of(course));

        CartResponse response = cartService.addItem(USER_ID, COURSE_ID);

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).saveAndFlush(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.getUserId()).isEqualTo(USER_ID);
        assertThat(savedCart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getCourseId()).isEqualTo(COURSE_ID));
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

        verifyNoInteractions(cartRepository, enrollmentRepository);
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

        verifyNoInteractions(cartRepository);
    }

    @Test
    void duplicateCourseCannotBeAddedToCart() {
        Course course = course(COURSE_ID, "Spring", 30_000, CourseStatus.ON_SALE);
        Cart cart = cart(CART_ID, USER_ID, cartItem(1L, COURSE_ID));

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE))
                .willReturn(false);
        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, COURSE_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ALREADY_IN_CART));
    }

    @Test
    void getCartReturnsItemsAndTotalPrice() {
        CartItem firstItem = cartItem(1L, 100L);
        CartItem secondItem = cartItem(2L, 200L);
        Cart cart = cart(CART_ID, USER_ID, firstItem, secondItem);
        Course firstCourse = course(100L, "Spring", 30_000, CourseStatus.ON_SALE);
        Course secondCourse = course(200L, "JPA", 20_000, CourseStatus.ON_SALE);

        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));
        given(courseRepository.findAllById(any())).willReturn(List.of(firstCourse, secondCourse));

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalPrice()).isEqualTo(50_000);
    }

    @Test
    void removeMyCartItemDeletesItem() {
        Cart cart = cart(CART_ID, USER_ID, cartItem(1L, COURSE_ID));

        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));

        CartResponse response = cartService.removeItem(USER_ID, 1L);

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isZero();
    }

    @Test
    void removingCartItemNotInMyCartFails() {
        Cart myCart = cart(CART_ID, USER_ID);

        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(myCart));

        assertThatThrownBy(() -> cartService.removeItem(USER_ID, 1L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Test
    void clearCartDeletesOnlyCartItems() {
        Cart cart = cart(CART_ID, USER_ID, cartItem(1L, COURSE_ID));
        given(cartRepository.findByUserIdWithItems(USER_ID)).willReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(USER_ID);

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isZero();
    }

    @Test
    void getCartReturnsEmptyResponseWhenCartDoesNotExist() {
        given(cartRepository.findByUserIdWithItems(OTHER_USER_ID)).willReturn(Optional.empty());

        CartResponse response = cartService.getCart(OTHER_USER_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isZero();
    }

    @Test
    void clearCartReturnsEmptyResponseWhenCartDoesNotExist() {
        given(cartRepository.findByUserIdWithItems(OTHER_USER_ID)).willReturn(Optional.empty());

        CartResponse response = cartService.clearCart(OTHER_USER_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isZero();
    }

    private Cart cart(Long cartId, Long userId, CartItem... items) {
        Cart cart = Cart.create(userId);
        ReflectionTestUtils.setField(cart, "id", cartId);
        for (CartItem item : items) {
            ReflectionTestUtils.setField(item, "cart", cart);
            cart.getItems().add(item);
        }
        return cart;
    }

    private CartItem cartItem(Long cartItemId, Long courseId) {
        CartItem cartItem = newInstance(CartItem.class);
        ReflectionTestUtils.setField(cartItem, "id", cartItemId);
        ReflectionTestUtils.setField(cartItem, "courseId", courseId);
        return cartItem;
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
