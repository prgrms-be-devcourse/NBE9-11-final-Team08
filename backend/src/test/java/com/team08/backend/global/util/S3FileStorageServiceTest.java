package com.team08.backend.global.util;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    private S3Template s3Template;

    @InjectMocks
    private S3FileStorageService s3FileStorageService;

    private File tempFile;
    private String bucket;
    private String s3Key;

    @BeforeEach
    void setUp() throws IOException {
        bucket = "test-bucket";
        s3Key = "videos/temp/test.mp4";
        ReflectionTestUtils.setField(s3FileStorageService, "bucket", bucket);

        Path tempPath = Files.createTempFile("s3-test-", ".mp4");
        tempFile = tempPath.toFile();
    }

    @Test
    void InputStream_기반_S3_파일_업로드_성공_시_S3_키를_반환한다() {
        InputStream is = new ByteArrayInputStream("content".getBytes());

        String result = s3FileStorageService.uploadFile(is, s3Key);

        assertEquals(s3Key, result);
        verify(s3Template).upload(eq(bucket), eq(s3Key), eq(is));
    }

    @Test
    void InputStream_기반_S3_파일_업로드_중_예외_발생_시_S3_UPLOAD_FAILED_에러코드를_던진다() {
        InputStream is = new ByteArrayInputStream("content".getBytes());
        doThrow(new RuntimeException())
                .when(s3Template)
                .upload(anyString(), anyString(), any(InputStream.class));

        CustomException exception = assertThrows(CustomException.class, () ->
                s3FileStorageService.uploadFile(is, s3Key)
        );

        assertEquals(ErrorCode.S3_UPLOAD_FAILED, exception.getErrorCode());
    }

    @Test
    void S3_파일_업로드_성공_시_S3_키를_반환한다() {
        String result = s3FileStorageService.uploadFile(tempFile, s3Key);

        assertEquals(s3Key, result);
        verify(s3Template).upload(eq(bucket), eq(s3Key), any(InputStream.class));
    }

    @Test
    void S3_파일_업로드_중_예외_발생_시_S3_UPLOAD_FAILED_에러코드를_던진다() {
        doThrow(new RuntimeException())
                .when(s3Template)
                .upload(anyString(), anyString(), any(InputStream.class));

        CustomException exception = assertThrows(CustomException.class, () ->
                s3FileStorageService.uploadFile(tempFile, s3Key)
        );

        assertEquals(ErrorCode.S3_UPLOAD_FAILED, exception.getErrorCode());
    }

    @Test
    void S3_파일_다운로드_중_예외_발생_시_S3_DOWNLOAD_FAILED_에러코드를_던진다() {
        given(s3Template.download(anyString(), anyString())).willThrow(new RuntimeException());

        CustomException exception = assertThrows(CustomException.class, () ->
                s3FileStorageService.downloadFile(s3Key, tempFile)
        );

        assertEquals(ErrorCode.S3_DOWNLOAD_FAILED, exception.getErrorCode());
    }

    @Test
    void S3_단일_파일_삭제_중_예외_발생_시_S3_DELETE_FAILED_에러코드를_던진다() {
        doThrow(new RuntimeException())
                .when(s3Template)
                .deleteObject(anyString(), anyString());

        CustomException exception = assertThrows(CustomException.class, () ->
                s3FileStorageService.deleteFile(s3Key)
        );

        assertEquals(ErrorCode.S3_DELETE_FAILED, exception.getErrorCode());
    }

    @Test
    void S3_디렉토리_삭제_중_조회_실패_시_S3_DELETE_FAILED_에러코드를_던진다() {
        given(s3Template.listObjects(anyString(), anyString())).willThrow(new RuntimeException());

        CustomException exception = assertThrows(CustomException.class, () ->
                s3FileStorageService.deleteDirectory("lectures/1/")
        );

        assertEquals(ErrorCode.S3_DELETE_FAILED, exception.getErrorCode());
    }

    @Test
    void S3_디렉토리_내부_오브젝트_삭제_중_일부_파일이_실패해도_튕기지_않고_나머지_삭제를_끝까지_수행한다() {
        S3Resource mockResource1 = mock(S3Resource.class);
        S3Resource mockResource2 = mock(S3Resource.class);

        given(mockResource1.getFilename()).willReturn("lectures/1/segment_001.ts");
        given(mockResource2.getFilename()).willReturn("lectures/1/segment_002.ts");
        given(s3Template.listObjects(anyString(), anyString())).willReturn(List.of(mockResource1, mockResource2));

        doThrow(new RuntimeException())
                .when(s3Template)
                .deleteObject(bucket, "lectures/1/segment_001.ts");

        s3FileStorageService.deleteDirectory("lectures/1/");

        verify(s3Template, times(1)).deleteObject(bucket, "lectures/1/segment_001.ts");
        verify(s3Template, times(1)).deleteObject(bucket, "lectures/1/segment_002.ts");
    }
}