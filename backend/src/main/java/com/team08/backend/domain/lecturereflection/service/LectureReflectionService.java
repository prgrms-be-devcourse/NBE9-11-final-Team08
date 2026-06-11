package com.team08.backend.domain.lecturereflection.service;

import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturereflection.dto.LectureReflectionResponse;
import com.team08.backend.domain.lecturereflection.entity.LectureReflection;
import com.team08.backend.domain.lecturereflection.repository.LectureReflectionRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LectureReflectionService {

    private final LectureReflectionRepository reflectionRepository;
    private final LectureRepository lectureRepository;

    /** 회고 작성 — (사용자, 강의) 기준 1개만 허용 */
    @Transactional
    public LectureReflectionResponse createReflection(Long userId, Long lectureId, String content) {
        lectureRepository.findById(lectureId)
                .filter(l -> l.getDeletedAt() == null)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        if (reflectionRepository.existsByUserIdAndLectureId(userId, lectureId)) {
            throw new CustomException(ErrorCode.REFLECTION_ALREADY_EXISTS);
        }

        LectureReflection reflection = LectureReflection.create(userId, lectureId, content);
        return LectureReflectionResponse.from(reflectionRepository.save(reflection));
    }

    /** 회고 수정 — 작성자 검증 후 수정 */
    @Transactional
    public LectureReflectionResponse updateReflection(Long reflectionId, Long userId, String content) {
        LectureReflection reflection = reflectionRepository.findById(reflectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RETROSPECTION_NOT_FOUND));

        if (!reflection.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.REFLECTION_ACCESS_DENIED);
        }

        reflection.update(content);
        return LectureReflectionResponse.from(reflectionRepository.save(reflection));
    }

    /** 회고 조회 — (사용자, 강의) 기준 단건 조회 */
    @Transactional(readOnly = true)
    public LectureReflectionResponse getReflection(Long userId, Long lectureId) {
        LectureReflection reflection = reflectionRepository.findByUserIdAndLectureId(userId, lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.RETROSPECTION_NOT_FOUND));
        return LectureReflectionResponse.from(reflection);
    }
}
