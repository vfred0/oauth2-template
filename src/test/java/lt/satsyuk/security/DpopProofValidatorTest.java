package lt.satsyuk.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lt.satsyuk.config.DpopProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DpopProofValidatorTest {

    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String URI = "https://localhost:8443/api/clients/1";
    private static final String METHOD = "GET";

    private DpopProofValidator validator;
    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        DpopProperties properties = new DpopProperties();
        properties.setEnabled(true);
        properties.setReplayCacheSize(100);

        validator = new DpopProofValidator(properties);
        rsaKey = generateRsaKey();
    }

    @Test
    void validatesCorrectProof() throws Exception {
        String expectedJkt = rsaKey.toPublicJWK().computeThumbprint().toString();
        String proof = createProof(rsaKey, METHOD, URI, ACCESS_TOKEN, UUID.randomUUID().toString());

        assertThatCode(() -> validator.validate(METHOD, URI, ACCESS_TOKEN, proof, expectedJkt))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAthMismatch() throws Exception {
        String expectedJkt = rsaKey.toPublicJWK().computeThumbprint().toString();
        String proof = createProof(rsaKey, METHOD, URI, "different-token", UUID.randomUUID().toString());

        assertThatThrownBy(() -> validator.validate(METHOD, URI, ACCESS_TOKEN, proof, expectedJkt))
                .isInstanceOf(DpopProofValidationException.class)
                .hasMessageContaining("hash mismatch");
    }

    @Test
    void rejectsReplayByJti() throws Exception {
        String expectedJkt = rsaKey.toPublicJWK().computeThumbprint().toString();
        String jti = UUID.randomUUID().toString();
        String proof = createProof(rsaKey, METHOD, URI, ACCESS_TOKEN, jti);

        validator.validate(METHOD, URI, ACCESS_TOKEN, proof, expectedJkt);

        assertThatThrownBy(() -> validator.validate(METHOD, URI, ACCESS_TOKEN, proof, expectedJkt))
                .isInstanceOf(DpopProofValidationException.class)
                .hasMessageContaining("replay");
    }

    private String createProof(RSAKey key,
                               String method,
                               String uri,
                               String accessToken,
                               String jti) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(jti)
                .issueTime(Date.from(Instant.now()))
                .claim("htm", method)
                .claim("htu", uri)
                .claim("ath", ath(accessToken))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(new JOSEObjectType("dpop+jwt"))
                        .jwk(key.toPublicJWK())
                        .build(),
                claims
        );

        jwt.sign(new RSASSASigner(key.toPrivateKey()));
        return jwt.serialize();
    }

    private static String ath(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return com.nimbusds.jose.util.Base64URL.encode(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.US_ASCII))).toString();
    }

    private static RSAKey generateRsaKey() throws JOSEException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                    .privateKey(keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
        } catch (Exception ex) {
            throw new JOSEException("Failed to generate test RSA key", ex);
        }
    }
}
