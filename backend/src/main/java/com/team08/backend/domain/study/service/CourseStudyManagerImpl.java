package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseStudyManagerImpl implements CourseStudyManager {

    @Transactional
    @Override
    public Long createForCourse(CourseStudyCreateCommand command) {
        return 0L;
    }
}
