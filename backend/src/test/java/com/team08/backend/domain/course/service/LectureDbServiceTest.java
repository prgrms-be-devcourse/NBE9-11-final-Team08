package com.team08.backend.domain.course.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LectureDbServiceTest {

    @InjectMocks
    private LectureDbService lectureDbService;

    @Mock
    private LectureRepository lectureRepository;

    @Test
    void 강의가_존재하면_HLS_파일_경로가_정상적으로_변경된다() {
        Long lectureId = 1L;
        String dbSavePath = "lectures/1/uuid/output.m3u8";

        Lecture lecture = Lecture.builder()
                .title("테스트 강의")
                .m3u8Path("")
                .durationSeconds(600)
                .orderNo(1)
                .build();

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));

        lectureDbService.updateLectureM3u8(lectureId, dbSavePath);

        assertThat(lecture.getM3u8Path()).isEqualTo(dbSavePath);
    }

    @Test
    void 강의가_존재하지_않으면_예외를_던진다() {
        Long lectureId = 1L;
        String dbSavePath = "lectures/1/uuid/output.m3u8";

        given(lectureRepository.findById(lectureId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lectureDbService.updateLectureM3u8(lectureId, dbSavePath))
                .isInstanceOf(CustomException.class);
    }
}