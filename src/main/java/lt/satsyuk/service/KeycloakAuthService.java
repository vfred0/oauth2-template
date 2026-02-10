package lt.satsyuk.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lt.satsyuk.config.KeycloakProperties;
import lt.satsyuk.dto.KeycloakTokenResponse;
import lt.satsyuk.dto.LoginRequest;
import lt.satsyuk.dto.LogoutRequest;
import lt.satsyuk.dto.RefreshRequest;
import lt.satsyuk.exception.KeycloakAuthException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class KeycloakAuthService {

    public static final String RESULT = "result";
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String INVALID_TOKEN = "invalid_token";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String REFRESH_TOKEN = "refresh_token";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SCOPE = "scope";
    private static final String OFFLINE_ACCESS = "offline_access";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String INVALID_CLIENT = "invalid_client";
    private static final String NOT_ALLOWED = "not_allowed";
    private static final String EMPTY_RESPONSE = "Empty response";
    private static final String LOGIN_FAILED = "Login failed";
    private static final String REFRESH_FAILED = "Refresh failed";
    private static final String LOGOUT_FAILED = "Logout failed";
    private final RestTemplate rest;
    private final KeycloakProperties props;

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter refreshSuccessCounter;
    private final Counter refreshFailureCounter;
    private final Counter logoutSuccessCounter;
    private final Counter logoutFailureCounter;

    public KeycloakAuthService(RestTemplate rest, KeycloakProperties props, MeterRegistry registry) {
        this.rest = rest;
        this.props = props;

        this.loginSuccessCounter = Counter.builder("auth.login")
                .tag(RESULT, SUCCESS)
                .description("Successful login attempts")
                .register(registry);

        this.loginFailureCounter = Counter.builder("auth.login")
                .tag(RESULT, FAILURE)
                .description("Failed login attempts")
                .register(registry);

        this.refreshSuccessCounter = Counter.builder("auth.refresh")
                .tag(RESULT, SUCCESS)
                .description("Successful token refresh attempts")
                .register(registry);

        this.refreshFailureCounter = Counter.builder("auth.refresh")
                .tag(RESULT, FAILURE)
                .description("Failed token refresh attempts")
                .register(registry);

        this.logoutSuccessCounter = Counter.builder("auth.logout")
                .tag(RESULT, SUCCESS)
                .description("Successful logout attempts")
                .register(registry);

        this.logoutFailureCounter = Counter.builder("auth.logout")
                .tag(RESULT, FAILURE)
                .description("Failed logout attempts")
                .register(registry);
    }

    // ------------------------------------------------------------
    // LOGIN
    // ------------------------------------------------------------
    public KeycloakTokenResponse login(LoginRequest req) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(CLIENT_ID, req.clientId());
        form.add(CLIENT_SECRET, req.clientSecret());
        form.add(GRANT_TYPE, PASSWORD);

        // Required for offline refresh tokens
        form.add(SCOPE, OFFLINE_ACCESS);

        form.add(USERNAME, req.username());
        form.add(PASSWORD, req.password());

        HttpEntity<MultiValueMap<String, String>> entity = createFormEntity(form);

        log.info("➡️  LOGIN request to Keycloak: user={}, clientId={}, realm={}",
                req.username(), req.clientId(), props.getRealm());

        try {
            ResponseEntity<KeycloakTokenResponse> response =
                    rest.postForEntity(props.getTokenUrl(), entity, KeycloakTokenResponse.class);

            log.info("⬅️  TOKEN response: status={}", response.getStatusCode());

            if (response.getBody() == null) {
                throw new KeycloakAuthException(
                        EMPTY_RESPONSE,
                        HttpStatus.BAD_REQUEST,
                        INVALID_GRANT
                );
            }

            loginSuccessCounter.increment();
            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            log.error("❌ LOGIN error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());

            loginFailureCounter.increment();
            throw new KeycloakAuthException(
                    LOGIN_FAILED,
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    extractErrorMessage(ex.getResponseBodyAsString())
            );
        }
    }

    // ------------------------------------------------------------
    // REFRESH
    // ------------------------------------------------------------
    public KeycloakTokenResponse refresh(RefreshRequest req) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(CLIENT_ID, req.clientId());
        form.add(CLIENT_SECRET, req.clientSecret());
        form.add(GRANT_TYPE, REFRESH_TOKEN);
        form.add(REFRESH_TOKEN, req.refreshToken());

        // Required for offline refresh tokens in Keycloak 26
        form.add(SCOPE, OFFLINE_ACCESS);

        HttpEntity<MultiValueMap<String, String>> entity = createFormEntity(form);

        log.info("➡️  REFRESH request to Keycloak: clientId={}, realm={}",
                req.clientId(), props.getRealm());

        try {
            ResponseEntity<KeycloakTokenResponse> response =
                    rest.postForEntity(props.getTokenUrl(), entity, KeycloakTokenResponse.class);

            log.info("⬅️  REFRESH response: status={}", response.getStatusCode());

            if (response.getBody() == null) {
                throw new KeycloakAuthException(
                        EMPTY_RESPONSE,
                        HttpStatus.BAD_REQUEST,
                        INVALID_GRANT
                );
            }

            refreshSuccessCounter.increment();
            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            log.error("❌ REFRESH error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());

            refreshFailureCounter.increment();
            throw new KeycloakAuthException(
                    REFRESH_FAILED,
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    extractErrorMessage(ex.getResponseBodyAsString())
            );
        }
    }

    // ------------------------------------------------------------
    // LOGOUT
    // ------------------------------------------------------------
    public void logout(LogoutRequest req) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(CLIENT_ID, req.clientId());
        form.add(CLIENT_SECRET, req.clientSecret());
        form.add(REFRESH_TOKEN, req.refreshToken());

        HttpEntity<MultiValueMap<String, String>> entity = createFormEntity(form);

        log.info("➡️  LOGOUT request to Keycloak: clientId={}, realm={}",
                req.clientId(), props.getRealm());

        try {
            ResponseEntity<String> response =
                    rest.postForEntity(props.getLogoutUrl(), entity, String.class);

            log.info("⬅️  LOGOUT response: status={}, body={}",
                    response.getStatusCode(), response.getBody());

            if (response.getBody() != null && response.getBody().contains(INVALID_TOKEN)) {
                logoutFailureCounter.increment();
                throw new KeycloakAuthException(
                        INVALID_TOKEN,
                        HttpStatus.valueOf(response.getStatusCode().value()),
                        INVALID_TOKEN
                );
            }

            logoutSuccessCounter.increment();

        } catch (HttpStatusCodeException ex) {
            log.error("❌ LOGOUT error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());

            logoutFailureCounter.increment();
            throw new KeycloakAuthException(
                    LOGOUT_FAILED,
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    extractErrorMessage(ex.getResponseBodyAsString())
            );
        }
    }

    // ------------------------------------------------------------
    // UNIFIED ERROR PARSER
    // ------------------------------------------------------------
    private String extractErrorMessage(String body) {
        if (body == null) return INVALID_GRANT;
        if (body.contains(INVALID_TOKEN)) return INVALID_TOKEN;
        if (body.contains(INVALID_GRANT)) return INVALID_GRANT;
        if (body.contains(INVALID_CLIENT)) return INVALID_CLIENT;
        if (body.contains(NOT_ALLOWED)) return NOT_ALLOWED;
        return INVALID_GRANT;
    }

    private HttpEntity<MultiValueMap<String, String>> createFormEntity(MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(form, headers);
    }
}