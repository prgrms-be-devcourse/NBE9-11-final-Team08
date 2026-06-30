package com.team08.backend.global.redis.stream;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

public abstract class AbstractStreamListenerWorker implements StreamListener<String, MapRecord<String, String, String>> {

    protected final StringRedisTemplate redisTemplate;

    protected AbstractStreamListenerWorker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    protected abstract String getStreamKey();
    protected abstract String getGroupName();

    @jakarta.annotation.PostConstruct
    public void createConsumerGroup() {
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    getStreamKey().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    getGroupName().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "0".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ));
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        try {
            processRecord(record);
            ack(record);
        } catch (Exception e) {
            handleError(record, e);
        }
    }

    protected abstract void processRecord(MapRecord<String, String, String> record);

    protected void ack(MapRecord<String, String, String> record) {
        redisTemplate.opsForStream().acknowledge(getStreamKey(), getGroupName(), record.getId());
    }

    protected void handleError(MapRecord<String, String, String> record, Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                "Redis Stream Listener 처리 실패. (PEL 대기) stream={}, group={}, recordId={}", 
                getStreamKey(), getGroupName(), record.getId(), e);
    }
}
