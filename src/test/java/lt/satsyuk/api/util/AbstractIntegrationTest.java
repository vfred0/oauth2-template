package lt.satsyuk.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.satsyuk.dto.ApiResponse;
import lt.satsyuk.config.KeycloakProperties;
import lt.satsyuk.dto.KeycloakTokenResponse;
import lt.satsyuk.dto.LoginRequest;
import lt.satsyuk.dto.LogoutRequest;
import lt.satsyuk.dto.RefreshRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    // Testcontainers-managed shared containers for integration tests
    private static final String DB_NAME = "appdb";
    private static final String DB_USER = "app";
    private static final String DB_PASS = "app";
    public static final String RESPONSE_MESSAGE_SHOULD_MATCH_EXPECTED = "Response message should match expected";
    public static final String RESPONSE_BODY_SHOULD_NOT_BE_NULL = "Response body should not be null";
    public static final String RESPONSE_DATA_SHOULD_NOT_BE_NULL = "Response data should not be null";
    public static final String RESPONSE_HTTP_STATUS_SHOULD_BE_OK = "Response HTTP status should be OK";
    public static final String RESPONSE_CODE_SHOULD_BE_ZERO = "Response code should be zero";
    public static final String RESPONSE_HTTP_STATUS_SHOULD_MATCH_EXPECTED = "Response HTTP status should match expected";
    public static final String RESPONSE_CODE_SHOULD_MATCH_EXPECTED = "Response code should match expected";
    private static final String RESPONSE_MESSAGE_SHOULD_NOT_BE_NULL = "Response message should not be null";
    public static final String FILTER_CONFIG_CACHE = "filterConfigCache";

    @Container
    protected static PostgreSQLContainer pg = new PostgreSQLContainer("postgres:16")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASS)
            .withReuse(true);

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        if (!pg.isRunning()) {
            pg.start();
        }
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
    }

    @BeforeAll
    static void checkDocker() {
        assumeTrue(pg != null && pg.isRunning(), "Postgres container must be running");
    }

    @Autowired
    protected KeycloakProperties props;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected CacheManager cacheManager;

    // ObjectMapper to convert raw map data into POJOs in tests
    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String USERNAME = "user";
    protected static final String USER_PASSWORD = "password";
    protected static final String ADMIN = "admin";
    protected static final String ADMIN_PASSWORD = "admin";
    protected static final String INVALID_GRANT = "invalid_grant";
    protected static final String INVALID_CLIENT = "invalid_client";

    @LocalServerPort
    protected int port;

    protected String mainUrl;

    protected String loginUrl;
    protected String refreshUrl;
    protected String logoutUrl;

    protected String clientUrl;

    @BeforeEach
    void initializeUrls() {
        mainUrl = "http://localhost:" + port + "/api";
        loginUrl = mainUrl + "/auth/login";
        refreshUrl = mainUrl + "/auth/refresh";
        logoutUrl = mainUrl + "/auth/logout";

        clientUrl = mainUrl + "/clients";

        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                if (FILTER_CONFIG_CACHE.equals(cacheName)) {
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

    protected <T> void assertErrorBody(ResponseEntity<ApiResponse<T>> response, int expectedCode, Object expectedMessage) {
        ApiResponse<T> body = response.getBody();
        assertThat(body).as(RESPONSE_BODY_SHOULD_NOT_BE_NULL).isNotNull();
        assertThat(body.code()).as(RESPONSE_CODE_SHOULD_MATCH_EXPECTED).isEqualTo(expectedCode);
        if (expectedMessage instanceof String) {
            assertThat(body.message()).as(RESPONSE_MESSAGE_SHOULD_MATCH_EXPECTED).isEqualTo(expectedMessage);
        } else if (expectedMessage instanceof Set<?>) {
            String msg = body.message();
            assertThat(msg).as(RESPONSE_MESSAGE_SHOULD_NOT_BE_NULL).isNotNull();
            Set<String> actual = Arrays.stream(msg.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            assertThat(actual).as(RESPONSE_MESSAGE_SHOULD_MATCH_EXPECTED).isEqualTo(expectedMessage);
        }
    }

    protected <T> void assertErrorStatusAndBody(ResponseEntity<ApiResponse<T>> response,
                                            HttpStatus expectedStatus,
                                            int expectedCode,
                                            Object expectedMessage) {
        assertThat(response.getStatusCode()).as(RESPONSE_HTTP_STATUS_SHOULD_MATCH_EXPECTED).isEqualTo(expectedStatus);
        assertErrorBody(response, expectedCode, expectedMessage);
    }

    // Make this method fully generic and return T to avoid unchecked casts in tests
    protected <T> T assertStatusAndBodyAndReturnBody(ResponseEntity<ApiResponse<T>> response, Class<T> clazz) {
        assertThat(response.getStatusCode()).as(RESPONSE_HTTP_STATUS_SHOULD_BE_OK).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_SHOULD_NOT_BE_NULL).isNotNull();

        ApiResponse<T> api = response.getBody();
        assertThat(api).as(RESPONSE_BODY_SHOULD_NOT_BE_NULL).isNotNull();
        assertThat(api.code()).as(RESPONSE_CODE_SHOULD_BE_ZERO).isZero();

        Object raw = api.data();
        assertThat(raw).as(RESPONSE_DATA_SHOULD_NOT_BE_NULL).isNotNull();

        T data = objectMapper.convertValue(raw, clazz);
        assertThat(data).as(RESPONSE_DATA_SHOULD_NOT_BE_NULL).isNotNull();

        return data;
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isEmpty()) {
            headers.setBearerAuth(token);
        }

        return headers;
    }

    protected <T> ResponseEntity<ApiResponse<T>> requestPost(String url, String token, Object body, ParameterizedTypeReference<ApiResponse<T>> responseType) {

        HttpHeaders headers = createHeaders(token);

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                responseType
        );
    }

    protected ResponseEntity<ApiResponse<Object>> requestPost(String url, String token, Record body) {
        return requestPost(url, token, body, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResponse<Object>> requestPost(String url, Record body) {
        return requestPost(url, null, body);
    }

    protected <T> ResponseEntity<ApiResponse<T>> requestGet(String url, String token, ParameterizedTypeReference<ApiResponse<T>> responseType) {

        HttpHeaders headers = createHeaders(token);

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType
        );
    }

    protected ResponseEntity<ApiResponse<Object>> requestGet(String url, String token) {
        return requestGet(url, token, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResponse<Object>> requestGet(String url) {
        return requestGet(url, null);
    }

    protected ResponseEntity<ApiResponse<KeycloakTokenResponse>> loginRequest(String username,
                                                               String password,
                                                               String clientId,
                                                               String clientSecret) {

        LoginRequest request = new LoginRequest(
                username,
                password,
                clientId,
                clientSecret
        );
        return requestPost(loginUrl, null, request, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResponse<KeycloakTokenResponse>> loginRequest(String username,
                                                               String password) {

        return loginRequest(username, password, props.getClientId(), props.getClientSecret());
    }

    protected KeycloakTokenResponse loginAndGetData(String username, String password) {
        ResponseEntity<ApiResponse<KeycloakTokenResponse>> response = loginRequest(username, password);

        return assertStatusAndBodyAndReturnBody(response, KeycloakTokenResponse.class);
    }

    protected String loginAndGetAccess(String username, String password) {
        return loginAndGetData(username, password).getAccessToken();
    }

    protected String loginAndGetRefresh(String username, String password) {
        return loginAndGetData(username, password).getRefreshToken();
    }

    protected ResponseEntity<ApiResponse<Void>> logoutRequest(String refreshToken,
                                                                String clientId,
                                                                String clientSecret) {
        LogoutRequest logoutRequest = new LogoutRequest(refreshToken, clientId, clientSecret);
        return requestPost(logoutUrl, null, logoutRequest, new ParameterizedTypeReference<ApiResponse<Void>>() {});
    }

    protected ResponseEntity<ApiResponse<Void>> logoutRequest(String refreshToken) {
        return logoutRequest(refreshToken, props.getClientId(), props.getClientSecret());
    }

    protected ResponseEntity<ApiResponse<KeycloakTokenResponse>> refreshRequest(String refreshToken,
                                                                String clientId,
                                                                String clientSecret) {
        RefreshRequest request = new RefreshRequest(refreshToken, clientId, clientSecret);
        return requestPost(refreshUrl, null, request, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResponse<KeycloakTokenResponse>> refreshRequest(String refreshToken) {
        return refreshRequest(refreshToken, props.getClientId(), props.getClientSecret());
    }

    // Helper: do POST and return typed data (convert via ObjectMapper)
    protected <T> T postAndReturnData(String url, String token, Object body, Class<T> clazz) {
        ResponseEntity<ApiResponse<T>> resp = requestPost(url, token, body, new ParameterizedTypeReference<>() {});
        return assertStatusAndBodyAndReturnBody(resp, clazz);
    }

    // Helper: do GET and return typed data (convert via ObjectMapper)
    protected <T> T getAndReturnData(String url, String token, Class<T> clazz) {
        ResponseEntity<ApiResponse<T>> resp = requestGet(url, token, new ParameterizedTypeReference<>() {});
        return assertStatusAndBodyAndReturnBody(resp, clazz);
    }
}