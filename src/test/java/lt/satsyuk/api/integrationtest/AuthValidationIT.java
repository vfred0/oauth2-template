package lt.satsyuk.api.integrationtest;

import lt.satsyuk.MainApplication;
import lt.satsyuk.api.dtos.core.ApiResult;
import lt.satsyuk.api.http_errors.ApiErrorType;
import lt.satsyuk.api.util.AbstractIntegrationTest;
import lt.satsyuk.config.keycloak.KeycloakProperties;
import lt.satsyuk.api.dtos.auth.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import lt.satsyuk.config.security.RateLimitingFilter;

import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainApplication.class)
class AuthValidationIT extends AbstractIntegrationTest {

    AuthValidationIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                     CacheManager cacheManager,
                     RateLimitingFilter rateLimitingFilter) {
        super(props, cacheManager, rateLimitingFilter);
    }

    @Test
    void login_emptyFields_badRequest() {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest("", "", "", "");

        Set<String> expected = Set.of(
                "username: Username is required",
                "clientId: ClientId is required",
                "password: Password is required",
                "clientSecret: ClientSecret is required"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.BAD_REQUEST.code(), expected);
    }

    @Test
    void refresh_emptyBody_badRequest() {
        ResponseEntity<ApiResult<TokenResponse>> response = refreshRequest("any-token", "", "");

        Set<String> expected = Set.of(
                "clientId: ClientId is required",
                "clientSecret: ClientSecret is required"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.BAD_REQUEST.code(), expected);
    }
}
