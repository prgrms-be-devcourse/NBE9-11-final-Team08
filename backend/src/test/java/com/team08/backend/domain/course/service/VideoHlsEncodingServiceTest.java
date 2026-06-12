package com.team08.backend.domain.course.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VideoHlsEncodingServiceTest {

    @Mock
    private LectureRepository lectureRepository;

    @InjectMocks
    private VideoHlsEncodingService videoHlsEncodingService;

    @Test
    void 존재하지_않는_강의_ID로_인코딩_요청_시_예외가_발생하고_원본_파일을_유지한다(@TempDir Path tempDir) throws IOException {
        Long invalidLectureId = 999L;
        String targetDirName = "test-dir";
        File sourceFile = tempDir.resolve("video.mp4").toFile();
        sourceFile.createNewFile();

        given(lectureRepository.findById(invalidLectureId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoHlsEncodingService.encodeToHls(sourceFile, targetDirName, invalidLectureId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.LECTURE_NOT_FOUND.getMessage());

        assertThat(sourceFile.exists()).isTrue();
        verify(lectureRepository).findById(invalidLectureId);
    }

    @Test
    void 원본_비디오_파일이_존재하지_않으면_인코딩_과정에서_예외가_발생한다() {
        Long lectureId = 1L;
        String targetDirName = "test-dir";
        File nonExistentFile = new File("invalid/path/video.mp4");

        Lecture lecture = Lecture.builder()
                .title("테스트 강의")
                .m3u8Path("")
                .chapter(Chapter.builder().build())
                .build();

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));

        assertThatThrownBy(() -> videoHlsEncodingService.encodeToHls(nonExistentFile, targetDirName, lectureId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.VIDEO_ENCODING_FAILED.getMessage());

        assertThat(lecture.getM3u8Path()).isEmpty();
    }
}