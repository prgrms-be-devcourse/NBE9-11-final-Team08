package com.team08.backend.support.security;

import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockLoginUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockLoginUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockLoginUser annotation) {
        LoginUserDto loginUser = new LoginUserDto(
                annotation.id(),
                annotation.email(),
                annotation.nickname(),
                annotation.role()
        );
        LoginUserPrincipal principal = LoginUserPrincipal.from(loginUser);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.authorities()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
