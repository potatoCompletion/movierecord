package com.my.movierecord.config;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Spring 컨텍스트 없이 시딩 분기만 검증한다. */
@ExtendWith(MockitoExtension.class)
class ProdDataInitializerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    private void stubEncoder() {
        given(passwordEncoder.encode(anyString())).willAnswer(inv -> "encoded:" + inv.getArgument(0));
    }

    private ProdDataInitializer initializer(String adminPassword, String demoPassword) {
        return new ProdDataInitializer(userRepository, passwordEncoder, adminPassword, demoPassword);
    }

    @Test
    void 두_비밀번호가_모두_있으면_admin과_demo를_생성한다() {
        stubEncoder();
        given(userRepository.existsByUsername(anyString())).willReturn(false);

        initializer("admin-pw", "demo-pw").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        User demo = captor.getAllValues().get(1);
        assertThat(demo.getUsername()).isEqualTo("demo");
        assertThat(demo.getName()).isEqualTo("데모");
        assertThat(demo.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(demo.getRole()).isEqualTo("ROLE_USER");
        assertThat(demo.getPassword()).isEqualTo("encoded:demo-pw");
        assertThat(captor.getAllValues().get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    void DEMO_PASSWORD가_없으면_admin만_생성하고_예외_없이_건너뛴다() {
        stubEncoder();
        given(userRepository.existsByUsername("admin")).willReturn(false);

        initializer("admin-pw", "").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        verify(userRepository, never()).existsByUsername("demo");
    }

    @Test
    void demo가_이미_존재하면_다시_만들지_않는다() {
        given(userRepository.existsByUsername("admin")).willReturn(true);
        given(userRepository.existsByUsername("demo")).willReturn(true);

        initializer("admin-pw", "demo-pw").run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void ADMIN_PASSWORD가_없으면_기동을_실패시킨다() {
        assertThatThrownBy(() -> initializer("", "demo-pw").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
        verify(userRepository, never()).save(any(User.class));
    }
}
