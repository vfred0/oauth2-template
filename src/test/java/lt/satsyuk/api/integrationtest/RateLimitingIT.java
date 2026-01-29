package lt.satsyuk.api.integrationtest;

import lt.satsyuk.MainApplication;
import lt.satsyuk.api.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class RateLimitingIT extends WireMockIntegrationTest {

    // ------------------------------------------------------------
    // LOGIN RATE LIMIT TESTS (5 requests per minute)
    // ------------------------------------------------------------

    @Test
    void login_rate_limit_blocks_after_5_requests() {

        // First 5 requests should succeed
        for (int i = 0; i < 5; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // 6th request should be rate limited (429 Too Many Requests)
        ResponseEntity<ApiResponse<Object>> response = loginRequest(USERNAME, USER_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void login_rate_limit_resets_after_one_minute() {

        // Exhaust the rate limit (5 requests)
        for (int i = 0; i < 5; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // Verify rate limit is active
        ResponseEntity<ApiResponse<Object>> blockedResponse = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(blockedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Wait for rate limit to reset (1 minute + buffer)
        await()
                .atMost(65, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ApiResponse<Object>> response = loginRequest(USERNAME, USER_PASSWORD);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }

    // ------------------------------------------------------------
    // ADMIN RATE LIMIT TESTS (20 requests per second)
    // ------------------------------------------------------------

    @Test
    void admin_rate_limit_blocks_after_20_requests() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        // First 20 requests should succeed
        for (int i = 0; i < 20; i++) {
            ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // 21st request should be rate limited
        ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void admin_rate_limit_resets_after_one_second() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        // Exhaust the rate limit (20 requests)
        for (int i = 0; i < 20; i++) {
            ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // Verify rate limit is active
        ResponseEntity<ApiResponse<Object>> blockedResponse = requestGet(adminUrl, token);
        assertThat(blockedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Wait for rate limit to reset (1 second + buffer)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, token);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }

    @Test
    void admin_rate_limit_allows_exactly_20_requests_per_second() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        int successCount = 0;
        int rateLimitedCount = 0;

        // Make 25 requests
        for (int i = 0; i < 25; i++) {
            ResponseEntity<ApiResponse<Object>> response = requestGet(adminUrl, token);

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                successCount++;
            } else if (response.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                rateLimitedCount++;
            }
        }

        // Should have exactly 20 successful requests and 5 rate limited
        assertThat(successCount).isEqualTo(20);
        assertThat(rateLimitedCount).isEqualTo(5);
    }

    // ------------------------------------------------------------
    // RATE LIMIT ISOLATION TESTS
    // ------------------------------------------------------------

    @Test
    void different_endpoints_have_independent_rate_limits() {
        // Login endpoint limit

        // Exhaust login rate limit
        for (int i = 0; i < 5; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // Verify login is rate limited
        ResponseEntity<ApiResponse<Object>> loginResponse = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Admin endpoint should still work (independent rate limit)
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);
        ResponseEntity<ApiResponse<Object>> adminResponse = requestGet(adminUrl, token);
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
