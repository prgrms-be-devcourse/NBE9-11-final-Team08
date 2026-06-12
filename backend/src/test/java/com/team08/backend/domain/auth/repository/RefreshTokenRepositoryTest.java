package com.team08.backend.domain.auth.repository;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRepositoryTest {

    @Test
    void refreshToken_조회는_동시_재사용을_막기_위해_쓰기_잠금을_사용한다() throws Exception {
        Lock lock = RefreshTokenRepository.class
                .getMethod("findByTokenHashForUpdate", String.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
