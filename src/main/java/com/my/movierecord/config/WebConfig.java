package com.my.movierecord.config;

import com.my.movierecord.common.web.CookieFlashMapManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

/**
 * 웹 리소스 처리 설정.
 * 외부 디렉토리(/uploads/**)의 파일을 정적 리소스로 제공하도록 설정한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

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

    /**
     * /uploads/** 경로의 요청을 설정된 업로드 디렉토리로 매핑한다.
     * 이를 통해 업로드된 이미지 파일에 HTTP로 접근할 수 있다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
