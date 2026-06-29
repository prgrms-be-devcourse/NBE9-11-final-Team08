package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.S3CleanupDlq;
import com.team08.backend.domain.media.event.VideoCleanUpEvent;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.domain.media.repository.S3CleanupDlqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoCleanupServiceTest {

    private VideoCleanupService videoCleanupService;

    @Mock
    private MediaEncodingService s3VideoEncodingService;

    @Mock
    private MediaEncodingService localVideoEncodingService;

    @Mock
    private S3CleanupDlqRepository dlqRepository;

    private VideoRollbackEvent videoRollbackEvent;
    private VideoCleanUpEvent videoCleanUpEvent;
    private Long lectureId;
    private String targetDirName;

    @BeforeEach
    void setUp() {
        List<MediaEncodingService> services = new ArrayList<>();
        services.add(s3VideoEncodingService);
        services.add(localVideoEncodingService);

        videoCleanupService = new VideoCleanupService(services, dlqRepository);

        lectureId = 1L;
        targetDirName = "cleanup-test-uuid";
        videoRollbackEvent = new VideoRollbackEvent(lectureId, targetDirName);
        videoCleanUpEvent = new VideoCleanUpEvent(lectureId, targetDirName);
    }

    @Test
    void 비동기_구영상_정리_수행_시_주입된_모든_인코딩_서비스의_삭제_메서드가_호출된다() {
        videoCleanupService.cleanUpOldVideosAsync(videoCleanUpEvent);

        verify(s3VideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
        verify(localVideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
    }

    @Test
    void 비동기_찌꺼기_정리_수행_시_주입된_모든_인코딩_서비스의_삭제_메서드가_호출된다() {
        videoCleanupService.cleanUpLeftoverVideosOnRollbackAsync(videoRollbackEvent);

        verify(s3VideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
        verify(localVideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
    }

    @Test
    void 특정_인코딩_서비스에서_삭제_중_예외가_발생해도_다른_서비스의_삭제_흐름은_유지되며_실패_항목은_DLQ에_저장된다() {
        doThrow(new RuntimeException("S3 Connection Fail"))
                .when(s3VideoEncodingService)
                .deleteEncodedFolder(anyString(), anyLong());

        videoCleanupService.cleanUpLeftoverVideosOnRollbackAsync(videoRollbackEvent);

        // 예외가 발생했어도 localVideoEncodingService는 무사히 호출되어야 함 (다형성 유지)
        verify(s3VideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
        verify(localVideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);

        // 실패한 삭제 건에 대해 DLQ 저장 동작 확인
        verify(dlqRepository, times(1)).save(any(S3CleanupDlq.class));
    }
}
