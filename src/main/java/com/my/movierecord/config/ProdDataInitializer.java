package com.my.movierecord.config;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영 기동 시 계정을 시딩한다.
 * <ul>
 *   <li>admin: {@code ADMIN_PASSWORD} 필수. 없으면 기동 실패.</li>
 *   <li>demo: 면접관 체험용 일반 계정. {@code DEMO_PASSWORD} 가 없으면 경고만 남기고 건너뛴다.</li>
 * </ul>
 * 비밀번호는 OS 환경변수를 Spring 프로퍼티로 읽는다(테스트에서 값을 주입할 수 있게 {@code @Value} 사용).
 */
@Slf4j
@Component
@Profile("prod")
public class ProdDataInitializer implements ApplicationRunner {

    static final String ADMIN_USERNAME = "admin";
    static final String DEMO_USERNAME = "demo";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String demoPassword;

    public ProdDataInitializer(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${ADMIN_PASSWORD:}") String adminPassword,
                               @Value("${DEMO_PASSWORD:}") String demoPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedDemo();
    }

    private void seedAdmin() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("환경변수 ADMIN_PASSWORD가 설정되지 않았습니다.");
        }
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            log.info("admin 계정이 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }
        userRepository.save(User.builder()
                .username(ADMIN_USERNAME)
                .password(passwordEncoder.encode(adminPassword))
                .name("관리자")
                .status(UserStatus.ACTIVE)
                .role("ROLE_ADMIN")
                .build());
        log.info("admin 계정을 생성했습니다.");
    }

    private void seedDemo() {
        if (demoPassword == null || demoPassword.isBlank()) {
            log.warn("DEMO_PASSWORD 미설정. demo 계정 시딩을 건너뜁니다.");
            return;
        }
        if (userRepository.existsByUsername(DEMO_USERNAME)) {
            log.info("demo 계정이 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }
        userRepository.save(User.builder()
                .username(DEMO_USERNAME)
                .password(passwordEncoder.encode(demoPassword))
                .name("데모")
                .status(UserStatus.ACTIVE)
                .role("ROLE_USER")
                .build());
        log.info("demo 계정을 생성했습니다.");
    }
}
