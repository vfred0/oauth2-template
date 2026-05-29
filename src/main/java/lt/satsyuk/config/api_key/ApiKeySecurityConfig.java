package lt.satsyuk.config.api_key;

import lt.satsyuk.config.security.JsonAccessDeniedHandler;
import lt.satsyuk.config.security.JsonAuthEntryPoint;
import lt.satsyuk.config.security.RateLimitingFilter;
import lt.satsyuk.config.security.TraceIdResponseHeaderFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;

@Configuration
@ConditionalOnProperty(name = "app.security.mode", havingValue = "API_KEY")
public class ApiKeySecurityConfig {

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                                   JsonAuthEntryPoint jsonAuthEntryPoint,
                                                   JsonAccessDeniedHandler jsonAccessDeniedHandler,
                                                   TraceIdResponseHeaderFilter traceIdResponseHeaderFilter,
                                                   RateLimitingFilter rateLimitingFilter) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                );

        http.addFilterBefore(traceIdResponseHeaderFilter, AuthenticationFilter.class);
        http.addFilterAfter(apiKeyAuthenticationFilter, AuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, ApiKeyAuthenticationFilter.class);

        return http.build();
    }
}
