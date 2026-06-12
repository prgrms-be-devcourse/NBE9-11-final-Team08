package com.team08.backend.domain.user.dto;

public record LoginUserDto(
        Long id,
        String email,
        String nickname,
        String role
) {
}