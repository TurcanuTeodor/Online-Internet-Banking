package ro.app.banking.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ro.app.banking.model.User;

//UserPrincipal is an adapter between the User entity(db model)
//and Spring Security's UserDetails inteface

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

    //Returns the authorities/roles granted to the user
    //Spring Security expects roles to be prefixed with "ROLE_"
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Example: DB role "USER" -> "ROLE_USER"
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
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
        return true; // Can be extended later with an "expired" field
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Can be extended later with a "locked" flag
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Can be extended with "credentialsExpired"
    }

    @Override
    public boolean isEnabled() {
        return true; // Can be extended with an "enabled" field
    }
}
