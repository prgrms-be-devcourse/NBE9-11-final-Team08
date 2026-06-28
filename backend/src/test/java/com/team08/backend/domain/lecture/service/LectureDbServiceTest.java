package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.event.VideoCleanUpEvent;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

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
        String targetDirName = UUID.randomUUID().toString();
        String dbSavePath = "lectures/1/" + targetDirName + "/output.m3u8";

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

        given(lectureRepository.findByIdWithPessimisticLock(lectureId)).willReturn(Optional.of(lecture));

        lectureDbService.updateLectureM3u8(lectureId, dbSavePath, targetDirName);

        assertThat(lecture.getM3u8Path()).isEqualTo(dbSavePath);
        verify(eventPublisher).publishEvent(new VideoRollbackEvent(lectureId, targetDirName));
    }

    @Test
    void 기존_videoUuid가_존재하는_경우_HLS_경로가_변경되고_롤백_이벤트와_클린업_이벤트를_모두_발행한다() {
        Long lectureId = 1L;
        String oldVideoUuid = UUID.randomUUID().toString();
        String targetDirName = UUID.randomUUID().toString();
        String dbSavePath = "lectures/1/" + targetDirName + "/output.m3u8";

        Chapter mockChapter = mock(Chapter.class);
        Lecture lecture = Lecture.createWithStream(
                "old-path/output.m3u8",
                oldVideoUuid,
                "테스트 강의",
                "",
                600,
                1,
                false,
                mockChapter
        );

        given(lectureRepository.findByIdWithPessimisticLock(lectureId)).willReturn(Optional.of(lecture));

        lectureDbService.updateLectureM3u8(lectureId, dbSavePath, targetDirName);

        assertThat(lecture.getM3u8Path()).isEqualTo(dbSavePath);
        verify(eventPublisher).publishEvent(new VideoRollbackEvent(lectureId, targetDirName));
        verify(eventPublisher).publishEvent(new VideoCleanUpEvent(lectureId, oldVideoUuid));
    }

    @Test
    void 강의가_존재하지_않으면_예외를_던지고_이벤트를_발행하지_않는다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        String dbSavePath = "lectures/1/" + targetDirName + "/output.m3u8";

        given(lectureRepository.findByIdWithPessimisticLock(lectureId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lectureDbService.updateLectureM3u8(lectureId, dbSavePath, targetDirName))
                .isInstanceOf(CustomException.class);

        verifyNoInteractions(eventPublisher);
    }
}