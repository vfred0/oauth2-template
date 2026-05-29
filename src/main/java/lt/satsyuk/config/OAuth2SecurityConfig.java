package lt.satsyuk.config;

import lt.satsyuk.config.security.JsonAccessDeniedHandler;
import lt.satsyuk.config.security.JsonAuthEntryPoint;
import lt.satsyuk.config.keycloak.KeycloakProperties;
import lt.satsyuk.config.dpop.DpopAuthenticationFilter;
import lt.satsyuk.config.dpop.DpopAwareBearerTokenResolver;
import lt.satsyuk.config.keycloak.KeycloakOpaqueRoleConverter;
import lt.satsyuk.config.keycloak.KeycloakOpaqueTokenIntrospector;
import lt.satsyuk.config.security.RateLimitingFilter;
import lt.satsyuk.config.security.RbacAuthoritiesLoader;
import lt.satsyuk.config.security.TraceIdResponseHeaderFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;

@Configuration
@ConditionalOnProperty(name = "app.security.mode", havingValue = "KEYCLOAK_OPAQUE", matchIfMissing = true)
public class OAuth2SecurityConfig {

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OpaqueTokenIntrospector opaqueTokenIntrospector,
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
                        .opaqueToken(opaque -> opaque.introspector(opaqueTokenIntrospector))
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

    @Bean
    public DpopAwareBearerTokenResolver dpopAwareBearerTokenResolver() {
        return new DpopAwareBearerTokenResolver();
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector(KeycloakProperties props,
                                                           KeycloakOpaqueRoleConverter roleConverter,
                                                           RbacAuthoritiesLoader rbacAuthoritiesLoader) {
        return new KeycloakOpaqueTokenIntrospector(
                props.getIntrospectionUrl(),
                props.getResourceClientId(),
                props.getResourceClientSecret(),
                roleConverter,
                rbacAuthoritiesLoader
        );
    }
}
