package lt.satsyuk.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakTokenResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSnakeCaseFields() throws Exception {
        String json = """
                {
                  "access_token": "access",
                  "refresh_token": "refresh",
                  "token_type": "Bearer",
                  "expires_in": 3600,
                  "refresh_expires_in": 7200,
                  "scope": "openid"
                }
                """;

        KeycloakTokenResponse response = objectMapper.readValue(json, KeycloakTokenResponse.class);

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getRefreshExpiresIn()).isEqualTo(7200L);
        assertThat(response.getScope()).isEqualTo("openid");
    }

    @Test
    void serializesToSnakeCaseFields() throws Exception {
        KeycloakTokenResponse response = new KeycloakTokenResponse()
                .setAccessToken("access")
                .setRefreshToken("refresh")
                .setTokenType("Bearer")
                .setExpiresIn(3600L)
                .setRefreshExpiresIn(7200L)
                .setScope("openid");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"access_token\":\"access\"")
                .contains("\"refresh_token\":\"refresh\"")
                .contains("\"token_type\":\"Bearer\"")
                .contains("\"expires_in\":3600")
                .contains("\"refresh_expires_in\":7200")
                .contains("\"scope\":\"openid\"");
    }
}

