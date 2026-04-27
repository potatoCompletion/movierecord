package com.my.movierecord.auth.domain;

import com.my.movierecord.auth.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_user_provider", columnNames = {"provider", "provider_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserStatus status;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(length = 10)
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String username, String password, String name, UserStatus status, String role, String provider, String providerId) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.status = status != null ? status : UserStatus.PENDING;
        this.role = role != null ? role : "ROLE_USER";
        this.provider = provider;
        this.providerId = providerId;
    }

    public void approve() {
        this.status = UserStatus.ACTIVE;
    }
}
