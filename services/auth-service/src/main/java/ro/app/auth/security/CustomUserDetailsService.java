package ro.app.auth.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ro.app.auth.model.entity.User;
import ro.app.auth.repository.UserRepository;

//loads a user by username/email
//this method is auto called  by Spring Security
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository){
        this.userRepository= userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                    .orElseThrow(()->new UsernameNotFoundException("User not found with username/email: " + usernameOrEmail)
                );

        return new UserPrincipal(user); //wrap the user entity into UserPrincipal
    }
}
