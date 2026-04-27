package com.my.movierecord.auth.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.dto.SignupForm;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.exception.UserAlreadyExistsException;
import com.my.movierecord.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (user.getProvider() != null) {
            // OAuth-only accounts must use social login, not form login
            throw new UsernameNotFoundException("Use social login for this account");
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority(user.getRole()))
                .disabled(user.getStatus() == UserStatus.PENDING)
                .build();
    }

    @Transactional
    public void signup(SignupForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new UserAlreadyExistsException(form.getUsername());
        }
        User user = User.builder()
                .username(form.getUsername())
                .password(passwordEncoder.encode(form.getPassword()))
                .name(form.getName())
                .status(UserStatus.PENDING)
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findPendingUsers() {
        return userRepository.findAllByStatus(UserStatus.PENDING);
    }

    @Transactional
    public void approveUser(Long id) {
        userRepository.findById(id).ifPresent(User::approve);
    }
}
