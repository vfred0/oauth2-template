package lt.satsyuk.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DpopAwareBearerTokenResolverTest {

    private final DpopAwareBearerTokenResolver resolver = new DpopAwareBearerTokenResolver();

    @Test
    void resolvesDpopAuthorizationScheme() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "DPoP token-value");

        String token = resolver.resolve(request);

        assertThat(token).isEqualTo("token-value");
    }

    @Test
    void resolvesBearerAuthorizationScheme() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-value");

        String token = resolver.resolve(request);

        assertThat(token).isEqualTo("token-value");
    }

    @Test
    void returnsNullForMissingAuthorization() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String token = resolver.resolve(request);

        assertThat(token).isNull();
    }
}
