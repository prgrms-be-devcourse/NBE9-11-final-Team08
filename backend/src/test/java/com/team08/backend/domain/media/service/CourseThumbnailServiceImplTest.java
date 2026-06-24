package com.team08.backend.domain.media.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.S3FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseThumbnailServiceImplTest {

    @Mock
    private S3FileStorageService s3FileStorageService;

    @InjectMocks
    private CourseThumbnailServiceImpl courseThumbnailService;

    @Test
    void 유효한_이미지_파일이_들어오면_S3에_성공적으로_업로드하고_저장된_키를_반환한다() {
        Long courseId = 100L;
        MultipartFile mockFile = new MockMultipartFile(
                "thumbnail", "강의썸네일.png", "image/png", "mock-image-bytes".getBytes()
        );

        given(s3FileStorageService.uploadFile(any(File.class), contains("courses/thumbnails/100/"))).willReturn("courses/thumbnails/100/generated-uuid.png");

        String resultKey = courseThumbnailService.uploadThumbnail(courseId, mockFile);

        assertThat(resultKey).contains("courses/thumbnails/100/");
        assertThat(resultKey).endsWith(".png");
        verify(s3FileStorageService).uploadFile(any(File.class), contains("courses/thumbnails/100/"));
    }

    @Test
    void 썸네일_파일이_비어있거나_null이면_예외가_발생한다() {
        Long courseId = 100L;
        MultipartFile emptyFile = new MockMultipartFile("thumbnail", "", "image/png", new byte[0]);

        assertThatThrownBy(() -> courseThumbnailService.uploadThumbnail(courseId, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        assertThatThrownBy(() -> courseThumbnailService.uploadThumbnail(courseId, emptyFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        verify(s3FileStorageService, never()).uploadFile(any(File.class), any(String.class));
    }

    @Test
    void 파일_이름에_확장자가_없으면_기본_확장자인_png를_적용하여_S3에_업로드한다() {
        Long courseId = 100L;
        MultipartFile noExtensionFile = new MockMultipartFile(
                "thumbnail", "no-extension-filename", "image/png", "mock-image-bytes".getBytes()
        );

        given(s3FileStorageService.uploadFile(any(File.class), contains(".png"))).willReturn("courses/thumbnails/100/uuid.png");

        String resultKey = courseThumbnailService.uploadThumbnail(courseId, noExtensionFile);

        assertThat(resultKey).endsWith(".png");
        verify(s3FileStorageService).uploadFile(any(File.class), contains(".png"));
    }

    @Test
    void 올바른_S3_키가_전달되면_S3에서_썸네일_파일을_삭제한다() {
        String s3Key = "courses/thumbnails/100/file.png";

        courseThumbnailService.deleteThumbnail(s3Key);

        verify(s3FileStorageService).deleteFile(s3Key);
    }

    @Test
    void 삭제할_S3_키가_null이거나_공백이면_물리_삭제_로직을_트리거하지_않는다() {
        courseThumbnailService.deleteThumbnail(null);
        courseThumbnailService.deleteThumbnail("   ");

        verify(s3FileStorageService, never()).deleteFile(any(String.class));
    }
}