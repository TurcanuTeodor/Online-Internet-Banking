package ro.app.banking.repository;

import ro.app.banking.model.User;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.usernameOrEmail = :val")
    Optional<User> findByUsernameOrEmail(@Param("val") String usernameOrEmail);

    @Query("SELECT CASE WHEN COUNT(u)>0 THEN true ELSE false END FROM User u WHERE u.usernameOrEmail = :val")
    boolean existsByUsernameOrEmail(@Param("val") String usernameOrEmail);

    Optional<User> findByClientId(Long clientId);
}