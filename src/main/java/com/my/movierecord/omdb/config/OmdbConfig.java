package com.my.movierecord.omdb.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OmdbProperties.class)
public class OmdbConfig {

    @Bean
    public RestClient omdbRestClient(OmdbProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}
