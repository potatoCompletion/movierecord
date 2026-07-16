package com.my.movierecord.support;

import com.my.movierecord.auth.handler.LoginFailureHandler;
import com.my.movierecord.auth.handler.LoginSuccessHandler;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@code @WebMvcTest} 슬라이스가 {@code @Import(SecurityConfig.class)} 할 때 필요한
 * 필수 인증 핸들러 목(mock) 빈을 제공한다. {@code SecurityConfig}는
 * {@code LoginSuccessHandler}/{@code LoginFailureHandler}를 required 로 주입받는데,
 * 이 둘은 {@code @Component}라 웹 슬라이스에 자동 포함되지 않아 컨텍스트 로드가 실패한다.
 */
@TestConfiguration
public class SecurityTestConfig {

    @Bean
    LoginSuccessHandler loginSuccessHandler() {
        return Mockito.mock(LoginSuccessHandler.class);
    }

    @Bean
    LoginFailureHandler loginFailureHandler() {
        return Mockito.mock(LoginFailureHandler.class);
    }
}
