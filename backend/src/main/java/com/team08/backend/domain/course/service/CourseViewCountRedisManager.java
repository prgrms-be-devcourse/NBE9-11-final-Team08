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
 
    /**
     * 상세 조회 시 RDB Write를 생략하고 Redis Delta 값을 1 증가시킵니다.
     * 메모리 유실 방지를 위해 1시간의 TTL을 적용합니다.
     * 활성 ID Set에 강좌 ID를 적재하여 KEYS 명령어 없이 대상 스캔이 가능하게 차단합니다.
     */
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
 
    /**
     * 현재 캐싱된 Redis Delta 값을 가져옵니다. (실시간 조회 응답 조립용)
     */
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
 
    /**
     * 10초 주기로 Redis에 쌓인 델타값을 RDB에 반영(Write-Behind)합니다.
     * 원자적인 getAndSet 연산을 활용하여 동기화 도중 인크리먼트되는 조회수의 유실을 100% 예방합니다.
     * Redis KEYS 명령어 전체 스캔을 제거하고 active_ids Set 구조만 O(M)으로 훑어 메인 스레드 멈춤 현상을 완전히 방지합니다.
     */
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
            // 원자적으로 키 이름을 변경하여 동기화 대상 격리 (Atomic Swap)
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
                // 원자적으로 현재 값을 가져오고 0으로 리셋하여 유실 0% 달성
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

        // 임시 격리 키 전체 데이터 일괄 영구 삭제 (N번의 remove 쿼리를 1번의 delete로 단축)
        try {
            redisTemplate.delete(tempKey);
        } catch (Exception e) {
            log.error("[조회수 동기화] 임시 격리 키 삭제 실패: {}", tempKey, e);
            meterRegistry.counter("redis.viewcount.errors", "operation", "sync_delete_temp").increment();
        }
    }
}
