package com.team08.backend.domain.lecturemodificationrequest.controller;

import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationRequestCreateDto;
import com.team08.backend.domain.lecturemodificationrequest.service.LectureModificationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lecture-modifications")
@RequiredArgsConstructor
public class LectureModificationRequestController {

    private final LectureModificationRequestService modificationService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Void> createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("request") @Valid LectureModificationRequestCreateDto requestDto,
            @RequestPart("video") MultipartFile videoFile
    ) {
        Long instructorId = Long.valueOf(userDetails.getUsername());

        modificationService.createRequest(requestDto, instructorId, videoFile);

        return ResponseEntity.ok().build();
    }
}