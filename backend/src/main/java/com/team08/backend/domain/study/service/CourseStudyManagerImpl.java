package com.team08.backend.domain.study.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.exception.DuplicateStudyException;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseStudyManagerImpl implements CourseStudyManager {
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 스터디가 롤백되어도 강좌는 사라지지 않도록
    @Override
    public Long createForCourse(CourseStudyCreateCommand command) {
        if (studyRepository.existsByCourseId(command.courseId())) {
            throw new DuplicateStudyException();
        }

        User owner = userRepository.findById(command.ownerId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        Study study = Study.createForCourse(owner, course, command.title(), command.description());
        studyRepository.save(study);

        StudyMember studyMember = StudyMember.owner(owner, study);
        studyMemberRepository.save(studyMember);

        return study.getId();
    }
}