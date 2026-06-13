package com.team08.backend.domain.fixture;

import com.team08.backend.domain.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {
    private Long id = 1L;
    private String email = "email@test.com";
    private String password = "password";
    private String nickname = "nickname";
    private String profileImage = "profileImage";

    public static UserFixture builder() {
        return new UserFixture();
    }

    public User build() {
        User user = User.createUser(email, password, nickname, profileImage);

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    public UserFixture email(String email) {
        this.email = email;
        return this;
    }

    public UserFixture password(String password) {
        this.password = password;
        return this;
    }
}
