package ro.app.client.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    // ── Action name constants ────────────────────────────────────────────────
    /** Admin listed all clients (analytic view, no PII decrypted). */
    public static final String ADMIN_CLIENT_LIST  = "ADMIN_CLIENT_LIST";
    /** Admin suspended a client account. */
    public static final String ACCOUNT_FREEZE     = "ACCOUNT_FREEZE";
    /** Admin or system soft-deleted a client. */
    public static final String CLIENT_DELETE      = "CLIENT_DELETE";
    /** GDPR right-to-erasure applied for a client. */
    public static final String GDPR_ERASURE       = "GDPR_ERASURE";
    /** GDPR data export performed for a client. */
    public static final String GDPR_EXPORT        = "GDPR_EXPORT";

    private static final Logger audit = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void log(String action, Long actorClientId, String actorRole, Long targetClientId, String details) {
        audit.info("ACTION={} | ACTOR={} (role={}) | TARGET={} | DETAILS={} | TIME={}",
                action, actorClientId, actorRole, targetClientId, details, LocalDateTime.now());
    }
}
