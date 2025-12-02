package ro.app.backend_Java_SpringBoot.security;

import ro.app.backend_Java_SpringBoot.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Long getClientId() {
        return user.getClient().getId();
    }

    public boolean isTwoFactorEnabled() {
        return user.isTwoFactorEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE din DB ex: "USER" / "ADMIN"
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsernameOrEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // extinde cu un câmp "expired" mai târziu
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // extinde cu "locked"
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // extinde cu "credentialsExpired"
    }

    @Override
    public boolean isEnabled() {
        return true; // extinde cu "enabled"
    }
}
