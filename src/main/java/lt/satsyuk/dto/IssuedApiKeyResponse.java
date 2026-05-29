package lt.satsyuk.dto;

import lt.satsyuk.service.IssuedApiKey;

public record IssuedApiKeyResponse(
        Long id,
        String apiKey,
        String prefixHint) {

    public static IssuedApiKeyResponse from(IssuedApiKey issued) {
        return new IssuedApiKeyResponse(issued.id(), issued.rawKey(), issued.prefixHint());
    }
}
