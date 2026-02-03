package lt.satsyuk.api.util;

import dasniko.testcontainers.keycloak.KeycloakContainer;

public class TestKeycloakContainer {
    
    private static KeycloakContainer instance;
    
    public static KeycloakContainer getInstance() {
        if (instance == null) {
            try {
                instance = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.0")
                        .withRealmImportFile("keycloak/realm-export.json")
                        .withReuse(true);
                instance.start();
            } catch (Exception e) {
                // If Docker/Testcontainers is not available, avoid throwing during static init.
                // Return null so tests can skip themselves using assumptions.
                instance = null;
            }
        }
        return instance;
    }
}
