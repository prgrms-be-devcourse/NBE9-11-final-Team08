package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
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
import static org.mockito.Mockito.mock;
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

        // 리팩토링 포인트: 빌더 제거 후 도메인 규칙에 맞는 정적 팩토리 메서드(createDraft)로 교체
        Chapter mockChapter = mock(Chapter.class);
        Lecture lecture = Lecture.createDraft(
                "테스트 강의",
                "",
                600,
                1,
                false,
                mockChapter
        );

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