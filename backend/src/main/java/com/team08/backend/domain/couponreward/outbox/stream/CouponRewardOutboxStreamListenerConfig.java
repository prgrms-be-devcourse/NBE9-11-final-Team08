package com.team08.backend.domain.couponreward.outbox.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.util.ErrorHandler;
import io.lettuce.core.RedisException;

import java.time.Duration;

@Configuration
public class CouponRewardOutboxStreamListenerConfig {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CouponRewardOutboxStreamListenerConfig.class);
    private static final ErrorHandler REDIS_STREAM_ERROR_HANDLER = error -> {
        if (isRedisConnectionError(error)) {
            log.warn("Redis Stream polling interrupted. The listener will keep polling after Redis is available again. message={}",
                    error.getMessage());
            return;
        }

        log.error("Unexpected error occurred while polling Redis Stream.", error);
    };

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> couponRewardOutboxStreamContainer(
            RedisConnectionFactory connectionFactory,
            CouponRewardOutboxStreamWorker worker
    ) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>>
                options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(POLL_TIMEOUT)
                .errorHandler(REDIS_STREAM_ERROR_HANDLER)
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        container.receive(
                Consumer.from(CouponRewardOutboxStreamWorker.GROUP_NAME, worker.consumerName()),
                StreamOffset.create(CouponRewardOutboxStreamPublishListener.STREAM_KEY, ReadOffset.lastConsumed()),
                worker
        );
        container.start();

        return container;
    }

    private static boolean isRedisConnectionError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof RedisConnectionFailureException
                    || current instanceof RedisSystemException
                    || current instanceof QueryTimeoutException
                    || current instanceof RedisException
                    || hasRedisConnectionMessage(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasRedisConnectionMessage(Throwable error) {
        String message = error.getMessage();
        return message != null && (
                message.contains("Connection closed")
                        || message.contains("Command timed out")
                        || message.contains("Connection reset")
        );
    }
}
