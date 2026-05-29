package lt.satsyuk.service;

public record IssuedApiKey(
        Long id,
        String rawKey,
        String prefixHint) {
}
