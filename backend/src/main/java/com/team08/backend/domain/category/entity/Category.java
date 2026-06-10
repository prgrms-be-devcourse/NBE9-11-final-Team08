package com.team08.backend.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long parentCategoryId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private Integer depth;
}
