package lt.satsyuk.api.util;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class KeycloakIntegrationTest extends AbstractIntegrationTest {

    @Container
    protected static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.0")
            .withRealmImportFile("keycloak/realm-export.json")
            .withReuse(true);

    @BeforeAll
    static void checkDockerAvailable() {
        try {
            assumeTrue(keycloak != null && keycloak.isRunning(), "Keycloak container should be running");
        } catch (Exception e) {
            assumeTrue(false, "Docker is not available. Please install Docker Desktop for Windows and ensure it's running.");
        }
    }

    @DynamicPropertySource
    static void registerResourceServerIssuerProperty(DynamicPropertyRegistry registry) {
        if (keycloak != null && keycloak.isRunning()) {
            String authServerUrl = keycloak.getAuthServerUrl();
            String realm = "my-realm";

            registry.add("keycloak.auth-server-url", () -> authServerUrl);
            registry.add("keycloak.realm", () -> realm);
            registry.add("keycloak.token-url", () -> authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token");
            registry.add("keycloak.logout-url", () -> authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout");
            registry.add("keycloak.introspection-url", () -> authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect");
            registry.add("spring.security.oauth2.resourceserver.opaque-token.introspection-uri",
                    () -> authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect");
            registry.add("spring.security.oauth2.resourceserver.opaque-token.client-id",
                    () -> "resource-server");
            registry.add("spring.security.oauth2.resourceserver.opaque-token.client-secret",
                    () -> "resource-server-secret");
            registry.add("keycloak.resource-client-id", () -> "resource-server");
            registry.add("keycloak.resource-client-secret", () -> "resource-server-secret");
        }
    }
}