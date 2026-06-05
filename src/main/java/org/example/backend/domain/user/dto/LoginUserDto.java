package org.example.backend.domain.user.dto;

public record LoginUserDto(
    Long id,
    String email,
    String name,
    String role
) {
}