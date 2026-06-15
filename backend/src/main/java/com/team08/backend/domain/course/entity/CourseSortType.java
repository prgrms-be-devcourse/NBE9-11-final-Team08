package com.team08.backend.domain.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@Getter
@RequiredArgsConstructor
public enum CourseSortType {

    TITLE_ASC("title", Sort.Direction.ASC),
    PRICE_ASC("price", Sort.Direction.ASC),
    CREATED_DESC("createdAt", Sort.Direction.DESC),
    VIEW_DESC("viewCount", Sort.Direction.DESC);

    private final String property;
    private final Sort.Direction direction;

    public Sort getSort() {
        return Sort.by(direction, property)
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}