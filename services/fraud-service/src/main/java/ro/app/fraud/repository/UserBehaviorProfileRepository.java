package ro.app.fraud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.fraud.model.entity.UserBehaviorProfile;

public interface UserBehaviorProfileRepository extends JpaRepository<UserBehaviorProfile, Long> {

    Optional<UserBehaviorProfile> findByClientId(Long clientId);
}
