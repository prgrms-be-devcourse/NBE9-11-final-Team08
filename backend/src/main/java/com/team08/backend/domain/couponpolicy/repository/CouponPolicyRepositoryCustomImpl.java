package com.team08.backend.domain.couponpolicy.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicySearchRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponStatus;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.team08.backend.domain.couponpolicy.entity.QCouponPolicy.couponPolicy;
import static com.team08.backend.domain.couponpolicycategory.entity.QCouponPolicyCategory.couponPolicyCategory;

@Repository
public class CouponPolicyRepositoryCustomImpl implements CouponPolicyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CouponPolicyRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    // 관리자 쿠폰 정책 목록 조회 (필터링 및 페이징)
    @Override
    public Page<CouponPolicy> findAllByCondition(CouponPolicySearchRequest condition, LocalDateTime now, Pageable pageable) {
        List<CouponPolicy> content = queryFactory
                .selectFrom(couponPolicy)
                .where(
                        nameContains(condition.name()),
                        typeEq(condition.couponType()),
                        statusEq(condition.status(), now)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(couponPolicy.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(couponPolicy.count())
                .from(couponPolicy)
                .where(
                        nameContains(condition.name()),
                        typeEq(condition.couponType()),
                        statusEq(condition.status(), now)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 쿠폰 정책 상세 조회
    @Override
    public Optional<CouponPolicy> findByIdWithDetails(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(couponPolicy)
                .leftJoin(couponPolicy.targetCategories, couponPolicyCategory)
                .fetchJoin()
                .where(couponPolicy.id.eq(id))
                .distinct()
                .fetchOne());
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? couponPolicy.name.contains(name) : null;
    }

    private BooleanExpression typeEq(CouponType type) {
        return type != null ? couponPolicy.couponType.eq(type) : null;
    }

    private BooleanExpression statusEq(CouponStatus status, LocalDateTime now) {
        if (status == null) return null;

        return switch (status) {
            case SCHEDULED -> couponPolicy.issueStartDate.after(now);
            case ONGOING -> (couponPolicy.issueStartDate.isNull().or(couponPolicy.issueStartDate.loe(now)))
                    .and(couponPolicy.issueEndDate.isNull().or(couponPolicy.issueEndDate.goe(now)));
            case ENDED -> couponPolicy.issueEndDate.before(now);
        };
    }
}
