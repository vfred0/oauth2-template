package lt.satsyuk.data.daos;

import lt.satsyuk.data.entities.core.audit.AuthAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditRepository extends JpaRepository<AuthAudit, Long> {
}
