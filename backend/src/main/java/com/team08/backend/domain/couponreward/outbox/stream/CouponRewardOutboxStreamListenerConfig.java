package com.team08.backend.domain.couponreward.outbox.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class CouponRewardOutboxStreamListenerConfig {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> couponRewardOutboxStreamContainer(
            RedisConnectionFactory connectionFactory,
            CouponRewardOutboxStreamWorker worker
    ) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>>
                options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(POLL_TIMEOUT)
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
}
