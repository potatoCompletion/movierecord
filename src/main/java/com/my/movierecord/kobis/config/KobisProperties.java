package com.my.movierecord.kobis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kobis.api")
public record KobisProperties(String key) {}
