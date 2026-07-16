package com.my.movierecord.config;

import com.my.movierecord.common.client.ExternalApiErrorHandler;
import com.my.movierecord.tmdb.config.TmdbProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration IMAGE_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient tmdbRestClient(TmdbProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token())
                .requestFactory(requestFactory(CONNECT_TIMEOUT, READ_TIMEOUT))
                .defaultStatusHandler(HttpStatusCode::isError, ExternalApiErrorHandler.forApi("tmdb"))
                .build();
    }

    @Bean
    public RestClient tmdbImageRestClient(TmdbProperties props) {
        return RestClient.builder()
                .baseUrl(props.imageBaseUrl())
                .requestFactory(requestFactory(IMAGE_TIMEOUT, IMAGE_TIMEOUT))
                .defaultStatusHandler(HttpStatusCode::isError, ExternalApiErrorHandler.forApi("tmdb-image"))
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
