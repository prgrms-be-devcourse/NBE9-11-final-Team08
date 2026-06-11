package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.StudySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    public List<StudySummaryResponse> getMyStudies(Long userId) {
        return new ArrayList<>();
    }
}
