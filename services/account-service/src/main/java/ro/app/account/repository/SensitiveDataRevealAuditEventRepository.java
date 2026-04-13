package ro.app.account.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.account.dto.request.SensitiveDataRevealReasonCode;
import ro.app.account.model.entity.SensitiveDataRevealAuditEvent;

public interface SensitiveDataRevealAuditEventRepository extends JpaRepository<SensitiveDataRevealAuditEvent, Long> {
    Page<SensitiveDataRevealAuditEvent> findByReasonCode(SensitiveDataRevealReasonCode reasonCode, Pageable pageable);
}
