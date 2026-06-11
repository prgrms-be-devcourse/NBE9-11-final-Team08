package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.StudySummaryResponse;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @InjectMocks
    private StudyService studyService;

    @Test
    void 스터디_목록을_조회하면_참여중인_Active_상태의_스터디_목록이_조회된다() {
        // given
        Long userId = 1L;

        Study study = TestEntityFactory.activeStudy(10L);
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
}
