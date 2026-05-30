package com.couponrush.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_인증정보를_저장한다() throws Exception {
        given(jwtProvider.validateToken("valid-token")).willReturn(true);
        given(jwtProvider.getMemberId("valid-token")).willReturn(7L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthenticationFilter(jwtProvider).doFilter(request, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 헤더가_없으면_인증정보가_없다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthenticationFilter(jwtProvider).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 유효하지_않은_토큰이면_인증정보가_없다() throws Exception {
        given(jwtProvider.validateToken("bad-token")).willReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthenticationFilter(jwtProvider).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
