package com.team08.backend.support.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockLoginUserSecurityContextFactory.class)
public @interface WithMockLoginUser {

    long id() default 1L;

    String email() default "test@example.com";

    String nickname() default "테스트유저";

    String role() default "ROLE_USER";
}
