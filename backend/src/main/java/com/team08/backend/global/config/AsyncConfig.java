package com.team08.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "videoEncodingExecutor")
    public Executor videoEncodingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("VideoAsync-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.error("[비동기 인코딩 스레드 풀 포화] 대기 큐(10) 및 최대 스레드(4) 임계치를 초과하여 인코딩 요청이 거절되었습니다. 인프라 확장 혹은 리소스 정리가 필요합니다.");
            throw new java.util.concurrent.RejectedExecutionException("Video encoding executor resources exhausted.");
        });

        executor.initialize();
        return executor;
    }
}