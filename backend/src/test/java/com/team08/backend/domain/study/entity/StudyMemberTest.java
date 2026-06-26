package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.study.fixture.StudyFixture;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StudyMemberTest {

    @Test
    void OWNER_멤버가_생성된다() {
        // given
        Study study = StudyFixture.draftStudy();
        User owner = TestEntityFactory.user(1L);

        // when
        StudyMember member = StudyMember.owner(owner, study);

        // then
        assertThat(member.getStudy()).isEqualTo(study);
        assertThat(member.getUser()).isEqualTo(owner);
        assertThat(member.getRole()).isEqualTo(StudyMemberRole.OWNER);
        assertThat(member.getStatus()).isEqualTo(StudyMemberStatus.ACTIVE);
    }

    @Test
    void MEMBER_멤버가_생성되고_LEFT로_변경된다() {
        Study study = StudyFixture.activeStudy();
        User user = TestEntityFactory.user(2L);
        java.time.LocalDateTime joinedAt = java.time.LocalDateTime.parse("2026-06-18T19:00:00");
        java.time.LocalDateTime leftAt = java.time.LocalDateTime.parse("2026-06-19T19:00:00");

        StudyMember member = StudyMember.member(user, study, joinedAt);
        member.leave(leftAt);

        assertThat(member.getStudy()).isEqualTo(study);
        assertThat(member.getUser()).isEqualTo(user);
        assertThat(member.getRole()).isEqualTo(StudyMemberRole.MEMBER);
        assertThat(member.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(member.getStatus()).isEqualTo(StudyMemberStatus.LEFT);
        assertThat(member.getLeftAt()).isEqualTo(leftAt);
    }
}
