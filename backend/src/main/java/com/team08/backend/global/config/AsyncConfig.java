package com.team08.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "videoEncodingExecutor")
    public Executor videoEncodingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("VideoAsync-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.error("[비동기 인코딩 작업 유실 발생] 인프라 임계치 초과(Pool: 4, Queue: 100). 작업 요청이 거절되었습니다.");
            throw new RejectedExecutionException("인코딩 처리 시스템이 혼잡하여 현재 요청을 수락할 수 없습니다. 잠시 후 다시 시도해주세요.");
        });

        executor.initialize();
        return executor;
    }
}