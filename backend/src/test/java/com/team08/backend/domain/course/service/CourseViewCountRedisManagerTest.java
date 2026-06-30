package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.repository.CourseRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseViewCountRedisManagerTest {

    @InjectMocks
    private CourseViewCountRedisManager courseViewCountRedisManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseDetailCacheManager courseDetailCacheManager;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void 최초_상세조회_시_Redis에_락_키를_등록하고_조회수_델타를_1_가산한다() {
        // given
        Long courseId = 1L;
        String userIdentifier = "GUEST:127.0.0.1";
        String lockKey = "{course:viewcount}:lock:1:GUEST:127.0.0.1";
        String deltaKey = "{course:viewcount}:delta:1";

        given(valueOperations.setIfAbsent(eq(lockKey), eq("1"), eq(5L), eq(TimeUnit.MINUTES)))
                .willReturn(true);

        // when
        courseViewCountRedisManager.increaseViewCount(courseId, userIdentifier);

        // then
        verify(valueOperations).setIfAbsent(eq(lockKey), eq("1"), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations).increment(eq(deltaKey));
        verify(redisTemplate).expire(eq(deltaKey), eq(1L), eq(TimeUnit.HOURS));
        verify(setOperations).add(eq("{course:viewcount}:active_ids"), eq("1"));
    }

    @Test
    void 동일_사용자가_5분_이내에_재조회_시_Redis_락_키_등록에_실패하여_조회수를_가산하지_않는다() {
        // given
        Long courseId = 1L;
        String userIdentifier = "GUEST:127.0.0.1";
        String lockKey = "{course:viewcount}:lock:1:GUEST:127.0.0.1";

        given(valueOperations.setIfAbsent(eq(lockKey), eq("1"), eq(5L), eq(TimeUnit.MINUTES)))
                .willReturn(false);

        // when
        courseViewCountRedisManager.increaseViewCount(courseId, userIdentifier);

        // then
        verify(valueOperations).setIfAbsent(eq(lockKey), eq("1"), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations, never()).increment(any(String.class));
        verify(redisTemplate, never()).expire(any(String.class), anyLong(), any(TimeUnit.class));
        verify(setOperations, never()).add(any(String.class), any(String.class));
    }
}
