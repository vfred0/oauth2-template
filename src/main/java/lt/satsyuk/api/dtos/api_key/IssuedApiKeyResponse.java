package lt.satsyuk.api.dtos.api_key;

import lt.satsyuk.service.core.api_key.IssuedApiKey;

public record IssuedApiKeyResponse(
        Long id,
        String apiKey,
        String prefixHint) {

    public static IssuedApiKeyResponse from(IssuedApiKey issued) {
        return new IssuedApiKeyResponse(issued.id(), issued.rawKey(), issued.prefixHint());
    }
}
