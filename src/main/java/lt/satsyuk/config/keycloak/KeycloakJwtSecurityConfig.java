package lt.satsyuk.config.keycloak;

import lt.satsyuk.config.security.JsonAccessDeniedHandler;
import lt.satsyuk.config.security.JsonAuthEntryPoint;
import lt.satsyuk.config.dpop.DpopAuthenticationFilter;
import lt.satsyuk.config.dpop.DpopAwareBearerTokenResolver;
import lt.satsyuk.config.security.RateLimitingFilter;
import lt.satsyuk.config.security.TraceIdResponseHeaderFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;

@Configuration
@ConditionalOnProperty(name = "app.security.mode", havingValue = "KEYCLOAK_JWT")
public class KeycloakJwtSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(KeycloakProperties props) {
        return NimbusJwtDecoder.withJwkSetUri(props.getJwkSetUri()).build();
    }

    @Bean
    public DpopAwareBearerTokenResolver dpopAwareBearerTokenResolver() {
        return new DpopAwareBearerTokenResolver();
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder,
                                                   KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
                                                   DpopAwareBearerTokenResolver dpopAwareBearerTokenResolver,
                                                   JsonAuthEntryPoint jsonAuthEntryPoint,
                                                   JsonAccessDeniedHandler jsonAccessDeniedHandler,
                                                   TraceIdResponseHeaderFilter traceIdResponseHeaderFilter,
                                                   DpopAuthenticationFilter dpopAuthenticationFilter,
                                                   RateLimitingFilter rateLimitingFilter) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(dpopAwareBearerTokenResolver)
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                );

        http.addFilterBefore(traceIdResponseHeaderFilter, AuthenticationFilter.class);
        http.addFilterAfter(dpopAuthenticationFilter, AuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, DpopAuthenticationFilter.class);

        return http.build();
    }
}
