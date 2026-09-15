package com.my.movierecord.tmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TMDB REST API 접속 정보. 이미지 CDN 루트({@code tmdb.api.image-base-url})는
 * {@code tmdb.image.TmdbImageUrlProvider} 가 직접 바인딩한다.
 */
@ConfigurationProperties("tmdb.api")
public record TmdbProperties(String baseUrl, String token) {}
