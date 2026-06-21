package com.team08.backend.domain.media.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.util.S3FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3VideoEncodingServiceTest {

    @Mock
    private S3FileStorageService s3FileStorageService;

    @Mock
    private EncodingResultHandler encodingResultHandler;

    @InjectMocks
    private S3VideoEncodingService s3VideoEncodingService;

    private MockMultipartFile mockMultipartFile;
    private String targetDirName;
    private Long lectureId;

    @BeforeEach
    void setUp() {
        mockMultipartFile = new MockMultipartFile(
                "file",
                "test.mp4",
                "video/mp4",
                "test video content".getBytes()
        );
        targetDirName = "test-uuid";
        lectureId = 1L;
    }

    @Test
    void S3_임시_경로로_원본_비디오_업로드_실패_시_예외를_던지고_로컬_임시_자원을_정리한다() {
        doThrow(new RuntimeException())
                .when(s3FileStorageService)
                .uploadFile(any(File.class), anyString());

        assertThrows(CustomException.class, () ->
                s3VideoEncodingService.encodeToHls(mockMultipartFile, targetDirName, lectureId)
        );

        verify(s3FileStorageService, never()).deleteFile(anyString());
    }

    @Test
    void S3로부터_원본_비디오_다운로드_실패_시_예외를_던지고_임시_파일을_삭제한다() {
        given(s3FileStorageService.uploadFile(any(File.class), anyString())).willReturn("path");
        doThrow(new RuntimeException())
                .when(s3FileStorageService)
                .downloadFile(anyString(), any(File.class));

        assertThrows(CustomException.class, () ->
                s3VideoEncodingService.encodeToHls(mockMultipartFile, targetDirName, lectureId)
        );

        verify(s3FileStorageService, times(1)).deleteFile(eq("videos/temp/" + targetDirName + ".mp4"));
    }

    @Test
    void 인코딩_프로세스_수행_중_에러가_발생하면_예외를_던지고_임시_파일을_삭제한다() {
        given(s3FileStorageService.uploadFile(any(File.class), anyString())).willReturn("path");
        given(s3FileStorageService.downloadFile(anyString(), any(File.class))).willReturn(mock(File.class));

        assertThrows(CustomException.class, () ->
                s3VideoEncodingService.encodeToHls(mockMultipartFile, targetDirName, lectureId)
        );

        verify(s3FileStorageService, times(1)).deleteFile(eq("videos/temp/" + targetDirName + ".mp4"));
    }

    @Test
    void 인코딩은_성공했으나_생성된_HLS_파편_파일이_없으면_예외를_던진다() {
        given(s3FileStorageService.uploadFile(any(File.class), anyString())).willReturn("path");
        given(s3FileStorageService.downloadFile(anyString(), any(File.class))).willReturn(mock(File.class));

        assertThrows(CustomException.class, () ->
                s3VideoEncodingService.encodeToHls(mockMultipartFile, targetDirName, lectureId)
        );

        verify(s3FileStorageService, times(1)).deleteFile(eq("videos/temp/" + targetDirName + ".mp4"));
        verifyNoInteractions(encodingResultHandler);
    }

    @Test
    void 인코딩된_폴더_삭제_요청_시_S3의_해당_디렉토리Prefix_삭제_API를_호출한다() {
        String expectedPrefix = "lectures/" + lectureId + "/" + targetDirName + "/";

        s3VideoEncodingService.deleteEncodedFolder(targetDirName, lectureId);

        verify(s3FileStorageService, times(1)).deleteDirectory(expectedPrefix);
    }
}