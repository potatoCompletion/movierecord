package com.my.movierecord.config;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.context.annotation.Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin1234"))
                .name("관리자")
                .status(UserStatus.ACTIVE)
                .role("ROLE_ADMIN")
                .build();
        userRepository.save(admin);
    }
}
