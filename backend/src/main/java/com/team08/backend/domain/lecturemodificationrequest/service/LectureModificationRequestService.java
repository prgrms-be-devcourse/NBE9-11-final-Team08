package com.team08.backend.domain.lecturemodificationrequest.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.service.MediaEncodingService;
import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationRequestCreateDto;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LectureModificationRequestService {

    private final LectureRepository lectureRepository;
    private final MediaEncodingService mediaEncodingService;

    @Transactional(readOnly = true)
    public void createRequest(LectureModificationRequestCreateDto dto, Long instructorId, MultipartFile videoFile) {
        Lecture lecture = lectureRepository.findById(dto.lectureId())
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        validateCourseOwnership(lecture, instructorId);

        String targetDirName = UUID.randomUUID().toString();

        java.io.File tempFile;
        try {
            java.nio.file.Path tempPath = java.nio.file.Files.createTempFile("lecture-mod-temp-upload-", ".mp4");
            tempFile = tempPath.toFile();
            videoFile.transferTo(tempFile);
        } catch (java.io.IOException e) {
            throw new CustomException(ErrorCode.VIDEO_UPLOAD_FAILED);
        }

        mediaEncodingService.encodeModificationToHls(tempFile, targetDirName, dto.lectureId(), dto.description(), instructorId);
    }

    private void validateCourseOwnership(Lecture lecture, Long instructorId) {
        Long courseOwnerId = lecture.getChapter().getCourse().getInstructorId();
        if (!courseOwnerId.equals(instructorId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_COURSE_OWNER);
        }
    }
}