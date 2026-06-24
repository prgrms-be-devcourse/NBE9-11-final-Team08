package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.event.CourseThumbnailEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseThumbnailEventHandlerTest {

    @Mock
    private CourseThumbnailService courseThumbnailService;

    @InjectMocks
    private CourseThumbnailEventHandler courseThumbnailEventHandler;

    @Test
    void 트랜잭션_커밋이_성공하면_S3에서_기존_구버전_썸네일_파일을_안전하게_소거한다() {
        CourseThumbnailEvent event = new CourseThumbnailEvent(100L, "old-thumbnail.png", "new-thumbnail.png");

        courseThumbnailEventHandler.cleanUpOldThumbnailOnCommit(event);

        verify(courseThumbnailService, times(1)).deleteThumbnail("old-thumbnail.png");
        verify(courseThumbnailService, never()).deleteThumbnail("new-thumbnail.png");
    }

    @Test
    void 트랜잭션_롤백이_발생하면_정합성을_위해_이미_S3에_업로드되었던_신규_썸네일_고아_파일을_삭제한다() {
        CourseThumbnailEvent event = new CourseThumbnailEvent(100L, "old-thumbnail.png", "new-thumbnail.png");

        courseThumbnailEventHandler.cleanUpNewThumbnailOnRollback(event);

        verify(courseThumbnailService, times(1)).deleteThumbnail("new-thumbnail.png");
        verify(courseThumbnailService, never()).deleteThumbnail("old-thumbnail.png");
    }

    @Test
    void 삭제할_구버전_썸네일이_없다면_물리_삭제_로직을_트리거하지_않는다() {
        CourseThumbnailEvent event = new CourseThumbnailEvent(100L, null, "new-thumbnail.png");

        courseThumbnailEventHandler.cleanUpOldThumbnailOnCommit(event);

        verify(courseThumbnailService, never()).deleteThumbnail(anyString());
    }
}