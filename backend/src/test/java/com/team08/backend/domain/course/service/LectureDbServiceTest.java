package com.team08.backend.domain.course.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LectureDbServiceTest {

    @InjectMocks
    private LectureDbService lectureDbService;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void 강의가_존재하면_독립_트랜잭션_내에서_HLS_경로가_변경되고_롤백_대비_이벤트를_발행한다() {
        Long lectureId = 1L;
        String targetDirName = "test-uuid";
        String dbSavePath = "lectures/1/test-uuid/output.m3u8";

        Lecture lecture = Lecture.builder()
                .title("테스트 강의")
                .m3u8Path("")
                .durationSeconds(600)
                .orderNo(1)
                .build();

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));

        lectureDbService.updateLectureM3u8(lectureId, dbSavePath, targetDirName);

        assertThat(lecture.getM3u8Path()).isEqualTo(dbSavePath);
        verify(eventPublisher).publishEvent(new VideoRollbackEvent(lectureId, targetDirName));
    }

    @Test
    void 강의가_존재하지_않으면_예외를_던지고_이벤트를_발행하지_않는다() {
        Long lectureId = 1L;
        String targetDirName = "test-uuid";
        String dbSavePath = "lectures/1/test-uuid/output.m3u8";

        given(lectureRepository.findById(lectureId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lectureDbService.updateLectureM3u8(lectureId, dbSavePath, targetDirName))
                .isInstanceOf(CustomException.class);

        verifyNoInteractions(eventPublisher);
    }
}