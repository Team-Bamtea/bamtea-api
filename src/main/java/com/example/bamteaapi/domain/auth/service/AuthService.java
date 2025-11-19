package com.example.bamteaapi.domain.auth.service;

import com.example.bamteaapi.domain.auth.dto.AuthResponse;
import com.example.bamteaapi.domain.auth.dto.SignInRequest;
import com.example.bamteaapi.domain.auth.dto.SignUpRequest;
import com.example.bamteaapi.domain.user.dto.UserResponse;
import com.example.bamteaapi.domain.user.entity.User;
import com.example.bamteaapi.domain.user.repository.UserRepository;
import com.example.bamteaapi.global.exception.CustomException;
import com.example.bamteaapi.global.exception.ErrorCode;
import com.example.bamteaapi.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateToken(savedUser.getEmail());

        return AuthResponse.of(accessToken, UserResponse.from(savedUser));
    }

    public AuthResponse signIn(SignInRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("User signed in: {}", user.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.of(accessToken, UserResponse.from(user));
    }
}
