package com.my.movierecord.omdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("omdb.api")
public record OmdbProperties(String baseUrl, String key) {}
