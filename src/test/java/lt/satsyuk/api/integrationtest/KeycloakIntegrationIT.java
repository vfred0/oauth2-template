package lt.satsyuk.api.integrationtest;

import lt.satsyuk.MainApplication;
import lt.satsyuk.dto.AppResponse;
import lt.satsyuk.api.util.KeycloakIntegrationTest;
import lt.satsyuk.config.KeycloakProperties;
import lt.satsyuk.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import lt.satsyuk.security.RateLimitingFilter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class KeycloakIntegrationIT extends KeycloakIntegrationTest {

    KeycloakIntegrationIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                          CacheManager cacheManager,
                          RateLimitingFilter rateLimitingFilter) {
        super(props, cacheManager, rateLimitingFilter);
    }

    // ------------------------------------------------------------
    // LOGIN TESTS
    // ------------------------------------------------------------

    @Test
    void login_success() {
        ResponseEntity<AppResponse<TokenResponse>> response = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TokenResponse data = assertStatusAndBodyAndReturnBody(response, TokenResponse.class);
        assertThat(data.accessToken()).as("Access token should not be blank").isNotBlank();

        String refreshCookie = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst().orElse(null);
        assertThat(refreshCookie).as("Refresh token cookie should be set").isNotBlank();
    }

    @Test
    void login_wrong_password() {
        ResponseEntity<AppResponse<TokenResponse>> response = loginRequest(USERNAME, "wrongpassword");

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                AppResponse.ErrorCode.UNAUTHORIZED.getCode(),
                INVALID_GRANT);
    }

    @Test
    void login_unknown_user() {
        ResponseEntity<AppResponse<TokenResponse>> response = loginRequest("unknownuser", "whatever");

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                AppResponse.ErrorCode.UNAUTHORIZED.getCode(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // ADMIN LOGIN TEST
    // ------------------------------------------------------------

    @Test
    void admin_login_success() {
        ResponseEntity<AppResponse<TokenResponse>> response = loginRequest(ADMIN, ADMIN_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TokenResponse data = assertStatusAndBodyAndReturnBody(response, TokenResponse.class);
        assertThat(data.accessToken()).as("Access token should not be blank").isNotBlank();
    }

    // ------------------------------------------------------------
    // REFRESH TOKEN TESTS
    // ------------------------------------------------------------

    @Test
    void refresh_success() {
        String refreshToken = loginAndGetRefresh(USERNAME, USER_PASSWORD);

        ResponseEntity<AppResponse<TokenResponse>> refreshResponse = refreshRequest(refreshToken);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenResponse refreshData = assertStatusAndBodyAndReturnBody(refreshResponse, TokenResponse.class);
        assertThat(refreshData.accessToken()).as("Access token should not be blank").isNotBlank();

        String newRefreshCookie = refreshResponse.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst().orElse(null);
        assertThat(newRefreshCookie).as("New refresh token cookie should be set").isNotBlank();
    }

    @Test
    void refresh_wrong_token() {
        ResponseEntity<AppResponse<TokenResponse>> refreshResponse = refreshRequest("invalid-token");

        assertErrorStatusAndBody(refreshResponse, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.INVALID_GRANT.getCode(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // LOGOUT TESTS
    // ------------------------------------------------------------

    @Test
    void logout_success() {
        String refreshToken = loginAndGetRefresh(USERNAME, USER_PASSWORD);

        ResponseEntity<AppResponse<Void>> logoutResponse = logoutRequest(refreshToken);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_wrong_token() {
        ResponseEntity<AppResponse<Void>> response = logoutRequest("invalid-token");

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.INVALID_TOKEN.getCode(),
                INVALID_GRANT);
    }
}
