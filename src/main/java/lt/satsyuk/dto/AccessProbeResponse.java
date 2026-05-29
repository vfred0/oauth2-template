package lt.satsyuk.dto;

import java.util.List;

public record AccessProbeResponse(
        String subject,
        String granted,
        List<String> authorities) {
}
