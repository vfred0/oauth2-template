package lt.satsyuk.repository;

import lt.satsyuk.model.AuthAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditRepository extends JpaRepository<AuthAudit, Long> {
}
