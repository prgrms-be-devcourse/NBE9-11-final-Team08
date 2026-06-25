package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaidCourseStudyMemberService {

    private static final List<StudyStatus> JOINABLE_STUDY_STATUSES = List.of(
            StudyStatus.ACTIVE,
            StudyStatus.READONLY
    );

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;

    public void joinAsMember(Long userId, Collection<Long> courseIds, LocalDateTime joinedAt) {
        if (courseIds.isEmpty()) {
            return;
        }

        User user = userRepository.getReferenceById(userId);
        studyRepository.findByCourseIdInAndStatusIn(courseIds, JOINABLE_STUDY_STATUSES)
                .forEach(study -> createMemberIfAbsent(user, study, joinedAt));
    }

    public void leaveMember(Long userId, Collection<Long> courseIds, LocalDateTime leftAt) {
        if (courseIds.isEmpty()) {
            return;
        }

        studyRepository.findByCourseIdInAndStatusIn(courseIds, JOINABLE_STUDY_STATUSES)
                .forEach(study -> leaveMemberIfPresent(userId, study, leftAt));
    }

    private void createMemberIfAbsent(User user, Study study, LocalDateTime joinedAt) {
        if (studyMemberRepository.findByStudyIdAndUserId(study.getId(), user.getId()).isPresent()) {
            return;
        }

        studyMemberRepository.save(StudyMember.member(user, study, joinedAt));
    }

    private void leaveMemberIfPresent(Long userId, Study study, LocalDateTime leftAt) {
        studyMemberRepository.findByStudyIdAndUserId(study.getId(), userId)
                .filter(member -> member.getRole() == StudyMemberRole.MEMBER)
                .ifPresent(member -> member.leave(leftAt));
    }
}
