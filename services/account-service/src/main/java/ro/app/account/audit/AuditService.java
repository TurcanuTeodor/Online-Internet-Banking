package ro.app.account.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private static final Logger audit = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void log(String action, Long actorClientId, String actorRole, Long targetClientId, String details) {
        audit.info("ACTION={} | ACTOR={} (role={}) | TARGET={} | DETAILS={} | TIME={}",
                action, actorClientId, actorRole, targetClientId, details, LocalDateTime.now());
    }
}
