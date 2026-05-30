package com.couponrush.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import com.couponrush.global.jwt.JwtProvider;
import com.couponrush.global.jwt.TokenBlacklistRepository;
import com.couponrush.member.domain.Member;
import com.couponrush.member.dto.LoginRequest;
import com.couponrush.member.dto.SignUpRequest;
import com.couponrush.member.dto.TokenResponse;
import com.couponrush.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;
    @InjectMocks
    private AuthService authService;

    @Test
    void 가입_중복이메일이면_예외를_던진다() {
        given(memberRepository.existsByEmail("dup@test.com")).willReturn(true);
        SignUpRequest request = new SignUpRequest("dup@test.com", "password1", "닉네임");

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void 가입_정상이면_비밀번호를_인코딩해_저장한다() {
        given(memberRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("ENCODED");
        SignUpRequest request = new SignUpRequest("new@test.com", "password1", "닉네임");

        authService.signUp(request);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getPassword()).isEqualTo("ENCODED");
        assertThat(saved.getNickname()).isEqualTo("닉네임");
    }

    @Test
    void 로그인_미존재이메일이면_예외를_던진다() {
        given(memberRepository.findByEmail("none@test.com")).willReturn(Optional.empty());
        LoginRequest request = new LoginRequest("none@test.com", "password1");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 로그인_비밀번호불일치면_예외를_던진다() {
        Member member = Member.builder().email("user@test.com").password("ENCODED").nickname("닉네임").build();
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", "ENCODED")).willReturn(false);
        LoginRequest request = new LoginRequest("user@test.com", "wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 로그인_성공시_토큰을_반환한다() {
        Member member = Member.builder().email("user@test.com").password("ENCODED").nickname("닉네임").build();
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password1", "ENCODED")).willReturn(true);
        given(jwtProvider.createToken(any(), anyString())).willReturn("TOKEN");
        LoginRequest request = new LoginRequest("user@test.com", "password1");

        TokenResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("TOKEN");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void 로그아웃_유효한토큰이면_잔여TTL로_블랙리스트에_등록한다() {
        given(jwtProvider.validateToken("token")).willReturn(true);
        given(jwtProvider.getRemainingMillis("token")).willReturn(5_000L);

        authService.logout("Bearer token");

        verify(tokenBlacklistRepository).blacklist("token", 5_000L);
    }

    @Test
    void 로그아웃_무효한토큰이면_아무것도_하지_않는다() {
        given(jwtProvider.validateToken("bad")).willReturn(false);

        authService.logout("Bearer bad");

        verify(tokenBlacklistRepository, never()).blacklist(anyString(), anyLong());
    }

    @Test
    void 로그아웃_헤더가_없으면_아무것도_하지_않는다() {
        authService.logout(null);

        verify(tokenBlacklistRepository, never()).blacklist(anyString(), anyLong());
    }
}
