package com.my.movierecord.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정.
 * @CreatedDate, @LastModifiedDate 등의 어노테이션으로 자동 관리되는
 * 타임스탬프를 활성화한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
