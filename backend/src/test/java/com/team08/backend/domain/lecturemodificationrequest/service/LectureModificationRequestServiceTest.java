package com.team08.backend.domain.lecturemodificationrequest.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.service.MediaEncodingService;
import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationRequestCreateDto;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LectureModificationRequestServiceTest {

    @InjectMocks
    private LectureModificationRequestService requestService;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private MediaEncodingService mediaEncodingService;

    @Test
    void 강좌_소유권이_검증되면_변경_신청용_비동기_인코딩_파이프라인을_시작한다() {
        Long lectureId = 1L;
        Long instructorId = 100L;
        String description = "강의 내용 업데이트 요청";
        MultipartFile videoFile = new MockMultipartFile("video", "test.mp4", "video/mp4", "content".getBytes());

        LectureModificationRequestCreateDto dto = new LectureModificationRequestCreateDto(lectureId, description);

        // 리팩토링 포인트: 빌더 제거 후 비즈니스 규칙에 호환되는 정적 팩토리 메서드 흐름으로 조립
        Course course = Course.createDraft(instructorId, 1L, "가상 코스", "설명", "thumb.jpg", 0);
        Chapter chapter = Chapter.create("가상 챕터", 1, course);
        Lecture lecture = Lecture.createWithStream("old/path.m3u8", "가상 강의", "요약", 600, 1, false, chapter);

        given(lectureRepository.findById(dto.lectureId())).willReturn(Optional.of(lecture));

        requestService.createRequest(dto, instructorId, videoFile);

        verify(mediaEncodingService).encodeModificationToHls(
                eq(videoFile),
                any(String.class),
                eq(dto.lectureId()),
                eq(dto.description()),
                eq(instructorId)
        );
    }

    @Test
    void 강좌_소유자가_아닌_경우_예외를_던지고_인코딩을_수행하지_않는다() {
        Long lectureId = 1L;
        Long targetInstructorId = 100L;
        Long hackerInstructorId = 999L;
        String description = "타인의 강의 탈취 시도";
        MultipartFile videoFile = new MockMultipartFile("video", "test.mp4", "video/mp4", "content".getBytes());

        LectureModificationRequestCreateDto dto = new LectureModificationRequestCreateDto(lectureId, description);

        // 리팩토링 포인트: 빌더 제거 후 비즈니스 규칙에 호환되는 정적 팩토리 메서드 흐름으로 조립
        Course course = Course.createDraft(targetInstructorId, 1L, "타인 코스", "설명", "thumb.jpg", 0);
        Chapter chapter = Chapter.create("타인 챕터", 1, course);
        Lecture lecture = Lecture.createWithStream("old/path.m3u8", "타인 강의", "요약", 600, 1, false, chapter);

        given(lectureRepository.findById(dto.lectureId())).willReturn(Optional.of(lecture));

        assertThatThrownBy(() -> requestService.createRequest(dto, hackerInstructorId, videoFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_COURSE_OWNER.getMessage());

        verifyNoInteractions(mediaEncodingService);
    }
}