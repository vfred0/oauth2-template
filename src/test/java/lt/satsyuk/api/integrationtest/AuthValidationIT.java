package lt.satsyuk.api.integrationtest;

import lt.satsyuk.MainApplication;
import lt.satsyuk.dto.AppResponse;
import lt.satsyuk.api.util.AbstractIntegrationTest;
import lt.satsyuk.config.KeycloakProperties;
import lt.satsyuk.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import lt.satsyuk.security.RateLimitingFilter;

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
        ResponseEntity<AppResponse<TokenResponse>> response = loginRequest("", "", "", "");

        Set<String> expected = Set.of(
                "username: Username is required",
                "clientId: ClientId is required",
                "password: Password is required",
                "clientSecret: ClientSecret is required"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.BAD_REQUEST.getCode(), expected);
    }

    @Test
    void refresh_emptyBody_badRequest() {
        ResponseEntity<AppResponse<TokenResponse>> response = refreshRequest("any-token", "", "");

        Set<String> expected = Set.of(
                "clientId: ClientId is required",
                "clientSecret: ClientSecret is required"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.BAD_REQUEST.getCode(), expected);
    }
}
