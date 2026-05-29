package lt.satsyuk.config.security;

import lt.satsyuk.config.DpopProperties;
import lt.satsyuk.config.RateLimitProperties;
import lt.satsyuk.config.api_key.ApiKeyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({RateLimitProperties.class, DpopProperties.class, SecurityProperties.class, ApiKeyProperties.class})
public class SecurityConfig {
}
