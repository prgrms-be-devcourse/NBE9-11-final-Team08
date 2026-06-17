package com.team08.backend.global.init;

import com.team08.backend.global.init.DataSeeder.SeedConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class SimpleDataInitializer {

    /** 소량(개발용): 강사 2 · 수강생 4 · 강의 4(2x2) · 챕터 2 · 영상 2 · 쿠폰정책 2 */
    private static final SeedConfig CONFIG = new SeedConfig(2, 2, 2, 2, 4, 2, 500);

    private final DataSeeder dataSeeder;

    public void init() {
        dataSeeder.seed(CONFIG);
    }
}
