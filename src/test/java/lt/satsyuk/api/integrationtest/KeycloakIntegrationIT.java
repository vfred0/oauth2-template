package lt.satsyuk.api.integrationtest;

import lt.satsyuk.MainApplication;
import lt.satsyuk.dto.ApiResponse;
import lt.satsyuk.api.util.KeycloakIntegrationTest;
import lt.satsyuk.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class KeycloakIntegrationIT extends KeycloakIntegrationTest {

    // ------------------------------------------------------------
    // LOGIN TESTS
    // ------------------------------------------------------------

    @Test
    void login_success() {
        KeycloakTokenResponse data = loginAndGetData(USERNAME, USER_PASSWORD);
        assertThat(data.getAccessToken()).as("Access token should not be blank").isNotBlank();
        assertThat(data.getRefreshToken()).as("Refresh token should not be blank").isNotBlank();
    }

    @Test
    void login_wrong_password() {
        ResponseEntity<ApiResponse<KeycloakTokenResponse>> response = loginRequest(
                USERNAME,
                "wrongpassword"
        );

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiResponse.ErrorCode.UNAUTHORIZED.getCode(),
                INVALID_GRANT);
    }

    @Test
    void login_unknown_user() {
        ResponseEntity<ApiResponse<KeycloakTokenResponse>> response = loginRequest(
                "unknownuser",
                "whatever"
        );

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiResponse.ErrorCode.UNAUTHORIZED.getCode(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // ADMIN LOGIN TEST
    // ------------------------------------------------------------

    @Test
    void admin_login_success() {
        KeycloakTokenResponse data = loginAndGetData(ADMIN, ADMIN_PASSWORD);
        assertThat(data.getAccessToken()).as("Access token should not be blank").isNotBlank();
        assertThat(data.getRefreshToken()).as("Refresh token should not be blank").isNotBlank();
    }

    // ------------------------------------------------------------
    // REFRESH TOKEN TESTS
    // ------------------------------------------------------------

    @Test
    void refresh_success() {
        String refreshToken = loginAndGetRefresh(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResponse<KeycloakTokenResponse>> refreshResponse = refreshRequest(refreshToken);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<KeycloakTokenResponse> refreshApi = refreshResponse.getBody();
        assertThat(refreshApi).isNotNull();
        KeycloakTokenResponse refreshData = refreshApi.getData();
        assertThat(refreshData).isNotNull();
        assertThat(refreshData.getAccessToken()).as("Access token should not be blank").isNotBlank();
        assertThat(refreshData.getRefreshToken()).as("Refresh token should not be blank").isNotBlank();
    }

    @Test
    void refresh_wrong_token() {
        ResponseEntity<ApiResponse<KeycloakTokenResponse>> refreshResponse = refreshRequest("invalid-token");

        assertErrorStatusAndBody(refreshResponse, HttpStatus.BAD_REQUEST,
                ApiResponse.ErrorCode.INVALID_GRANT.getCode(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // LOGOUT TESTS
    // ------------------------------------------------------------

    @Test
    void logout_success() {
        String refreshToken = loginAndGetRefresh(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResponse<Void>> logoutResponse = logoutRequest(refreshToken);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_wrong_token() {
        ResponseEntity<ApiResponse<Void>> response = logoutRequest("invalid-token");

        assertErrorStatusAndBody(response, HttpStatus.OK,
                ApiResponse.ErrorCode.INVALID_TOKEN.getCode(),
                INVALID_TOKEN);
    }
}