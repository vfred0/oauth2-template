package lt.satsyuk.service.core.rbac;

import lt.satsyuk.data.entities.core.audit.AuthAuditOutcome;

public record AuthAuditEvent(
        String subject,
        Long apiKeyId,
        String clientIp,
        String userAgent,
        AuthAuditOutcome outcome,
        String requestPath,
        String requestMethod) {
}
