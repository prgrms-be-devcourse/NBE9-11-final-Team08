package com.team08.backend.global.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.TestController.class)
@Import({
        SecurityConfig.class,
        SecurityConfigTest.TestController.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void accessToken이_없으면_인증에_실패한다() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerAccessToken이_있으면_요청할_수_있다() throws Exception {
        mockMvc.perform(get("/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token"))
                .andExpect(status().isOk());
    }

    @RestController
    static class TestController {

        @GetMapping("/test")
        String test() {
            return "ok";
        }
    }
}
