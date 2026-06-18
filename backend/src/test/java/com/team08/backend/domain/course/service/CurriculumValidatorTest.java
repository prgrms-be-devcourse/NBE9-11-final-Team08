package com.team08.backend.domain.course.service;

import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurriculumValidatorTest {

    private final CurriculumValidator curriculumValidator = new CurriculumValidator();

    @Test
    void DB_개수와_요청_개수가_같으면_예외가_발생하지_않는다() {
        assertThatCode(() -> curriculumValidator.validateSize(3, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void DB_개수와_요청_개수가_다르면_예외가_발생한다() {
        assertThatThrownBy(() -> curriculumValidator.validateSize(3, 2))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void DB_id집합과_요청_id집합이_같으면_예외가_발생하지_않는다() {
        Set<Long> dbIds = Set.of(1L, 2L, 3L);
        Set<Long> requestIds = Set.of(1L, 2L, 3L);

        assertThatCode(() -> curriculumValidator.validateIds(dbIds, requestIds))
                .doesNotThrowAnyException();
    }

    @Test
    void DB_id집합과_요청_id집합이_다르면_예외가_발생한다() {
        Set<Long> dbIds = Set.of(1L, 2L, 3L);
        Set<Long> requestIds = Set.of(1L, 2L, 999L);

        assertThatThrownBy(() -> curriculumValidator.validateIds(dbIds, requestIds))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void DB_id집합과_요청_id집합의_개수는_같지만_원소가_다르면_예외가_발생한다() {
        Set<Long> dbIds = Set.of(1L, 2L);
        Set<Long> requestIds = Set.of(1L, 3L);

        assertThatThrownBy(() -> curriculumValidator.validateIds(dbIds, requestIds))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 순서가_1부터_시작하는_연속된_정수이면_예외가_발생하지_않는다() {
        List<Integer> sortedOrders = List.of(1, 2, 3);

        assertThatCode(() -> curriculumValidator.validateOrderSequence(sortedOrders))
                .doesNotThrowAnyException();
    }

    @Test
    void 순서가_1이_아닌_값부터_시작하면_예외가_발생한다() {
        List<Integer> sortedOrders = List.of(2, 3, 4);

        assertThatThrownBy(() -> curriculumValidator.validateOrderSequence(sortedOrders))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 순서_중간에_숫자가_비어있으면_예외가_발생한다() {
        List<Integer> sortedOrders = List.of(1, 2, 4);

        assertThatThrownBy(() -> curriculumValidator.validateOrderSequence(sortedOrders))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 순서_번호가_중복되면_예외가_발생한다() {
        List<Integer> sortedOrders = List.of(1, 1, 2);

        assertThatThrownBy(() -> curriculumValidator.validateOrderSequence(sortedOrders))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 단일_원소만_있고_1이면_예외가_발생하지_않는다() {
        List<Integer> sortedOrders = List.of(1);

        assertThatCode(() -> curriculumValidator.validateOrderSequence(sortedOrders))
                .doesNotThrowAnyException();
    }
}