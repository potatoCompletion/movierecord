package com.my.movierecord.config;

import com.my.movierecord.tmdb.config.TmdbProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

    @Bean
    public RestClient tmdbRestClient(TmdbProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token())
                .build();
    }

    @Bean
    public RestClient tmdbImageRestClient(TmdbProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .baseUrl(props.imageBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
