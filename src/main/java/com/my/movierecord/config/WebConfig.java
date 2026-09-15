package com.my.movierecord.config;

import com.my.movierecord.common.web.CookieFlashMapManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

/**
 * 웹 MVC 설정.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 리다이렉트 flash 속성을 세션 대신 쿠키에 보관한다. 기본 SessionFlashMapManager 는 flash 를 쓸 때마다
     * 세션을 만들어 stateless 인증 구조에서도 JSESSIONID 가 발급되므로 교체한다.
     * Boot 의 기본 빈은 같은 이름의 빈이 있으면 물러난다(@ConditionalOnMissingBean(name = "flashMapManager")).
     */
    @Bean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
    public FlashMapManager flashMapManager(ObjectMapper objectMapper,
                                           @Value("${app.cookie.secure:false}") boolean secure) {
        return new CookieFlashMapManager(objectMapper, secure);
    }
}
