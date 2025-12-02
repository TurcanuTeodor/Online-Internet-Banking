package ro.app.banking.security;

import ro.app.banking.model.User;
import ro.app.banking.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository){
        this.userRepository= userRepository;
    }

    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                    .orElseThrow(()->new UsernameNotFoundException("User not found with username/email: " + usernameOrEmail)
                );

        return new UserPrincipal(user);
    }
}
