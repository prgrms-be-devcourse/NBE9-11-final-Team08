package com.team08.backend.domain.user.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    private String profileImage;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private UserRole role;

    private User(String email, String password, String nickname, String profileImage, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.role = role;
    }

    public static User createUser(String email, String password, String nickname, String profileImage) {
        return new User(email, password, nickname, profileImage, UserRole.ROLE_USER);
    }

    public static User createSeller(String email, String password, String nickname, String profileImage) {
        return new User(email, password, nickname, profileImage, UserRole.ROLE_SELLER);
    }

    // 더미데이터 생성용, 비즈니스 로직에선 안쓰임
    public static User createAdmin(String email, String password, String nickname, String profileImage) {
        return new User(email, password, nickname, profileImage, UserRole.ROLE_ADMIN);
    }
}
