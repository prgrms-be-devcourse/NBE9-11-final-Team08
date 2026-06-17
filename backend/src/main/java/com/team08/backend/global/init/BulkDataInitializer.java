package com.team08.backend.global.init;

import com.team08.backend.global.init.DataSeeder.SeedConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class BulkDataInitializer {

    /** 대량(성능 테스트용): seedConfig */
    private static final SeedConfig CONFIG = new SeedConfig(20, 5, 10, 3, 1000, 50, 500);

    private final DataSeeder dataSeeder;

    public void init() {
        dataSeeder.seed(CONFIG);
    }
}
