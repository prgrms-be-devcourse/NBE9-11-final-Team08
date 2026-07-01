package com.team08.backend.domain.course.service;
 
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
 
import java.util.concurrent.TimeUnit;
 
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseDetailCacheManager {
 
    private static final String CACHE_KEY_PREFIX = "course:detail:";
    private static final long CACHE_TTL_MINUTES = 30;
 
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public CourseDetailResponse getCache(Long courseId) {
        String key = CACHE_KEY_PREFIX + courseId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, CourseDetailResponse.class);
        } catch (JsonProcessingException e) {
            log.error("[Redis Deserialization Error] Data corruption for courseId: {}", courseId, e);
            evictCache(courseId);
            meterRegistry.counter("redis.cache.errors", "operation", "deserialize", "courseId", String.valueOf(courseId)).increment();
            return null;
        } catch (Exception e) {
            log.error("[Redis Read Error] Failed to read course detail cache for courseId: {}", courseId, e);
            meterRegistry.counter("redis.cache.errors", "operation", "read", "courseId", String.valueOf(courseId)).increment();
            return null;
        }
    }

    public void setCache(Long courseId, CourseDetailResponse response) {
        String key = CACHE_KEY_PREFIX + courseId;
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("[Redis Cache Write] Cached course detail for courseId: {}", courseId);
        } catch (Exception e) {
            log.error("[Redis Write Error] Failed to cache course detail for courseId: {}", courseId, e);
            meterRegistry.counter("redis.cache.errors", "operation", "write", "courseId", String.valueOf(courseId)).increment();
        }
    }

    public void evictCache(Long courseId) {
        String key = CACHE_KEY_PREFIX + courseId;
        try {
            redisTemplate.delete(key);
            log.debug("[Redis Cache Evict] Evicted course detail cache for courseId: {}", courseId);
        } catch (Exception e) {
            log.error("[Redis Evict Error] Failed to evict course detail cache for courseId: {}", courseId, e);
            meterRegistry.counter("redis.cache.errors", "operation", "evict", "courseId", String.valueOf(courseId)).increment();
        }
    }
}
