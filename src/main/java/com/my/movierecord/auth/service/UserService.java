package com.my.movierecord.auth.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.dto.SignupForm;
import com.my.movierecord.auth.exception.UserAlreadyExistsException;
import com.my.movierecord.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직을 담당하는 서비스.
 * Spring Security의 UserDetailsService를 구현하여
 * 로그인 시 사용자 정보를 로드하고, 회원가입 처리를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Spring Security에서 사용자명으로 사용자 정보를 조회한다.
     * 조회된 사용자명과 암호화된 비밀번호를 기반으로 UserDetails를 반환한다.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }

    /**
     * 새 사용자의 회원가입을 처리한다.
     * 중복된 사용자명이 있으면 UserAlreadyExistsException을 throw한다.
     * 비밀번호는 BCrypt로 암호화한 후 저장한다.
     */
    @Transactional
    public void signup(SignupForm form) {
        // 사용자명 중복 확인
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new UserAlreadyExistsException(form.getUsername());
        }
        // 비밀번호를 BCrypt로 암호화하여 저장
        User user = User.builder()
                .username(form.getUsername())
                .password(passwordEncoder.encode(form.getPassword()))
                .build();
        userRepository.save(user);
    }
}
