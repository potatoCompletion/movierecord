package com.my.movierecord.auth.oauth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomUserPrincipal implements UserDetails, OAuth2User {

    private final String username;
    private final String password;
    private final String displayNickname;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public CustomUserPrincipal(String username, String displayNickname, String role,
                               Map<String, Object> attributes, String nameAttributeKey) {
        this.username = username;
        this.password = null;
        this.displayNickname = displayNickname;
        this.authorities = List.of(new SimpleGrantedAuthority(role));
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    public CustomUserPrincipal(String username, String password, String displayNickname, String role) {
        this.username = username;
        this.password = password;
        this.displayNickname = displayNickname;
        this.authorities = List.of(new SimpleGrantedAuthority(role));
        this.attributes = Map.of();
        this.nameAttributeKey = "";
    }

    public String getDisplayNickname() { return displayNickname; }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public String getName() {
        if (attributes.isEmpty()) return username;
        return (String) attributes.get(nameAttributeKey);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }
}
