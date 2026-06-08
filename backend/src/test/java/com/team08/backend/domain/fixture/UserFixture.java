package com.team08.backend.domain.fixture;

import com.team08.backend.domain.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {
    private UserFixture() {
    }

    public static User user() {
        return User.create(
                "test@example.com",
                "1234",
                "테스트 유저"
        );
    }

    public static User user(Long id) {
        User user = user();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}
