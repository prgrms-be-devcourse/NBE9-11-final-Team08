package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.fixture.StudyFixture;
import com.team08.backend.domain.study.repository.StudyRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private StudyService studyService;

    @Test
    void 스터디_목록을_조회하면_조회_가능한_상태의_스터디_목록이_조회된다() {
        // given
        Long userId = 1L;
        Study study = StudyFixture.activeStudy();
        List<Study> studies = List.of(study);

        given(studyRepository.findVisibleStudiesByUserId(userId))
                .willReturn(studies);

        // when
        List<StudySummaryResponse> result = studyService.getMyStudies(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studyId()).isEqualTo(study.getId());

        verify(studyRepository).findVisibleStudiesByUserId(userId);
    }

    @Test
    void studyId로_조회_가능한_스터디_상세를_조회한다() {
        Long userId = 1L;
        Study study = StudyFixture.activeStudy();
        StudyMember member = member(StudyMemberRole.MEMBER);

        given(studyRepository.findByIdWithCourse(study.getId()))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                study.getId(),
                userId,
                StudyMemberStatus.ACTIVE
        ))
                .willReturn(Optional.of(member));

        StudyDetailResponse result = studyService.getStudyDetail(study.getId(), userId);

        assertThat(result.studyId()).isEqualTo(study.getId());
        assertThat(result.courseId()).isEqualTo(study.getCourse().getId());
        assertThat(result.status()).isEqualTo(StudyStatus.ACTIVE);
        assertThat(result.myRole()).isEqualTo(StudyMemberRole.MEMBER);

        verify(studyMemberRepository).findByStudyIdAndUserIdAndStatus(
                study.getId(),
                userId,
                StudyMemberStatus.ACTIVE
        );
    }

    @Test
    void studyId로_조회_가능한_스터디_상세는_비멤버에게_null_role을_반환한다() {
        Long userId = 1L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByIdWithCourse(study.getId()))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                study.getId(),
                userId,
                StudyMemberStatus.ACTIVE
        )).willReturn(Optional.empty());

        StudyDetailResponse result = studyService.getStudyDetail(study.getId(), userId);

        assertThat(result.studyId()).isEqualTo(study.getId());
        assertThat(result.myRole()).isNull();
    }

    @Test
    void studyId로_조회_가능한_스터디_상세는_비로그인_사용자에게_null_role을_반환한다() {
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByIdWithCourse(study.getId()))
                .willReturn(Optional.of(study));

        StudyDetailResponse result = studyService.getStudyDetail(study.getId(), null);

        assertThat(result.studyId()).isEqualTo(study.getId());
        assertThat(result.myRole()).isNull();

        verifyNoInteractions(studyMemberRepository);
    }

    @Test
    void courseId로_조회_가능한_스터디_id를_조회한다() {
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByCourseIdWithCourse(study.getCourse().getId()))
                .willReturn(Optional.of(study));

        Long result = studyService.getStudyIdByCourseId(study.getCourse().getId());

        assertThat(result).isEqualTo(study.getId());

        verifyNoInteractions(studyMemberRepository);
    }

    @Test
    void 존재하지_않는_스터디를_조회하면_찾을_수_없다() {
        Long studyId = 999L;

        given(studyRepository.findByIdWithCourse(studyId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getStudyDetail(studyId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);
    }

    private StudyMember member(StudyMemberRole role) {
        StudyMember member = mock(StudyMember.class);
        when(member.getRole()).thenReturn(role);
        return member;
    }
}
