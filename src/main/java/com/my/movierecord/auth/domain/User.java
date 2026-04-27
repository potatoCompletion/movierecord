package com.my.movierecord.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자 정보를 저장하는 JPA 엔티티.
 * 사용자명, 비밀번호, 생성 시간 정보를 관리한다.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 고유한 사용자명 (최대 50자)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // 암호화된 비밀번호
    @Column(nullable = false)
    private String password;

    // JPA Auditing으로 자동 관리되는 생성 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
