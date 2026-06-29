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
            int maxPoolSize = threadPoolExecutor.getMaximumPoolSize();
            int currentQueueSize = threadPoolExecutor.getQueue().size();
            int totalQueueCapacity = currentQueueSize + threadPoolExecutor.getQueue().remainingCapacity();
            log.error("[비동기 인코딩 작업 유실 발생] 인프라 임계치 초과(Pool: {}, Queue: {}/{}). 작업 요청이 거절되었습니다.",
                    maxPoolSize, currentQueueSize, totalQueueCapacity);
            throw new RejectedExecutionException("인코딩 처리 시스템이 혼잡하여 현재 요청을 수락할 수 없습니다. 잠시 후 다시 시도해주세요.");
        });

        executor.initialize();
        return executor;
    }

    @Bean(name = "videoCleanupExecutor")
    public Executor videoCleanupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("VideoCleanup-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            int maxPoolSize = threadPoolExecutor.getMaximumPoolSize();
            int currentQueueSize = threadPoolExecutor.getQueue().size();
            int totalQueueCapacity = currentQueueSize + threadPoolExecutor.getQueue().remainingCapacity();
            log.warn("[비동기 삭제 작업 거절] 인프라 임계치 초과(Pool: {}, Queue: {}/{}). 작업을 거절합니다.",
                    maxPoolSize, currentQueueSize, totalQueueCapacity);
            throw new RejectedExecutionException("삭제 처리 시스템 대기열이 가득 찼습니다.");
        });

        executor.initialize();
        return executor;
    }
}