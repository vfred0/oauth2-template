package lt.satsyuk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String authServerUrl;
    private String realm;

    private String tokenUrl;
    private String logoutUrl;
    private String introspectionUrl;

    // optional - defaults
    private String clientId;
    private String clientSecret;

    private String resourceClientId;
    private String resourceClientSecret;
}