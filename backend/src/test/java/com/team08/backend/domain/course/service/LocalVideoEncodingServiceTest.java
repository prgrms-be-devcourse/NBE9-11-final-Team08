package com.team08.backend.domain.course.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalVideoEncodingServiceTest {

    @InjectMocks
    private LocalVideoEncodingService localVideoEncodingService;

    @Mock
    private LectureRepository lectureRepository;

    private File tempSourceFile;
    private Path tempUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        tempUploadDir = Files.createTempDirectory("videos-test");
        ReflectionTestUtils.setField(localVideoEncodingService, "uploadDir", tempUploadDir.toString());

        tempSourceFile = File.createTempFile("test-source", ".mp4");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempSourceFile.exists()) {
            tempSourceFile.delete();
        }
        Files.walk(tempUploadDir)
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void 비디오_인코딩이_성공하면_HLS_파일_경로가_갱신되고_임시_원본_파일이_삭제된다() throws InterruptedException {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        CountDownLatch latch = new CountDownLatch(1);

        Lecture lecture = Lecture.builder()
                .title("테스트 강의")
                .m3u8Path("")
                .durationSeconds(600)
                .orderNo(1)
                .build();

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));

        LocalVideoEncodingService stubService = new LocalVideoEncodingService(lectureRepository) {
            @Override
            public void encodeToHls(File sourceFile, String targetDirName, Long lectureId) {
                try {
                    Path targetPath = Path.of(tempUploadDir.toString(), targetDirName);
                    Files.createDirectories(targetPath);
                    String dbSavePath = targetDirName + "/output.m3u8";
                    Lecture targetLecture = lectureRepository.findById(lectureId).orElseThrow();
                    targetLecture.updateM3u8Path(dbSavePath);
                    latch.countDown();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (sourceFile.exists()) {
                        sourceFile.delete();
                    }
                }
            }
        };
        ReflectionTestUtils.setField(stubService, "uploadDir", tempUploadDir.toString());

        stubService.encodeToHls(tempSourceFile, targetDirName, lectureId);

        latch.await(5, TimeUnit.SECONDS);

        verify(lectureRepository).findById(lectureId);
        assertFalse(tempSourceFile.exists());
    }

    @Test
    void 비디오_인코딩_중_예외가_발생하면_인코딩_실패_예외를_던지고_임시_원본_파일을_삭제한다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();

        LocalVideoEncodingService stubService = new LocalVideoEncodingService(lectureRepository) {
            @Override
            public void encodeToHls(File sourceFile, String targetDirName, Long lectureId) {
                try {
                    throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
                } finally {
                    if (sourceFile.exists()) {
                        sourceFile.delete();
                    }
                }
            }
        };
        ReflectionTestUtils.setField(stubService, "uploadDir", tempUploadDir.toString());

        assertThatThrownBy(() -> stubService.encodeToHls(tempSourceFile, targetDirName, lectureId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.VIDEO_ENCODING_FAILED.getMessage());

        assertFalse(tempSourceFile.exists());
    }
}