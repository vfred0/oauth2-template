package lt.satsyuk.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lt.satsyuk.config.KeycloakProperties;
import lt.satsyuk.dto.KeycloakTokenResponse;
import lt.satsyuk.dto.LoginRequest;
import lt.satsyuk.dto.LogoutRequest;
import lt.satsyuk.dto.RefreshRequest;
import lt.satsyuk.exception.KeycloakAuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceTest {

    private static final String CLIENT_ID = "client";
    private static final String CLIENT_SECRET = "secret";
    private static final String REFRESH_TOKEN = "refresh";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    @Mock
    private RestTemplate rest;

    @Mock
    private KeycloakProperties props;

    @Test
    void refreshThrowsWhenResponseBodyMissing() {
        when(props.getTokenUrl()).thenReturn("http://token");
        when(rest.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(lt.satsyuk.dto.KeycloakTokenResponse.class)
        )).thenReturn(ResponseEntity.ok().body(null));

        KeycloakAuthService service = new KeycloakAuthService(rest, props, new SimpleMeterRegistry());
        RefreshRequest request = new RefreshRequest(REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET);

        assertThatThrownBy(() -> service.refresh(request))
                .isInstanceOf(KeycloakAuthException.class)
                .hasMessage("Empty response")
                .satisfies(ex -> {
                    KeycloakAuthException authEx = (KeycloakAuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(authEx.getKeycloakMessage()).isEqualTo("invalid_grant");
                });
    }

    @Test
    void logoutThrowsWhenInvalidTokenReturned() {
        when(props.getLogoutUrl()).thenReturn("http://logout");
        when(rest.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("invalid_token"));

        KeycloakAuthService service = new KeycloakAuthService(rest, props, new SimpleMeterRegistry());
        LogoutRequest request = new LogoutRequest(REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET);

        assertThatThrownBy(() -> service.logout(request))
                .isInstanceOf(KeycloakAuthException.class)
                .hasMessage("invalid_token")
                .satisfies(ex -> {
                    KeycloakAuthException authEx = (KeycloakAuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.OK);
                    assertThat(authEx.getKeycloakMessage()).isEqualTo("invalid_token");
                });
    }

    @Test
    void extractErrorMessageHandlesNullBody() {
        KeycloakAuthService service = new KeycloakAuthService(rest, props, new SimpleMeterRegistry());

        String result = ReflectionTestUtils.invokeMethod(service, "extractErrorMessage", (String) null);

        assertThat(result).isEqualTo("invalid_grant");
    }

    @Test
    void extractErrorMessageHandlesNotAllowed() {
        KeycloakAuthService service = new KeycloakAuthService(rest, props, new SimpleMeterRegistry());

        String result = ReflectionTestUtils.invokeMethod(service, "extractErrorMessage", "not_allowed");

        assertThat(result).isEqualTo("not_allowed");
    }

    @Test
    void loginCountersKeepSuccessAndFailureTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(props.getTokenUrl()).thenReturn("http://token");

        RestTemplate realRestTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(realRestTemplate).build();
        mockServer.expect(requestTo("http://token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"a\",\"refresh_token\":\"r\",\"token_type\":\"Bearer\"}",
                        MediaType.APPLICATION_JSON
                ));
        mockServer.expect(requestTo("http://token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));

        KeycloakAuthService service = new KeycloakAuthService(realRestTemplate, props, registry);
        service.login(new LoginRequest(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET));

        assertThatThrownBy(() -> service.login(new LoginRequest(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET)))
                .isInstanceOf(KeycloakAuthException.class);

        mockServer.verify();

        assertThat(counterCount(registry, "auth.login", "success")).isEqualTo(1.0);
        assertThat(counterCount(registry, "auth.login", "failure")).isEqualTo(1.0);
    }

    @Test
    void refreshCountersKeepSuccessAndFailureTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(props.getTokenUrl()).thenReturn("http://token");

        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse().setAccessToken("a").setRefreshToken("r");
        when(rest.postForEntity(anyString(), any(HttpEntity.class), eq(KeycloakTokenResponse.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        KeycloakAuthService service = new KeycloakAuthService(rest, props, registry);
        service.refresh(new RefreshRequest(REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET));

        doThrow(new org.springframework.web.client.HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "bad request",
                "{\"error\":\"invalid_grant\"}".getBytes(),
                java.nio.charset.StandardCharsets.UTF_8
        )).when(rest).postForEntity(anyString(), any(HttpEntity.class), eq(KeycloakTokenResponse.class));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest(REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET)))
                .isInstanceOf(KeycloakAuthException.class);

        assertThat(counterCount(registry, "auth.refresh", "success")).isEqualTo(1.0);
        assertThat(counterCount(registry, "auth.refresh", "failure")).isEqualTo(1.0);
    }

    @Test
    void logoutCountersKeepSuccessAndFailureTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(props.getLogoutUrl()).thenReturn("http://logout");
        when(rest.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(""));

        KeycloakAuthService service = new KeycloakAuthService(rest, props, registry);
        service.logout(new LogoutRequest(REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET));

        when(rest.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("invalid_token"));

        assertThatThrownBy(() -> service.logout(new LogoutRequest(REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET)))
                .isInstanceOf(KeycloakAuthException.class);

        assertThat(counterCount(registry, "auth.logout", "success")).isEqualTo(1.0);
        assertThat(counterCount(registry, "auth.logout", "failure")).isEqualTo(1.0);
    }

    private double counterCount(SimpleMeterRegistry registry, String name, String resultTag) {
        return registry.get(name)
                .tag("result", resultTag)
                .counter()
                .count();
    }
}

