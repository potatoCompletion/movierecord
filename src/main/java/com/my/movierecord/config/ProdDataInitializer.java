package com.my.movierecord.config;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("환경변수 ADMIN_PASSWORD가 설정되지 않았습니다.");
        }

        if (userRepository.existsByUsername("admin")) {
            log.info("admin 계정이 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(adminPassword))
                .name("관리자")
                .status(UserStatus.ACTIVE)
                .role("ROLE_ADMIN")
                .build();
        userRepository.save(admin);
        log.info("admin 계정을 생성했습니다.");
    }
}
