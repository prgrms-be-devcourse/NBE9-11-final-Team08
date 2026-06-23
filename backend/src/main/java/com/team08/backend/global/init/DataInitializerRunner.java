package com.team08.backend.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializerRunner implements CommandLineRunner {

    @Value("${app.data-init.mode:none}")
    private String mode;

    private final SimpleDataInitializer simpleDataInitializer;
    private final BulkDataInitializer bulkDataInitializer;
    private final DemoDataInitializer demoDataInitializer;

    @Override
    public void run(String... args) {
        switch (mode.toLowerCase()) {
            case "simple" -> {
                log.info("[DataInit] 간이 데이터 초기화 시작");
                simpleDataInitializer.init();
                log.info("[DataInit] 간이 데이터 초기화 완료");
            }
            case "demo" -> {
                log.info("[DataInit] 시연용 데이터 초기화 시작");
                demoDataInitializer.init();
                log.info("[DataInit] 시연용 데이터 초기화 완료");
            }
            case "bulk" -> {
                log.info("[DataInit] 대량 데이터 초기화 시작");
                bulkDataInitializer.init();
                log.info("[DataInit] 대량 데이터 초기화 완료");
            }
            default -> log.info("[DataInit] 데이터 초기화 건너뜀 (mode={})", mode);
        }
    }
}
