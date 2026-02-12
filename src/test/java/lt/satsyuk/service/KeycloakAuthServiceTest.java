package lt.satsyuk.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lt.satsyuk.config.KeycloakProperties;
import lt.satsyuk.dto.LogoutRequest;
import lt.satsyuk.dto.RefreshRequest;
import lt.satsyuk.exception.KeycloakAuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceTest {

    private static final String CLIENT_ID = "client";
    private static final String CLIENT_SECRET = "secret";
    private static final String REFRESH_TOKEN = "refresh";

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
}

