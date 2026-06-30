package com.team08.backend.domain.course.service;
 
import com.team08.backend.domain.course.repository.CourseRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.Set;
import java.util.concurrent.TimeUnit;
 
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseViewCountRedisManager {
 
    private static final String KEY_PREFIX = "course:viewcount:delta:";
    private static final String KEY_SET_PREFIX = "course:viewcount:active_ids";
 
    private final StringRedisTemplate redisTemplate;
    private final CourseRepository courseRepository;
    private final CourseDetailCacheManager courseDetailCacheManager;
    private final MeterRegistry meterRegistry;
 

    public void increaseViewCount(Long courseId) {
        try {
            String key = KEY_PREFIX + courseId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            redisTemplate.opsForSet().add(KEY_SET_PREFIX, String.valueOf(courseId));
        } catch (Exception e) {
            meterRegistry.counter("redis.viewcount.errors", "operation", "increase").increment();
            throw e;
        }
    }
 

    public int getViewCountDelta(Long courseId) {
        try {
            String key = KEY_PREFIX + courseId;
            String val = redisTemplate.opsForValue().get(key);
            return val != null ? Integer.parseInt(val) : 0;
        } catch (NumberFormatException e) {
            return 0;
        } catch (Exception e) {
            meterRegistry.counter("redis.viewcount.errors", "operation", "getDelta").increment();
            return 0;
        }
    }
 

    @Transactional
    @Scheduled(fixedDelay = 10000)
    public void syncViewCountsToDb() {
        Boolean hasKey = false;
        try {
            hasKey = redisTemplate.hasKey(KEY_SET_PREFIX);
        } catch (Exception e) {
            log.error("[조회수 동기화] 활성 ID 키 존재 여부 확인 실패", e);
            meterRegistry.counter("redis.viewcount.errors", "operation", "sync_check_key").increment();
            return;
        }

        if (hasKey == null || !hasKey) {
            return;
        }

        String tempKey = KEY_SET_PREFIX + ":temp:" + System.currentTimeMillis();
        Set<String> activeIds = null;
        try {
            redisTemplate.rename(KEY_SET_PREFIX, tempKey);
            activeIds = redisTemplate.opsForSet().members(tempKey);
        } catch (Exception e) {
            log.error("[조회수 동기화] 활성 ID 임시 격리 및 조회 실패", e);
            meterRegistry.counter("redis.viewcount.errors", "operation", "sync_isolate_ids").increment();
            return;
        }
 
        if (activeIds == null || activeIds.isEmpty()) {
            redisTemplate.delete(tempKey);
            return;
        }
 
        for (String idStr : activeIds) {
            String key = KEY_PREFIX + idStr;
            try {
                String deltaStr = redisTemplate.opsForValue().getAndSet(key, "0");
                if (deltaStr != null) {
                    int delta = Integer.parseInt(deltaStr);
                    if (delta > 0) {
                        Long courseId = Long.parseLong(idStr);
                        courseRepository.increaseViewCountByDelta(courseId, delta);
                        courseDetailCacheManager.evictCache(courseId);
                        log.debug("[조회수 동기화 완료] Course: {}, Delta: {}", courseId, delta);
                    }
                }
            } catch (Exception e) {
                log.error("[조회수 동기화 실패] CourseId: {}", idStr, e);
                meterRegistry.counter("redis.viewcount.errors", "operation", "sync_write_db").increment();
            }
        }

        try {
            redisTemplate.delete(tempKey);
        } catch (Exception e) {
            log.error("[조회수 동기화] 임시 격리 키 삭제 실패: {}", tempKey, e);
            meterRegistry.counter("redis.viewcount.errors", "operation", "sync_delete_temp").increment();
        }
    }
}
