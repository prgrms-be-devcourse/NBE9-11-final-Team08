package com.team08.backend.domain.course.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Component
public class CurriculumValidator {

    public void validateSize(int dbSize, int requestSize) {
        if (dbSize != requestSize) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }
    }

    public void validateIds(Set<Long> dbIds, Set<Long> requestIds) {
        if (!dbIds.equals(requestIds)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }
    }

    public void validateOrderSequence(List<Integer> sortedOrders) {
        if (new HashSet<>(sortedOrders).size() != sortedOrders.size()) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }

        boolean isInvalid = IntStream.range(0, sortedOrders.size())
                .anyMatch(i -> sortedOrders.get(i) != i + 1);

        if (isInvalid) {
            throw new CustomException(ErrorCode.INVALID_ORDER_REQUEST);
        }
    }
}