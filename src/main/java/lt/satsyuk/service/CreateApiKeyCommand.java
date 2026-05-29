package lt.satsyuk.service;

import java.time.Instant;

public record CreateApiKeyCommand(
        String subject,
        String label,
        String allowedIps,
        Instant expiresAt) {
}
