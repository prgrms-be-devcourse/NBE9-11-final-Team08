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
 
    /**
     * Redis 캐시에서 강좌 상세 정보를 조회합니다.
     * 장애 감내(Graceful Degradation)를 위해 모든 예외를 안전하게 catch하고 null을 반환합니다.
     * 예외 수치는 모니터링을 위해 Micrometer 메트릭 카운터에 기록합니다.
     */
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
            evictCache(courseId); // 손상된 캐시 즉시 제거
            meterRegistry.counter("redis.cache.errors", "operation", "deserialize", "courseId", String.valueOf(courseId)).increment();
            return null;
        } catch (Exception e) {
            log.error("[Redis Read Error] Failed to read course detail cache for courseId: {}", courseId, e);
            meterRegistry.counter("redis.cache.errors", "operation", "read", "courseId", String.valueOf(courseId)).increment();
            return null; // Redis 다운 시 RDB로 자연스럽게 복구되도록 null 반환
        }
    }
 
    /**
     * 강좌 상세 정보를 Redis 캐시에 기록합니다. (30분 TTL)
     */
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
 
    /**
     * 강좌 상세 캐시를 즉각 무효화(삭제)합니다. (Write 발생 시 호출)
     */
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
