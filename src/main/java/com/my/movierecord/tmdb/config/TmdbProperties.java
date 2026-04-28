package com.my.movierecord.tmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tmdb.api")
public record TmdbProperties(String baseUrl, String imageBaseUrl, String token) {}
