package com.team08.backend.global.auth.principal;

import com.team08.backend.domain.user.dto.LoginUserDto;

public record LoginUserPrincipal(LoginUserDto user) {
}