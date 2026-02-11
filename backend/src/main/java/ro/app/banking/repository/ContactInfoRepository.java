package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.banking.model.embedded.ContactInfo;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    ContactInfo findByClientId(Long clientId);
}
