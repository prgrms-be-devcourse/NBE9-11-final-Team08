package com.team08.backend.global.auth.principal;

import com.team08.backend.domain.user.dto.LoginUserDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public record LoginUserPrincipal(
        LoginUserDto user,
        List<? extends GrantedAuthority> authorities
) {

    public static LoginUserPrincipal from(LoginUserDto user) {
        return new LoginUserPrincipal(
                user,
                List.of(new SimpleGrantedAuthority(user.role()))
        );
    }
}
