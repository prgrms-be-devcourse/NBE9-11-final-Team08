package com.team08.backend.domain.lecturemodificationrequest.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationApprovalRequest;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.entity.RequestStatus;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LectureModificationApprovalServiceTest {

    @InjectMocks
    private LectureModificationApprovalService approvalService;

    @Mock
    private LectureModificationRequestRepository requestRepository;

    private Long requestId;
    private Long adminId;
    private Lecture lecture;
    private LectureModificationRequest pendingRequest;

    @BeforeEach
    void setUp() {
        requestId = 1L;
        adminId = 999L;

        lecture = Lecture.builder()
                .m3u8Path("old-path/output.m3u8")
                .title("테스트 강의")
                .build();

        pendingRequest = LectureModificationRequest.createPending(
                lecture,
                100L,
                "강의 영상 수정 요청 사유",
                "new-path/output.m3u8"
        );
        ReflectionTestUtils.setField(pendingRequest, "id", requestId);
    }

    @Test
    void 변경_요청_승인_시_상태가_APPROVED로_바뀌고_강의의_영상_경로가_갱신된다() {
        given(requestRepository.findByIdWithLecture(requestId)).willReturn(Optional.of(pendingRequest));

        approvalService.approveRequest(requestId, adminId);

        assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(pendingRequest.getManagedBy()).isEqualTo(adminId);
        assertThat(lecture.getM3u8Path()).isEqualTo("new-path/output.m3u8");
    }

    @Test
    void 승인_시_요청_내역이_존재하지_않으면_예외를_던진다() {
        given(requestRepository.findByIdWithLecture(requestId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> approvalService.approveRequest(requestId, adminId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.LECTURE_MODIFICATION_REQUEST_NOT_FOUND.getMessage());
    }

    @Test
    void 승인_시_요청_상태가_PENDING이_아니면_예외를_던진다() {
        pendingRequest.approve(adminId);
        given(requestRepository.findByIdWithLecture(requestId)).willReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> approvalService.approveRequest(requestId, adminId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    void 변경_요청_반려_시_상태가_REJECTED로_바뀌고_반려_사유가_적재되며_기존_강의_경로는_보존된다() {
        LectureModificationApprovalRequest approvalRequest = new LectureModificationApprovalRequest("부적절한 영상 사유");
        given(requestRepository.findByIdWithLecture(requestId)).willReturn(Optional.of(pendingRequest));

        approvalService.rejectRequest(requestId, adminId, approvalRequest);

        assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(pendingRequest.getRejectedReason()).isEqualTo("부적절한 영상 사유");
        assertThat(pendingRequest.getManagedBy()).isEqualTo(adminId);
        assertThat(lecture.getM3u8Path()).isEqualTo("old-path/output.m3u8");
    }

    @Test
    void 반려_사유가_누락되거나_공백이면_DB_조회_없이_즉시_예외를_던진다() {
        LectureModificationApprovalRequest invalidRequest = new LectureModificationApprovalRequest("   ");

        assertThatThrownBy(() -> approvalService.rejectRequest(requestId, adminId, invalidRequest))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.REJECT_REASON_REQUIRED.getMessage());

        verify(requestRepository, never()).findByIdWithLecture(requestId);
    }

    @Test
    void 반려_시_요청_상태가_PENDING이_아니면_예외를_던진다() {
        pendingRequest.approve(adminId);
        LectureModificationApprovalRequest approvalRequest = new LectureModificationApprovalRequest("이미 처리된 요청");
        given(requestRepository.findByIdWithLecture(requestId)).willReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> approvalService.rejectRequest(requestId, adminId, approvalRequest))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }
}