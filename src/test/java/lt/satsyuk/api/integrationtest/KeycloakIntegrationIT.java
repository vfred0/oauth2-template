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

    // ------------------------------------------------------------
    // PROTECTED RESOURCES TESTS
    // ------------------------------------------------------------

    @Test
    void access_protected_without_token_unauthorized() {
        ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, "");

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiResponse.ErrorCode.UNAUTHORIZED.getCode(),
                ApiResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void user_cannot_access_admin_forbidden() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, token);

        assertErrorStatusAndBody(response, HttpStatus.FORBIDDEN,
                ApiResponse.ErrorCode.FORBIDDEN.getCode(),
                ApiResponse.ErrorCode.FORBIDDEN.getDescription());
    }

    @Test
    void admin_can_access_admin() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<Object> api = response.getBody();
        assertThat(api).isNotNull();
        Object raw = api.getData();
        assertThat(raw).isNotNull();
        String data = (String) raw;

        assertThat(data).isEqualTo("admin endpoint");
    }

    @Test
    void user_can_access_user_resource() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResponse<Object>> response = requestGet(userUrl, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<Object> api = response.getBody();
        assertThat(api).isNotNull();
        Object raw = api.getData();
        assertThat(raw).isNotNull();
        String data = (String) raw;

        assertThat(data).isEqualTo("user endpoint");
    }

    @Test
    void admin_cannot_access_user_resource() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<ApiResponse<Object>> response = requestGet(userUrl, token);

        assertErrorStatusAndBody(response, HttpStatus.FORBIDDEN,
                ApiResponse.ErrorCode.FORBIDDEN.getCode(),
                ApiResponse.ErrorCode.FORBIDDEN.getDescription());
    }
}