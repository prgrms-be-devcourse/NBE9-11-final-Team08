package org.example.backend.domain.user.controller;

import org.example.backend.domain.user.dto.LoginUserDto;
import org.example.backend.global.auth.principal.LoginUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/api/auth/me")
    public LoginUserDto getMyInfo(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return principal.user();
    }
}