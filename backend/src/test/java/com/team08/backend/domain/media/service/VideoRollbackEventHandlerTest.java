package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.S3CleanupDlq;
import com.team08.backend.domain.media.event.VideoCleanUpEvent;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.domain.media.repository.S3CleanupDlqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.RejectedExecutionException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoRollbackEventHandlerTest {

    @InjectMocks
    private VideoRollbackEventHandler videoRollbackEventHandler;

    @Mock
    private VideoCleanupService videoCleanupService;

    @Mock
    private S3CleanupDlqRepository dlqRepository;

    private VideoRollbackEvent videoRollbackEvent;
    private VideoCleanUpEvent videoCleanUpEvent;
    private Long lectureId;
    private String targetDirName;

    @BeforeEach
    void setUp() {
        lectureId = 1L;
        targetDirName = "rollback-test-uuid";
        videoRollbackEvent = new VideoRollbackEvent(lectureId, targetDirName);
        videoCleanUpEvent = new VideoCleanUpEvent(lectureId, targetDirName);
    }

    @Test
    void 커밋_완료_이벤트_소비_시_비동기_정리_서비스가_트리거된다() {
        videoRollbackEventHandler.cleanUpOldVideos(videoCleanUpEvent);

        verify(videoCleanupService, times(1)).cleanUpOldVideosAsync(videoCleanUpEvent);
    }

    @Test
    void 트랜잭션_롤백_이벤트_소비_시_비동기_정리_서비스가_트리거된다() {
        videoRollbackEventHandler.cleanUpLeftoverVideosOnRollback(videoRollbackEvent);

        verify(videoCleanupService, times(1)).cleanUpLeftoverVideosOnRollbackAsync(videoRollbackEvent);
    }

    @Test
    void 비동기_구영상_정리_작업이_스레드_풀에서_거절되면_즉시_DLQ에_기록한다() {
        doThrow(new RejectedExecutionException("풀 포화"))
                .when(videoCleanupService)
                .cleanUpOldVideosAsync(videoCleanUpEvent);

        videoRollbackEventHandler.cleanUpOldVideos(videoCleanUpEvent);

        verify(dlqRepository, times(1)).save(any(S3CleanupDlq.class));
    }

    @Test
    void 비동기_찌꺼기_정리_작업이_스레드_풀에서_거절되면_즉시_DLQ에_기록한다() {
        doThrow(new RejectedExecutionException("풀 포화"))
                .when(videoCleanupService)
                .cleanUpLeftoverVideosOnRollbackAsync(videoRollbackEvent);

        videoRollbackEventHandler.cleanUpLeftoverVideosOnRollback(videoRollbackEvent);

        verify(dlqRepository, times(1)).save(any(S3CleanupDlq.class));
    }
}