package lt.satsyuk.api.integrationtest;

import lt.satsyuk.MainApplication;
import lt.satsyuk.api.dto.ApiResponse;
import lt.satsyuk.api.util.WireMockIntegrationTest;
import lt.satsyuk.auth.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class RateLimitingIT extends WireMockIntegrationTest {

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
        ResponseEntity<ApiResponse<KeycloakTokenResponse>> response = loginRequest(USERNAME, USER_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void login_rate_limit_resets_after_20_seconds() {

        // Exhaust the rate limit (5 requests)
        for (int i = 0; i < 5; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // Verify rate limit is active
        ResponseEntity<ApiResponse<KeycloakTokenResponse>> blockedResponse = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(blockedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Wait for rate limit to reset (20 seconds + buffer)
        await()
                .atMost(25, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ApiResponse<KeycloakTokenResponse>> response = loginRequest(USERNAME, USER_PASSWORD);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }

    // ------------------------------------------------------------
    // ADMIN RATE LIMIT TESTS (20 requests per second)
    // ------------------------------------------------------------

    @Test
    void admin_rate_limit_blocks_after_20_requests() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        List<HttpStatusCode> statuses = requestAdminStatuses(token, 21);
        int successCount = 0;
        int rateLimitedCount = 0;

        for (HttpStatusCode status : statuses) {
            if (status.equals(HttpStatus.OK)) {
                successCount++;
            } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                rateLimitedCount++;
            }
        }

        assertThat(successCount).as("Success count should be 20").isEqualTo(20);
        assertThat(rateLimitedCount).as("Rate limited count should be 1").isEqualTo(1);
    }

    @Test
    void admin_rate_limit_resets_after_20_seconds() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        List<HttpStatusCode> statuses = requestAdminStatuses(token, 21);
        int successCount = 0;
        int rateLimitedCount = 0;

        for (HttpStatusCode status : statuses) {
            if (status.equals(HttpStatus.OK)) {
                successCount++;
            } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                rateLimitedCount++;
            }
        }

        assertThat(successCount).as("Success count should be 20").isEqualTo(20);
        assertThat(rateLimitedCount).as("Rate limited count should be 1").isEqualTo(1);

        // Wait for rate limit to reset (20 seconds + buffer)
        await()
                .atMost(25, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
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

        List<HttpStatusCode> statuses = requestAdminStatuses(token, 25);
        for (HttpStatusCode status : statuses) {
            if (status.equals(HttpStatus.OK)) {
                successCount++;
            } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                rateLimitedCount++;
            }
        }

        // Should have exactly 20 successful requests and 5 rate limited
        assertThat(successCount).as("Success count should be 20").isEqualTo(20);
        assertThat(rateLimitedCount).as("Rate limited count should be 5").isEqualTo(5);
    }

    // ------------------------------------------------------------
    // RATE LIMIT ISOLATION TESTS
    // ------------------------------------------------------------

    @Test
    void different_endpoints_have_independent_rate_limits() {
         String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        // Exhaust login rate limit
        for (int i = 0; i < 4; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // Verify login is rate limited
        ResponseEntity<ApiResponse<KeycloakTokenResponse>> loginResponse = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Admin endpoint should still work (independent rate limit)
        ResponseEntity<ApiResponse<Object>> adminResponse = requestGet(adminUrl, token);
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private List<HttpStatusCode> requestAdminStatuses(String token, int count) {
        List<CompletableFuture<HttpStatusCode>> futures = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                ResponseEntity<ApiResponse<Object>> resp = requestGet(adminUrl, token);
                return resp.getStatusCode();
            }));
        }

        List<HttpStatusCode> statuses = new ArrayList<>(count);
        for (CompletableFuture<HttpStatusCode> future : futures) {
            try {
                statuses.add(future.get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute admin requests", e);
            }
        }

        return statuses;
    }
}
