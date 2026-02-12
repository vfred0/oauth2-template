package lt.satsyuk.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakTokenResponseTest {

    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
    public static final String BEARER = "Bearer";
    public static final String OPENID = "openid";
    public static final long EXPIRES_IN = 3600L;
    public static final long REFRESH_EXPIRES_IN = 7200L;
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

        assertThat(response)
                .extracting(KeycloakTokenResponse::getAccessToken,
                        KeycloakTokenResponse::getRefreshToken,
                        KeycloakTokenResponse::getTokenType,
                        KeycloakTokenResponse::getExpiresIn,
                        KeycloakTokenResponse::getRefreshExpiresIn,
                        KeycloakTokenResponse::getScope)
                .containsExactly(ACCESS, REFRESH, BEARER, EXPIRES_IN, REFRESH_EXPIRES_IN, OPENID);
    }

    @Test
    void serializesToSnakeCaseFields() throws Exception {
        KeycloakTokenResponse response = new KeycloakTokenResponse()
                .setAccessToken(ACCESS)
                .setRefreshToken(REFRESH)
                .setTokenType(BEARER)
                .setExpiresIn(EXPIRES_IN)
                .setRefreshExpiresIn(REFRESH_EXPIRES_IN)
                .setScope(OPENID);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"access_token\":\"access\"")
                .contains("\"refresh_token\":\"refresh\"")
                .contains("\"token_type\":\"Bearer\"")
                .contains("\"expires_in\":3600")
                .contains("\"refresh_expires_in\":7200")
                .contains("\"scope\":\"openid\"");
    }

    @Test
    void supportsEqualsHashCodeAndToString() {
        KeycloakTokenResponse first = new KeycloakTokenResponse()
                .setAccessToken(ACCESS)
                .setRefreshToken(REFRESH)
                .setTokenType(BEARER)
                .setExpiresIn(EXPIRES_IN)
                .setRefreshExpiresIn(REFRESH_EXPIRES_IN)
                .setScope(OPENID);
        KeycloakTokenResponse same = new KeycloakTokenResponse()
                .setAccessToken(ACCESS)
                .setRefreshToken(REFRESH)
                .setTokenType(BEARER)
                .setExpiresIn(EXPIRES_IN)
                .setRefreshExpiresIn(REFRESH_EXPIRES_IN)
                .setScope(OPENID);
        KeycloakTokenResponse different = new KeycloakTokenResponse()
                .setAccessToken("different")
                .setRefreshToken(REFRESH)
                .setTokenType(BEARER)
                .setExpiresIn(EXPIRES_IN)
                .setRefreshExpiresIn(REFRESH_EXPIRES_IN)
                .setScope(OPENID);

        assertThat(first)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isNotEqualTo(different)
                .asString().contains("accessToken=access");
    }

    @Test
    void equalsHandlesNullAndDifferentTypes() {
        KeycloakTokenResponse response = new KeycloakTokenResponse()
                .setAccessToken(ACCESS)
                .setRefreshToken(REFRESH);

        assertThat(response)
                .isEqualTo(response)
                .isNotNull();
    }

    @Test
    void equalsAndHashCodeWorkWithNullFields() {
        KeycloakTokenResponse first = new KeycloakTokenResponse()
                .setAccessToken(null)
                .setRefreshToken(REFRESH)
                .setTokenType(null)
                .setExpiresIn(0L)
                .setRefreshExpiresIn(0L)
                .setScope(null);
        KeycloakTokenResponse same = new KeycloakTokenResponse()
                .setAccessToken(null)
                .setRefreshToken(REFRESH)
                .setTokenType(null)
                .setExpiresIn(0L)
                .setRefreshExpiresIn(0L)
                .setScope(null);
        KeycloakTokenResponse different = new KeycloakTokenResponse()
                .setAccessToken(ACCESS)
                .setRefreshToken(REFRESH)
                .setTokenType(null)
                .setExpiresIn(0L)
                .setRefreshExpiresIn(0L)
                .setScope(null);

        assertThat(first)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isNotEqualTo(different)
                .asString().contains("accessToken=null");
    }

    @Test
    void chainedSettersReturnSameInstance() {
        KeycloakTokenResponse response = new KeycloakTokenResponse();

        assertThat(response.setAccessToken(ACCESS)).isSameAs(response);
        assertThat(response.setRefreshToken(REFRESH)).isSameAs(response);
        assertThat(response.setTokenType(BEARER)).isSameAs(response);
        assertThat(response.setExpiresIn(EXPIRES_IN)).isSameAs(response);
        assertThat(response.setRefreshExpiresIn(REFRESH_EXPIRES_IN)).isSameAs(response);
        assertThat(response.setScope(OPENID)).isSameAs(response);
    }
}
