package lt.satsyuk.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({RateLimitProperties.class, DpopProperties.class, SecurityProperties.class, ApiKeyProperties.class})
public class SecurityConfig {
}
