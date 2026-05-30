package com.couponrush.member.service;

import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import com.couponrush.global.jwt.JwtProvider;
import com.couponrush.global.jwt.TokenBlacklistRepository;
import com.couponrush.member.domain.Member;
import com.couponrush.member.dto.LoginRequest;
import com.couponrush.member.dto.SignUpRequest;
import com.couponrush.member.dto.TokenResponse;
import com.couponrush.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    private static final String BEARER_PREFIX = "Bearer ";

    @Transactional
    public void signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtProvider.createToken(member.getId(), member.getEmail());
        return TokenResponse.of(token);
    }

    public void logout(String authorizationHeader) {
        String token = resolveToken(authorizationHeader);
        if (token == null || !jwtProvider.validateToken(token)) {
            return;
        }
        long remainingMillis = jwtProvider.getRemainingMillis(token);
        if (remainingMillis > 0) {
            tokenBlacklistRepository.blacklist(token, remainingMillis);
        }
    }

    private String resolveToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
