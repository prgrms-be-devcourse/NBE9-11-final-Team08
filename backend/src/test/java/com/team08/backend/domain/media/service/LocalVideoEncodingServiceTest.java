package com.team08.backend.domain.media.service;

import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.domain.media.dto.EncodingContext;
import com.team08.backend.domain.media.entity.EncodingPurpose;
import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Disabled("실제 외부 ffmpeg 프로세스를 구동하여 인코딩하는 무거운 테스트이므로 평소에는 비활성화합니다.")
@ExtendWith(MockitoExtension.class)
class LocalVideoEncodingServiceTest {

    @InjectMocks
    private LocalVideoEncodingService localVideoEncodingService;

    @Mock
    private EncodingResultHandler encodingResultHandler;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private LectureModificationRequestRepository requestRepository;

    private Path tempUploadDir;
    private byte[] videoContent;

    @BeforeEach
    void setUp() throws IOException {
        tempUploadDir = Files.createTempDirectory("videos-real-test");
        ReflectionTestUtils.setField(localVideoEncodingService, "uploadDir", tempUploadDir.toString());

        ClassPathResource videoResource = new ClassPathResource("test-video.mp4");
        videoContent = Files.readAllBytes(Path.of(videoResource.getURI()));
    }

    @Test
    void 비디오_인코딩이_성공하면_HLS_파일_경로가_갱신된다() throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(isFfmpegAvailable(), "ffmpeg is not installed, skipping test");

        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        String expectedDbPath = targetDirName + "/output.m3u8";

        File testVideoFile = Files.createTempFile("test-video-", ".mp4").toFile();
        Files.write(testVideoFile.toPath(), videoContent);

        localVideoEncodingService.encodeToHls(testVideoFile, targetDirName, lectureId);

        EncodingContext expectedContext = new EncodingContext(
                lectureId, expectedDbPath, targetDirName, EncodingPurpose.CREATE, null, null
        );

        verify(encodingResultHandler).handleSuccess(expectedContext);

        Path targetWorkspace = tempUploadDir.resolve(targetDirName);
        assertThat(Files.exists(targetWorkspace.resolve("output.m3u8"))).isTrue();

        File[] segments = targetWorkspace.toFile().listFiles((dir, name) -> name.endsWith(".ts"));
        assertThat(segments).isNotNull();
        assertThat(segments.length).isGreaterThan(0);
    }

    @Test
    void 빈_파일을_업로드하면_인코딩_실패_예외를_던진다() throws IOException {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        File emptyFile = Files.createTempFile("empty-video-", ".mp4").toFile();

        assertThatThrownBy(() -> localVideoEncodingService.encodeToHls(emptyFile, targetDirName, lectureId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 인코딩_프로세스는_완료되었으나_결과_파편_파일이_없으면_예외를_던지고_생성되던_디렉토리를_청소한다() throws IOException {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();

        File corruptedVideoFile = Files.createTempFile("corrupted-video-", ".mp4").toFile();
        Files.write(corruptedVideoFile.toPath(), "invalid video content data format headers".getBytes());

        assertThatThrownBy(() -> localVideoEncodingService.encodeToHls(corruptedVideoFile, targetDirName, lectureId))
                .isInstanceOf(CustomException.class);

        Path targetWorkspace = tempUploadDir.resolve(targetDirName);
        assertThat(Files.exists(targetWorkspace)).isFalse();

        verifyNoInteractions(encodingResultHandler);
    }

    private boolean isFfmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            process.destroy();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}