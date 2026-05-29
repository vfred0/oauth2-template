package lt.satsyuk.service;

import lt.satsyuk.model.AuthAuditOutcome;

public record AuthAuditEvent(
        String subject,
        Long apiKeyId,
        String clientIp,
        String userAgent,
        AuthAuditOutcome outcome,
        String requestPath,
        String requestMethod) {
}
