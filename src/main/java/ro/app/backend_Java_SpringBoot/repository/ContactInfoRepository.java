package ro.app.backend_Java_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.backend_Java_SpringBoot.model.ContactInfo;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    ContactInfo findByClientId(Long clientId);
}
