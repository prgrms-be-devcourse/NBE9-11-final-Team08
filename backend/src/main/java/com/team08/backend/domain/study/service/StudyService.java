package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;

    public List<StudySummaryResponse> getMyStudies(Long userId) {

        List<Study> studies = studyRepository.findActiveStudiesByMemberUserId(userId);

        return studies.stream()
                .map(StudySummaryResponse::from)
                .toList();
    }
}
