package com.team08.backend.domain.user.controller;

import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
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
