package ro.app.banking.repository;

import ro.app.banking.model.User;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    boolean existsByUsernameOrEmail(String usernameOrEmail);

    Optional<User> findByClientId(Long clientId);
}