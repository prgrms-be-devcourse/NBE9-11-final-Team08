package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.event.VideoCleanUpEvent;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.global.util.S3FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoRollbackEventHandlerTest {

    @InjectMocks
    private VideoRollbackEventHandler videoRollbackEventHandler;

    @Spy
    private List<MediaEncodingService> mediaEncodingServices = new ArrayList<>();

    @Mock
    private S3FileStorageService s3FileStorageService;

    @Mock
    private EncodingResultHandler encodingResultHandler;

    private S3VideoEncodingService s3VideoEncodingService;
    private LocalVideoEncodingService localVideoEncodingService;

    private VideoRollbackEvent videoRollbackEvent;
    private VideoCleanUpEvent videoCleanUpEvent;
    private Long lectureId;
    private String targetDirName;

    @BeforeEach
    void setUp() {
        s3VideoEncodingService = spy(new S3VideoEncodingService(
                encodingResultHandler,
                s3FileStorageService
        ));

        LocalVideoEncodingService concreteLocalService = new LocalVideoEncodingService(
                encodingResultHandler
        );
        ReflectionTestUtils.setField(concreteLocalService, "uploadDir", "src/test/resources/temp-upload");
        localVideoEncodingService = spy(concreteLocalService);

        mediaEncodingServices.clear();
        mediaEncodingServices.add(s3VideoEncodingService);
        mediaEncodingServices.add(localVideoEncodingService);

        lectureId = 1L;
        targetDirName = "rollback-test-uuid";
        videoRollbackEvent = new VideoRollbackEvent(lectureId, targetDirName);
        videoCleanUpEvent = new VideoCleanUpEvent(lectureId, targetDirName);
    }

    @Test
    void 커밋_완료_이벤트_소비_시_주입된_모든_인코딩_서비스의_물리_폴더_삭제_메서드가_다형성_메시징으로_트리거된다() {
        videoRollbackEventHandler.cleanUpOldVideos(videoCleanUpEvent);

        verify(s3VideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
        verify(localVideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
    }

    @Test
    void 트랜잭션_롤백_이벤트_소비_시_주입된_모든_인코딩_서비스의_물리_폴더_삭제_메서드가_다형성_메시징으로_트리거된다() {
        videoRollbackEventHandler.cleanUpLeftoverVideosOnRollback(videoRollbackEvent);

        verify(s3VideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
        verify(localVideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
    }

    @Test
    void 특정_서비스에서_삭제_중_예외가_터져도_다른_서비스의_삭제_흐름에_영향을_주지_않는다() {
        doThrow(new RuntimeException())
                .when(s3FileStorageService)
                .deleteDirectory(anyString());

        videoRollbackEventHandler.cleanUpOldVideos(videoCleanUpEvent);

        verify(s3VideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
        verify(localVideoEncodingService, times(1)).deleteEncodedFolder(targetDirName, lectureId);
    }
}