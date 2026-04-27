package com.my.movierecord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 인코더 설정.
 * BCrypt를 사용하여 비밀번호를 암호화한다.
 * strength 12는 bcrypt의 강도 지수로, 높을수록 더 강한 암호화를 제공한다.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCryptPasswordEncoder 빈을 생성한다.
     * strength 12로 설정하여 충분한 보안 강도를 제공한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
