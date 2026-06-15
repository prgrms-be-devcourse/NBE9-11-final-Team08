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
}
