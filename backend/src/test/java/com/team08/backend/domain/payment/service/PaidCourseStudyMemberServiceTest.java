package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaidCourseStudyMemberServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 100L;
    private static final Long STUDY_ID = 200L;
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-06-18T19:00:00");

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private UserRepository userRepository;

    private PaidCourseStudyMemberService service;

    @BeforeEach
    void setUp() {
        service = new PaidCourseStudyMemberService(studyRepository, studyMemberRepository, userRepository);
    }

    @Test
    void 결제한_강의의_스터디에_MEMBER로_참여시킨다() {
        User user = TestEntityFactory.user(USER_ID);
        Study study = study(StudyStatus.ACTIVE);

        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(studyRepository.findByCourseIdInAndStatusIn(List.of(COURSE_ID), List.of(StudyStatus.ACTIVE, StudyStatus.READONLY)))
                .willReturn(List.of(study));
        given(studyMemberRepository.findByStudyIdAndUserId(STUDY_ID, USER_ID)).willReturn(Optional.empty());

        service.joinAsMember(USER_ID, List.of(COURSE_ID), NOW);

        ArgumentCaptor<StudyMember> captor = ArgumentCaptor.forClass(StudyMember.class);
        verify(studyMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getStudy()).isEqualTo(study);
        assertThat(captor.getValue().getStatus()).isEqualTo(StudyMemberStatus.ACTIVE);
        assertThat(captor.getValue().getJoinedAt()).isEqualTo(NOW);
    }

    @Test
    void 환불한_강의의_스터디_MEMBER를_LEFT로_변경한다() {
        Study study = study(StudyStatus.ACTIVE);
        StudyMember member = StudyMember.member(TestEntityFactory.user(USER_ID), study, NOW.minusDays(1));

        given(studyRepository.findByCourseIdInAndStatusIn(List.of(COURSE_ID), List.of(StudyStatus.ACTIVE, StudyStatus.READONLY)))
                .willReturn(List.of(study));
        given(studyMemberRepository.findByStudyIdAndUserId(STUDY_ID, USER_ID)).willReturn(Optional.of(member));

        service.leaveMember(USER_ID, List.of(COURSE_ID), NOW);

        assertThat(member.getStatus()).isEqualTo(StudyMemberStatus.LEFT);
        assertThat(member.getLeftAt()).isEqualTo(NOW);
        verify(studyMemberRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Study study(StudyStatus status) {
        Study study = TestEntityFactory.study(STUDY_ID, status);
        ReflectionTestUtils.setField(study, "course", TestEntityFactory.course(COURSE_ID));
        return study;
    }
}
