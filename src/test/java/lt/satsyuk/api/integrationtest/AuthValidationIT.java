package lt.satsyuk.api.integrationtest;

import lt.satsyuk.MainApplication;
import lt.satsyuk.api.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainApplication.class)
class AuthValidationIT extends AbstractIntegrationTest {

    @Test
    void login_emptyFields_badRequest() {
        ResponseEntity<ApiResponse<Object>> response = loginRequest(
                "",
                "",
                "",
                ""
        );

        Set<String> expected = Set.of(
                "username: Username is required",
                "clientId: ClientId is required",
                "password: Password is required",
                "clientSecret: ClientSecret is required"
        );

        assertErrorStatusAndBody(
                response,
                HttpStatus.BAD_REQUEST,
                ApiResponse.ErrorCode.BAD_REQUEST.getCode(),
                expected
        );
    }

    @Test
    void refresh_emptyBody_badRequest() {
        ResponseEntity<ApiResponse<Object>> response = refreshRequest(
                "",
                "",
                ""
        );

        Set<String> expected = Set.of(
                "refreshToken: RefreshToken is required",
                "clientId: ClientId is required",
                "clientSecret: ClientSecret is required"
        );

        assertErrorStatusAndBody(
                response,
                HttpStatus.BAD_REQUEST,
                ApiResponse.ErrorCode.BAD_REQUEST.getCode(),
                expected
        );
    }
}
