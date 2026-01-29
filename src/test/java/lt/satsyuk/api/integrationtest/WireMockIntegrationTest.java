package lt.satsyuk.api.integrationtest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public abstract class WireMockIntegrationTest extends AbstractIntegrationTest {

    protected static final String REALMS_PROTOCOL_OPENID_CONNECT_TOKEN = "/realms/.*/protocol/openid-connect/token";
    protected static final String REALMS_PROTOCOL_OPENID_CONNECT_REVOKE = "/realms/.*/protocol/openid-connect/revoke";

    protected static WireMockServer wireMockServer;
    protected static final String REALM = "test-realm";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                        .dynamicPort()
                        .extensions(new UserTokenTransformerV2()) // наш трансформер
        );
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) wireMockServer.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        String base = "http://localhost:" + wireMockServer.port();
        r.add("keycloak.auth-server-url", () -> base);
        r.add("keycloak.realm", () -> REALM);
        r.add("keycloak.token-url", () -> base + "/realms/" + REALM + "/protocol/openid-connect/token");
        r.add("keycloak.logout-url", () -> base + "/realms/" + REALM + "/protocol/openid-connect/revoke");
        r.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> base + "/realms/" + REALM);
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();

        String issuer = "http://localhost:" + wireMockServer.port() + "/realms/" + REALM;

        // Discovery
        stubFor(get(urlEqualTo("/realms/" + REALM + "/.well-known/openid-configuration"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "issuer": "%s",
                                  "jwks_uri": "%s/realms/%s/protocol/openid-connect/certs",
                                  "token_endpoint": "%s/realms/%s/protocol/openid-connect/token"
                                }
                                """.formatted(
                                issuer,
                                "http://localhost:" + wireMockServer.port(),
                                REALM,
                                "http://localhost:" + wireMockServer.port(),
                                REALM
                        ))));

        // JWKS
        stubFor(get(urlEqualTo("/realms/" + REALM + "/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(JwtTestUtil.getJwksJson())));

        // Token endpoint — трансформер обработает только если тест НЕ переопределил stubFor(...)
        stubFor(post(urlEqualTo("/realms/" + REALM + "/protocol/openid-connect/token"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withTransformers("user-token-transformer-v2")));

        // Logout
        stubFor(post(urlEqualTo("/realms/" + REALM + "/protocol/openid-connect/revoke"))
                .willReturn(aResponse().withStatus(200)));
    }

    // -------------------------------------------------------------------------
    // TRANSFORMER — НЕ ЛЕЗЕТ В ЧУЖИЕ МОКИ
    // -------------------------------------------------------------------------

    public static class UserTokenTransformerV2 implements ResponseTransformerV2 {

        @Override
        public String getName() {
            return "user-token-transformer-v2";
        }

        @Override
        public boolean applyGlobally() {
            return false; // критично: НЕ глобально!
        }

        @Override
        public Response transform(Response response, ServeEvent event) {
            Request req = event.getRequest();

            if (!req.getUrl().contains("/protocol/openid-connect/token"))
                return response;

            String body = req.getBodyAsString();
            String username = extract(body, "username");
            String password = extract(body, "password");

            String issuer = "http://localhost:" + wireMockServer.port() + "/realms/" + REALM;

            try {
                if ("admin".equals(username) && "admin".equals(password)) {
                    return token(JwtTestUtil.generateToken("admin", "ADMIN", issuer));
                }
                if ("user".equals(username) && "password".equals(password)) {
                    return token(JwtTestUtil.generateToken("user", "USER", issuer));
                }
            } catch (Exception e) {
                return error(500, "token_generation_failed");
            }

            return error(400, "invalid_grant");
        }

        private Response token(String jwt) {
            return Response.response()
                    .status(200)
                    .headers(json())
                    .body("""
                            {
                              "access_token": "%s",
                              "refresh_token": "dummy"
                            }
                            """.formatted(jwt))
                    .build();
        }

        private Response error(int status, String msg) {
            return Response.response()
                    .status(status)
                    .headers(json())
                    .body("""
                            {"error":"%s"}
                            """.formatted(msg))
                    .build();
        }

        private HttpHeaders json() {
            return new HttpHeaders(new HttpHeader("Content-Type", "application/json"));
        }

        private String extract(String body, String key) {
            if (body == null) return null;
            for (String p : body.split("&")) {
                if (p.startsWith(key + "=")) return p.substring((key + "=").length());
            }
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // JWT UTIL
    // -------------------------------------------------------------------------

    public static class JwtTestUtil {

        private static final RSAKey KEY;
        private static final JWSSigner SIGNER;

        static {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048);
                KeyPair kp = gen.generateKeyPair();

                KEY = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) kp.getPublic())
                        .privateKey(kp.getPrivate())
                        .keyID(UUID.randomUUID().toString())
                        .algorithm(JWSAlgorithm.RS256)
                        .build();

                SIGNER = new RSASSASigner(KEY.toPrivateKey());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static String getJwksJson() {
            return """
                    {"keys":[%s]}
                    """.formatted(KEY.toPublicJWK().toJSONString());
        }

        public static String generateToken(String username, String role, String issuer) throws JOSEException {

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issuer(issuer)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .claim("preferred_username", username)
                    .claim("realm_access", Map.of("roles", java.util.List.of(role)))
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(KEY.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims
            );

            jwt.sign(SIGNER);

            return jwt.serialize();
        }
    }
}