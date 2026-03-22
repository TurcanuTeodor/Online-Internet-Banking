package ro.app.client.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.app.client.model.entity.ContactInfo;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    ContactInfo findByClientId(Long clientId);

    @Modifying
    @Query("DELETE FROM ContactInfo c WHERE c.client.id = :clientId")
    void deleteAllForClientId(@Param("clientId") Long clientId);
}
