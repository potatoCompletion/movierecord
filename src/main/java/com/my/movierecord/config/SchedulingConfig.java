package com.my.movierecord.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화 설정.
 * @Scheduled 어노테이션이 동작하려면 이 설정이 필요하다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
