package com.team08.backend.domain.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseSortType {

    TITLE_ASC("title"),
    PRICE_ASC("price"),
    CREATED_DESC("createdAt"),
    VIEW_DESC("viewCount");

    private final String property;
}