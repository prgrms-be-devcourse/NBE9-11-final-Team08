package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicycategory.entity.CouponPolicyCategory;
import com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class CouponPolicyUpdater {

    public void updateTargets(CouponPolicy policy, List<Long> categoryIds, List<Long> courseIds) {
        // 카테고리 대상 업데이트
        List<Long> currentCategoryIds = policy.getTargetCategories().stream()
                .map(CouponPolicyCategory::getCategoryId)
                .toList();
        updateCollection(currentCategoryIds, categoryIds, policy::addTargetCategory, policy::removeTargetCategory);

        // 강좌 대상 업데이트
        List<Long> currentCourseIds = policy.getTargetCourses().stream()
                .map(CouponPolicyCourse::getCourseId)
                .toList();
        updateCollection(currentCourseIds, courseIds, policy::addTargetCourse, policy::removeTargetCourse);
    }

    private void updateCollection(
            List<Long> currentIds,
            List<Long> newIds,
            Consumer<Long> adder,
            Consumer<Long> remover
    ) {
        if (newIds == null || newIds.isEmpty()) {
            currentIds.forEach(remover);
            return;
        }

        Set<Long> currentIdSet = new HashSet<>(currentIds);
        Set<Long> newIdSet = new HashSet<>(newIds);

        // 삭제할 항목
        currentIds.stream()
                .filter(id -> !newIdSet.contains(id))
                .forEach(remover);

        // 추가할 항목
        newIds.stream()
                .filter(id -> !currentIdSet.contains(id))
                .forEach(adder);
    }
}
