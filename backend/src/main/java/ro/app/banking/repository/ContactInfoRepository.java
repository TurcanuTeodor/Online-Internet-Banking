package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.banking.model.ContactInfo;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    ContactInfo findByClientId(Long clientId);
}
