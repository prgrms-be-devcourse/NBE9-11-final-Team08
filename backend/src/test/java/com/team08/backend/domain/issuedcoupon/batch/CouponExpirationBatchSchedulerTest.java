package com.team08.backend.domain.issuedcoupon.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponExpirationBatchSchedulerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-26T15:00:00Z");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job couponExpirationJob;

    @Mock
    private TaskScheduler retryTaskScheduler;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement getLockStatement;

    @Mock
    private PreparedStatement releaseLockStatement;

    @Mock
    private ResultSet lockResultSet;

    private CouponExpirationBatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, SEOUL);
        scheduler = new CouponExpirationBatchScheduler(
                jobLauncher,
                couponExpirationJob,
                retryTaskScheduler,
                jdbcTemplate,
                clock
        );
    }

    @Test
    @DisplayName("분산락을 획득하면 배치 Job을 실행하고 완료 시 재시도를 예약하지 않는다.")
    void runJobWhenLockAcquired() throws Exception {
        // given
        mockJdbcTemplateConnectionCallback();
        mockLockResult(1);
        JobExecution completedExecution = new JobExecution(1L);
        completedExecution.setStatus(BatchStatus.COMPLETED);
        when(jobLauncher.run(eq(couponExpirationJob), any(JobParameters.class)))
                .thenReturn(completedExecution);

        // when
        scheduler.runCouponExpirationJob();

        // then
        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(couponExpirationJob), parametersCaptor.capture());
        assertThat(parametersCaptor.getValue().getString("now"))
                .isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, SEOUL)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        assertThat(parametersCaptor.getValue().getLong("retryAttempt")).isZero();
        verify(retryTaskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verify(releaseLockStatement).execute();
    }

    @Test
    @DisplayName("분산락을 획득하지 못하면 배치 Job을 실행하지 않는다.")
    void skipJobWhenLockNotAcquired() throws Exception {
        // given
        mockJdbcTemplateConnectionCallback();
        mockLockResult(0);

        // when
        scheduler.runCouponExpirationJob();

        // then
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
        verify(retryTaskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verify(releaseLockStatement, never()).execute();
    }

    @Test
    @DisplayName("배치 Job이 실패하면 5분 뒤 재시도를 예약하고 락을 해제한다.")
    void scheduleRetryWhenJobFails() throws Exception {
        // given
        mockJdbcTemplateConnectionCallback();
        mockLockResult(1);
        JobExecution failedExecution = new JobExecution(1L);
        failedExecution.setStatus(BatchStatus.FAILED);
        when(jobLauncher.run(eq(couponExpirationJob), any(JobParameters.class)))
                .thenReturn(failedExecution);

        // when
        scheduler.runCouponExpirationJob();

        // then
        ArgumentCaptor<Runnable> retryCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(retryTaskScheduler).schedule(
                retryCaptor.capture(),
                eq(FIXED_INSTANT.plus(Duration.ofMinutes(5)))
        );
        verify(releaseLockStatement).execute();
    }

    @SuppressWarnings("unchecked")
    private void mockJdbcTemplateConnectionCallback() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class)))
                .thenAnswer(invocation -> {
                    ConnectionCallback<Void> callback = invocation.getArgument(0);
                    return callback.doInConnection(connection);
                });
    }

    private void mockLockResult(int lockResult) throws Exception {
        when(connection.prepareStatement("SELECT GET_LOCK(?, 0)")).thenReturn(getLockStatement);
        when(getLockStatement.executeQuery()).thenReturn(lockResultSet);
        when(lockResultSet.next()).thenReturn(true);
        when(lockResultSet.getInt(1)).thenReturn(lockResult);
        if (lockResult == 1) {
            when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseLockStatement);
        }
    }
}
