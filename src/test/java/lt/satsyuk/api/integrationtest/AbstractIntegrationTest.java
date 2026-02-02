package lt.satsyuk.api.integrationtest;

import lt.satsyuk.api.dto.ApiResponse;
import lt.satsyuk.auth.KeycloakProperties;
import lt.satsyuk.auth.dto.LoginRequest;
import lt.satsyuk.auth.dto.LogoutRequest;
import lt.satsyuk.auth.dto.RefreshRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected KeycloakProperties props;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected CacheManager cacheManager;

    protected static final String USERNAME = "user";
    protected static final String USER_PASSWORD = "password";
    protected static final String ADMIN = "admin";
    protected static final String ADMIN_PASSWORD = "admin";
    protected static final String INVALID_GRANT = "invalid_grant";
    protected static final String INVALID_CLIENT = "invalid_client";
    protected static final String INVALID_TOKEN = "invalid_token";

    @LocalServerPort
    protected int port;

    protected String mainUrl;

    protected String loginUrl;
    protected String refreshUrl;
    protected String logoutUrl;

    protected String userUrl;
    protected String adminUrl;

    protected String clientUrl;

    @BeforeEach
    void initializeUrls() {
        mainUrl = "http://localhost:" + port + "/api";
        loginUrl = mainUrl + "/auth/login";
        refreshUrl = mainUrl + "/auth/refresh";
        logoutUrl = mainUrl + "/auth/logout";

        userUrl = mainUrl + "/user";
        adminUrl = mainUrl + "/admin";

        clientUrl = mainUrl + "/clients";

        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                if ("filterConfigCache".equals(cacheName)) {
                    continue;
                }

                clearCache(cacheName);
            }
        }
    }

    protected void clearCache(String cacheName) {
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    protected void assertErrorBody(ResponseEntity<ApiResponse<Object>> response, int expectedCode, Object expectedMessage) {
        ApiResponse<Object> body = response.getBody();
        assertThat(body).as("Response body should not be null").isNotNull();
        assertThat(body.getCode()).as("Response code should match expected").isEqualTo(expectedCode);
        if (expectedMessage instanceof String) {
            assertThat(body.getMessage()).as("Response message should match expected").isEqualTo(expectedMessage);
        } else if (expectedMessage instanceof Set<?>) {
            String msg = body.getMessage();
            Set<String> actual = Arrays.stream(msg.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            assertThat(actual).as("Response message should match expected").isEqualTo(expectedMessage);
        }
    }

    protected void assertErrorStatusAndBody(ResponseEntity<ApiResponse<Object>> response,
                                            HttpStatus expectedStatus,
                                            int expectedCode,
                                            Object expectedMessage) {
        assertThat(response.getStatusCode()).as("Response HTTP status should match expected").isEqualTo(expectedStatus);
        assertErrorBody(response, expectedCode, expectedMessage);
    }

    protected <T> ResponseEntity<ApiResponse<T>> requestPost(String url, String token, Object body, ParameterizedTypeReference<ApiResponse<T>> responseType) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isEmpty())
            headers.setBearerAuth(token);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                responseType
        );
    }

    protected ResponseEntity<ApiResponse<Object>> requestPost(String url, String token, Record body) {
        return requestPost(url, token, body, apiResponseType());
    }

    protected ResponseEntity<ApiResponse<Object>> requestPost(String url, Record body) {
        return requestPost(url, null, body);
    }

    protected <T> ResponseEntity<ApiResponse<T>> requestGet(String url, String token, ParameterizedTypeReference<ApiResponse<T>> responseType) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isEmpty())
            headers.setBearerAuth(token);

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType
        );
    }

    protected ResponseEntity<ApiResponse<Object>> requestGet(String url, String token) {
        return requestGet(url, token, apiResponseType());
    }

    protected ResponseEntity<ApiResponse<Object>> requestGet(String url) {
        return requestGet(url, null);
    }

    protected ResponseEntity<ApiResponse<Object>> loginRequest(String username,
                                                               String password,
                                                               String clientId,
                                                               String clientSecret) {

        LoginRequest request = new LoginRequest(
                username,
                password,
                clientId,
                clientSecret
        );
        return requestPost(loginUrl, request);
    }

    protected ResponseEntity<ApiResponse<Object>> loginRequest(String username,
                                                               String password) {

        return loginRequest(username, password, props.getClientId(), props.getClientSecret());
    }

    protected Map<String, Object> loginAndGetData(String username, String password) {
        ResponseEntity<ApiResponse<Object>> response = loginRequest(username, password);
        assertThat(response.getStatusCode()).as("Login request should return HTTP OK").isEqualTo(HttpStatus.OK);

        ApiResponse<Object> body = response.getBody();
        assertThat(body).as("Login response body should not be null").isNotNull();
        Object data = body.getData();
        assertThat(data).as("Login response data should not be null").isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) data;
        return map;
    }

    protected String loginAndGetAccess(String username, String password) {
        return (String) loginAndGetData(username, password).get("access_token");
    }

    protected String loginAndGetRefresh(String username, String password) {
        return (String) loginAndGetData(username, password).get("refresh_token");
    }

    protected ResponseEntity<ApiResponse<Object>> logoutRequest(String refreshToken,
                                                                String clientId,
                                                                String clientSecret) {
        LogoutRequest logoutRequest = new LogoutRequest(refreshToken, clientId, clientSecret);
        return requestPost(logoutUrl, logoutRequest);
    }

    protected ResponseEntity<ApiResponse<Object>> logoutRequest(String refreshToken) {
        return logoutRequest(refreshToken, props.getClientId(), props.getClientSecret());
    }

    protected ResponseEntity<ApiResponse<Object>> refreshRequest(String refreshToken,
                                                                String clientId,
                                                                String clientSecret) {
        RefreshRequest request = new RefreshRequest(refreshToken, clientId, clientSecret);
        return requestPost(refreshUrl, request);
    }

    protected ResponseEntity<ApiResponse<Object>> refreshRequest(String refreshToken) {
        return refreshRequest(refreshToken, props.getClientId(), props.getClientSecret());
    }

    // Helper: create ParameterizedTypeReference<ApiResponse<T>> easily
    protected static <T> ParameterizedTypeReference<ApiResponse<T>> apiResponseType() {
        return new ParameterizedTypeReference<>() {};
    }
}