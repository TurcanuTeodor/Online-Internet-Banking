package ro.app.auth.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import ro.app.auth.model.entity.RefreshToken;
import ro.app.auth.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser(User user);

    @Query("SELECT rt " +
            "FROM RefreshToken rt " +
            "WHERE rt.user = :user " +
            "AND rt.revokedAt IS NULL " +
            "AND rt.expiryDate > CURRENT_TIMESTAMP")
    List<RefreshToken> findActiveTokensByUser(@Param("user") User user);

    long deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken rt " +
            "WHERE rt.expiryDate < :now")
    void deleteAllExpiredBefore(@Param("now") LocalDateTime now);
}
