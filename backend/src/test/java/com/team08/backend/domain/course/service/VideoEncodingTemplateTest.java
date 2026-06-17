package com.team08.backend.domain.course.service;

import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VideoEncodingTemplateTest {

    @Mock
    private LectureDbService lectureDbService;

    private VideoEncodingTemplate videoEncodingTemplate;
    private MockMultipartFile mockMultipartFile;
    private String targetDirName;
    private Long lectureId;

    private boolean isPrepareCalled;
    private boolean isHandleCalled;
    private boolean isDbPathCalled;
    private boolean shouldThrowInPrepare;

    @BeforeEach
    void setUp() {
        isPrepareCalled = false;
        isHandleCalled = false;
        isDbPathCalled = false;
        shouldThrowInPrepare = false;

        videoEncodingTemplate = new VideoEncodingTemplate(lectureDbService) {
            @Override
            protected File prepareSourceFile(MultipartFile file, String targetDirName, Long lectureId) {
                isPrepareCalled = true;
                if (shouldThrowInPrepare) {
                    throw new RuntimeException();
                }
                return new File("invalid/path/to/trigger/fail");
            }

            @Override
            protected void handleGeneratedFiles(Path workspacePath, String targetDirName, Long lectureId) {
                isHandleCalled = true;
            }

            @Override
            protected String getDbSavePath(String targetDirName, Long lectureId) {
                isDbPathCalled = true;
                return "dummy/path";
            }
        };

        mockMultipartFile = new MockMultipartFile(
                "file",
                "test.mp4",
                "video/mp4",
                "test data".getBytes()
        );
        targetDirName = "test-dir";
        lectureId = 1L;
    }

    @Test
    void 파이프라인_실행_시_정해진_추상_메서드_생명주기가_순서대로_호출된다() {
        assertThrows(CustomException.class, () ->
                videoEncodingTemplate.executePipeline(mockMultipartFile, targetDirName, lectureId)
        );

        assertThat(isPrepareCalled).isTrue();
    }

    @Test
    void 소스_파일_준비_단계에서_예외_발생_시_후속_단계는_실행되지_않는다() {
        shouldThrowInPrepare = true;

        assertThrows(RuntimeException.class, () ->
                videoEncodingTemplate.executePipeline(mockMultipartFile, targetDirName, lectureId)
        );

        assertThat(isHandleCalled).isFalse();
        assertThat(isDbPathCalled).isFalse();
        verifyNoInteractions(lectureDbService);
    }
}