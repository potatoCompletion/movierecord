package com.my.movierecord.auth.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.dto.SignupForm;
import com.my.movierecord.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    void signup_새사용자_인코딩된_비밀번호로_저장() {
        SignupForm form = new SignupForm();
        form.setUsername("newuser");
        form.setPassword("pass1234");
        form.setPasswordConfirm("pass1234");
        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(passwordEncoder.encode("pass1234")).willReturn("encoded");

        userService.signup(form);

        then(userRepository).should().save(any(User.class));
    }

    @Test
    void signup_중복_아이디_예외_발생() {
        SignupForm form = new SignupForm();
        form.setUsername("existing");
        form.setPassword("pass1234");
        form.setPasswordConfirm("pass1234");
        given(userRepository.existsByUsername("existing")).willReturn(true);

        assertThatThrownBy(() -> userService.signup(form))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("existing");

        then(userRepository).should(never()).save(any());
    }

    @Test
    void loadUserByUsername_존재하는_사용자_반환() {
        User user = User.builder()
                .username("testuser")
                .password("encoded_pw")
                .build();
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("testuser");

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("encoded_pw");
    }

    @Test
    void loadUserByUsername_없는_사용자_예외() {
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
