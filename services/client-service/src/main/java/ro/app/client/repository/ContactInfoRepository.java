package ro.app.client.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.client.model.embedded.ContactInfo;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    ContactInfo findByClientId(Long clientId);
}
