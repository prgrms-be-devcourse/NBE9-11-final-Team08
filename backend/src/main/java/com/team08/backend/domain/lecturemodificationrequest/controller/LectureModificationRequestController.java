package com.team08.backend.domain.lecturemodificationrequest.controller;

import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationRequestCreateDto;
import com.team08.backend.domain.lecturemodificationrequest.service.LectureModificationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lecture-modifications")
@RequiredArgsConstructor
public class LectureModificationRequestController {

    private final LectureModificationRequestService modificationService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Void> createRequest(
            @RequestPart("request") @Valid LectureModificationRequestCreateDto requestDto,
            @RequestPart("video") MultipartFile videoFile
    ) {
        // TODO: SecurityContext 또는 커스텀 애노테이션에서 실제 로그인한 강사 ID 추출 필요
        Long instructorId = 1L;

        modificationService.createRequest(
                requestDto.getLectureId(),
                instructorId,
                requestDto.getDescription(),
                videoFile
        );

        return ResponseEntity.ok().build();
    }
}