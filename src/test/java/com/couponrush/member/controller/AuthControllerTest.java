package com.couponrush.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.couponrush.member.dto.LoginRequest;
import com.couponrush.member.dto.SignUpRequest;
import com.couponrush.member.dto.TokenResponse;
import com.couponrush.member.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AuthService authService;

    @Test
    void 회원가입_성공시_200과_success를_반환한다() throws Exception {
        SignUpRequest request = new SignUpRequest("new@test.com", "password1", "닉네임");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(authService).signUp(any(SignUpRequest.class));
    }

    @Test
    void 회원가입_이메일형식이_틀리면_400을_반환한다() throws Exception {
        SignUpRequest request = new SignUpRequest("not-an-email", "password1", "닉네임");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 로그인_성공시_토큰을_반환한다() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(TokenResponse.of("TOKEN"));
        LoginRequest request = new LoginRequest("user@test.com", "password1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("TOKEN"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void 로그아웃_성공시_200과_success를_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(authService).logout("Bearer token");
    }
}
