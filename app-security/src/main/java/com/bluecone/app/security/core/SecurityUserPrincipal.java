package com.bluecone.app.security.core;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

/**
 * Security 上下文中的用户信息。
 */
@Getter
public class SecurityUserPrincipal implements UserDetails {

    private final Long userId;
    private final Long tenantId;
    private final String username;
    private final String clientType;
    private final String deviceId;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserPrincipal(Long userId, Long tenantId, String username, String clientType, String deviceId) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
        this.clientType = clientType;
        this.deviceId = deviceId;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
