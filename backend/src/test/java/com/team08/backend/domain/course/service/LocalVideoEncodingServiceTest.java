package com.team08.backend.domain.course.service;

import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LocalVideoEncodingServiceTest {

    @InjectMocks
    private LocalVideoEncodingService localVideoEncodingService;

    @Mock
    private LectureDbService lectureDbService;

    private Path tempUploadDir;
    private MockMultipartFile realMockMultipartFile;

    @BeforeEach
    void setUp() throws IOException {
        tempUploadDir = Files.createTempDirectory("videos-real-test");
        ReflectionTestUtils.setField(localVideoEncodingService, "uploadDir", tempUploadDir.toString());

        ClassPathResource videoResource = new ClassPathResource("test-video.mp4");
        byte[] videoContent = Files.readAllBytes(Path.of(videoResource.getURI()));

        realMockMultipartFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                videoContent
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempUploadDir)) {
            try (var stream = Files.walk(tempUploadDir)) {
                stream.sorted(reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {}
                        });
            }
        }
    }

    @Test
    void 비디오_인코딩이_성공하면_HLS_파일_경로가_갱신된다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        String expectedDbPath = targetDirName + "/output.m3u8";

        localVideoEncodingService.encodeToHls(realMockMultipartFile, targetDirName, lectureId);

        verify(lectureDbService).updateLectureM3u8(lectureId, expectedDbPath, targetDirName);

        Path targetWorkspace = tempUploadDir.resolve(targetDirName);
        assertThat(Files.exists(targetWorkspace.resolve("output.m3u8"))).isTrue();

        File[] segments = targetWorkspace.toFile().listFiles((dir, name) -> name.endsWith(".ts"));
        assertThat(segments).isNotNull();
        assertThat(segments.length).isGreaterThan(0);
    }

    @Test
    void 빈_파일을_업로드하면_인코딩_실패_예외를_던진다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "video/mp4", new byte[0]);

        assertThatThrownBy(() -> localVideoEncodingService.encodeToHls(emptyFile, targetDirName, lectureId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 인코딩_프로세스는_완료되었으나_결과_파편_파일이_없으면_예외를_던지고_생성되던_디렉토리를_청소한다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();

        MockMultipartFile corruptedVideoFile = new MockMultipartFile(
                "file",
                "corrupted.mp4",
                "video/mp4",
                "invalid video content data format headers".getBytes()
        );

        assertThatThrownBy(() -> localVideoEncodingService.encodeToHls(corruptedVideoFile, targetDirName, lectureId))
                .isInstanceOf(CustomException.class);

        Path targetWorkspace = tempUploadDir.resolve(targetDirName);
        assertThat(Files.exists(targetWorkspace)).isFalse();

        verifyNoInteractions(lectureDbService);
    }
}