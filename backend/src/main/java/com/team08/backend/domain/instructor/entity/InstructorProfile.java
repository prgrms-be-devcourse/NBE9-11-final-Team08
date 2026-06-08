package com.team08.backend.domain.instructor.entity;

import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "instructor_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstructorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String introduction;

    @Column(nullable = false)
    private String bankAccount;

    private LocalDateTime approvedAt;

    @Builder
    public InstructorProfile(User user, String introduction, String bankAccount) {
        this.user = user;
        this.introduction = introduction;
        this.bankAccount = bankAccount;
        this.approvedAt = LocalDateTime.now(); // Todo: MVP 단계이므로 생성 시 자동 승인 처리 (필요시 관리자 로직 전환 가능)
    }
}