package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.study.fixture.StudyFixture;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private StudyService studyService;

    @Test
    void 스터디_목록을_조회하면_참여중인_Active_상태의_스터디_목록이_조회된다() {
        // given
        Long userId = 1L;
        Study study = StudyFixture.activeStudy();
        List<Study> studies = List.of(study);

        given(studyRepository.findActiveStudiesByMemberUserId(userId))
                .willReturn(studies);

        // when
        List<StudySummaryResponse> result = studyService.getMyStudies(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studyId()).isEqualTo(study.getId());

        verify(studyRepository).findActiveStudiesByMemberUserId(userId);
    }

    @Test
    void studyId로_ACTIVE_스터디_상세를_조회한다() {
        Long userId = 1L;
        Study study = StudyFixture.activeStudy();
        StudyMember member = member(StudyMemberRole.MEMBER);

        given(studyRepository.findByIdAndStatusNot(study.getId(), StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                study.getId(), userId, StudyMemberStatus.ACTIVE
        )).willReturn(Optional.of(member));

        StudyDetailResponse result = studyService.getStudyDetail(study.getId(), userId);

        assertThat(result.studyId()).isEqualTo(study.getId());
        assertThat(result.courseId()).isEqualTo(study.getCourse().getId());
        assertThat(result.status()).isEqualTo(StudyStatus.ACTIVE);
        assertThat(result.myRole()).isEqualTo(StudyMemberRole.MEMBER);
    }

    @Test
    void courseId로_READONLY_스터디_상세를_조회한다() {
        Long userId = 1L;
        Study study = StudyFixture.activeStudy();
        ReflectionTestUtils.setField(study, "status", StudyStatus.READONLY);
        StudyMember member = member(StudyMemberRole.OWNER);

        given(studyRepository.findByCourseIdAndStatusNot(study.getCourse().getId(), StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                study.getId(), userId, StudyMemberStatus.ACTIVE
        )).willReturn(Optional.of(member));

        StudyDetailResponse result = studyService.getStudyDetailByCourseId(
                study.getCourse().getId(), userId
        );

        assertThat(result.studyId()).isEqualTo(study.getId());
        assertThat(result.status()).isEqualTo(StudyStatus.READONLY);
        assertThat(result.myRole()).isEqualTo(StudyMemberRole.OWNER);
    }

    @Test
    void DRAFT_스터디는_studyId로_조회할_수_없다() {
        Long studyId = 1L;

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getStudyDetail(studyId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);
    }

    @Test
    void DRAFT_스터디는_courseId로_조회할_수_없다() {
        Long courseId = 1L;

        given(studyRepository.findByCourseIdAndStatusNot(courseId, StudyStatus.DRAFT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getStudyDetailByCourseId(courseId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_스터디를_조회하면_찾을_수_없다() {
        Long studyId = 999L;

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getStudyDetail(studyId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);
    }

    @Test
    void ACTIVE_멤버가_아니면_스터디_상세에_접근할_수_없다() {
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByIdAndStatusNot(study.getId(), StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                study.getId(), userId, StudyMemberStatus.ACTIVE
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getStudyDetail(study.getId(), userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);
    }

    private StudyMember member(StudyMemberRole role) {
        StudyMember member = mock(StudyMember.class);
        when(member.getRole()).thenReturn(role);
        return member;
    }
}
