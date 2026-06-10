package com.team08.backend.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "studies")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long courseId;
    @Column(nullable = false)
    private Long ownerId;
    @Column(nullable = false, length = 255)
    private String title;
    @Lob
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StudyStatus status;
}
