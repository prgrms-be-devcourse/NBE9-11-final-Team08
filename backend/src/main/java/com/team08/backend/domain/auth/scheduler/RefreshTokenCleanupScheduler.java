package com.team08.backend.domain.auth.scheduler;

import com.team08.backend.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Scheduled(cron = "${app.jwt.refresh-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredRefreshTokens() {
        refreshTokenRepository.deleteByExpiresAtLessThanEqual(LocalDateTime.now(clock));
    }
}
