package com.team08.backend.domain.media.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.service.LectureDbService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.domain.media.entity.EncodingPurpose;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncodingResultHandlerImplTest {

    @InjectMocks
    private EncodingResultHandlerImpl encodingResultHandler;

    @Mock
    private LectureDbService lectureDbService;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private LectureModificationRequestRepository requestRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void 신규_생성_목적인_경우_일반_강의_영상_경로만_업데이트하고_수정_요청_로직을_수행하지_않는다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        String dbSavePath = "lectures/1/" + targetDirName + "/output.m3u8";

        encodingResultHandler.handleSuccess(lectureId, dbSavePath, targetDirName, EncodingPurpose.CREATE, null, null);

        verify(lectureDbService).updateLectureM3u8(lectureId, dbSavePath, targetDirName);
        verifyNoInteractions(lectureRepository, requestRepository, eventPublisher);
    }

    @Test
    void 영상_수정_목적인_경우_수정_신청_요청을_저장하고_롤백_이벤트를_발행한다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        String dbSavePath = "lectures/1/" + targetDirName + "/output.m3u8";
        String description = "강의 수정 요청";
        Long instructorId = 100L;

        Chapter mockChapter = mock(Chapter.class);
        Lecture lecture = Lecture.createWithStream(
                "old/path",
                UUID.randomUUID().toString(),
                "테스트 강의 제목",
                "요약",
                600,
                1,
                false,
                mockChapter
        );

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));

        encodingResultHandler.handleSuccess(lectureId, dbSavePath, targetDirName, EncodingPurpose.MODIFY, description, instructorId);

        verify(lectureRepository).findById(lectureId);
        verify(requestRepository).save(any(LectureModificationRequest.class));
        verify(eventPublisher).publishEvent(any(VideoRollbackEvent.class));
        verifyNoInteractions(lectureDbService);
    }

    @Test
    void 영상_수정_목적이지만_강의를_찾을_수_없으면_예외를_던지고_저장_및_이벤트_발행을_수행하지_않는다() {
        Long lectureId = 1L;
        String targetDirName = UUID.randomUUID().toString();
        String dbSavePath = "lectures/1/" + targetDirName + "/output.m3u8";
        String description = "강의 수정 요청";
        Long instructorId = 100L;

        given(lectureRepository.findById(lectureId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> encodingResultHandler.handleSuccess(lectureId, dbSavePath, targetDirName, EncodingPurpose.MODIFY, description, instructorId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.LECTURE_NOT_FOUND.getMessage());

        verify(lectureRepository).findById(lectureId);
        verifyNoInteractions(requestRepository, eventPublisher, lectureDbService);
    }
}