package com.my.movierecord.kobis.config;

import kr.or.kobis.kobisopenapi.consumer.rest.KobisOpenAPIRestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KobisConfig {

    @Bean
    public KobisOpenAPIRestService kobisOpenAPIRestService(KobisProperties kobisProperties) {
        return new KobisOpenAPIRestService(kobisProperties.key());
    }
}
