package lt.satsyuk.api.integrationtest;

import lt.satsyuk.dto.ApiResponse;
import lt.satsyuk.api.util.KeycloakIntegrationTest;
import lt.satsyuk.dto.ClientResponse;
import lt.satsyuk.dto.CreateClientRequest;
import lt.satsyuk.dto.KeycloakTokenResponse;
import lt.satsyuk.model.Client;
import lt.satsyuk.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = lt.satsyuk.MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ClientIntegrationIT extends KeycloakIntegrationTest {

    public static final String JOHN = "John";
    public static final String DOE = "Doe";
    public static final String ALICE = "Alice";
    public static final String SMITH = "Smith";
    public static final String PHONE = "+37061234567";
    @Autowired
    ClientRepository repo;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
    }

    @Test
    void create_client_success_and_persistence() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        CreateClientRequest req = new CreateClientRequest(JOHN, DOE, PHONE);

        ClientResponse data = postAndReturnData(clientUrl, token, req, ClientResponse.class);

        assertThat(data.phone()).isEqualTo(req.phone());
        assertThat(data.id()).isNotNull();

        // Verify persisted
        assertThat(repo.existsByPhone(req.phone())).isTrue();

        // Verify GET by id returns same data
        ClientResponse fetched = getAndReturnData(clientUrl + "/" + data.id(), token, ClientResponse.class);
        assertThat(fetched).isNotNull();
        assertThat(fetched.id()).isEqualTo(data.id());
        assertThat(fetched.phone()).isEqualTo(data.phone());
    }

    @Test
    void create_client_duplicate_phone_conflict() {
        // prepare existing
        Client existing = Client.builder().firstName("Jane").lastName("Roe").phone(PHONE).build();
        repo.save(existing);

        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        CreateClientRequest req = new CreateClientRequest(JOHN, DOE, PHONE);

        ResponseEntity<ApiResponse<Object>> resp = requestPost(clientUrl, token, req);

        assertErrorStatusAndBody(resp, HttpStatus.CONFLICT,
                ApiResponse.ErrorCode.CONFLICT.getCode(),
                "Client with phone=" + req.phone() + " already exists");
    }

    @Test
    void get_client_success() {
        Client saved = repo.save(Client.builder().firstName(ALICE).lastName(SMITH).phone("+37060000000").build());

        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ClientResponse data = getAndReturnData(clientUrl + "/" + saved.getId(), token, ClientResponse.class);

        assertThat(data.id()).isEqualTo(saved.getId());
        assertThat(data.phone()).isEqualTo(saved.getPhone());
    }

    @Test
    void get_client_not_found_returns_404() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResponse<Object>> resp = requestGet(clientUrl + "/999999", token);

        assertErrorStatusAndBody(resp, HttpStatus.NOT_FOUND,
                ApiResponse.ErrorCode.NOT_FOUND.getCode(),
                "Client with id=999999 not found");
    }

    // Additional negative tests for GET
    @Test
    void get_client_unauthorized() {
        Client saved = repo.save(Client.builder().firstName("Bob").lastName("Brown").phone("+37063333333").build());

        ResponseEntity<ApiResponse<Object>> resp = requestGet(clientUrl + "/" + saved.getId());

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                ApiResponse.ErrorCode.UNAUTHORIZED.getCode(),
                ApiResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void get_client_unauthorized_after_logout() {
        Client saved = repo.save(Client.builder().firstName(ALICE).lastName(SMITH).phone("+37060000000").build());
        KeycloakTokenResponse tokens = loginAndGetData(USERNAME, USER_PASSWORD);
        String accessToken = tokens.getAccessToken();

        ResponseEntity<ApiResponse<Object>> resp = requestGet(clientUrl + "/" + saved.getId(), accessToken);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResponse<Void>> logoutResponse = logoutRequest(tokens.getRefreshToken());
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResponse<Object>> errorResponse = requestGet(clientUrl + "/" + saved.getId(), accessToken);
        assertErrorStatusAndBody(errorResponse, HttpStatus.UNAUTHORIZED,
                ApiResponse.ErrorCode.UNAUTHORIZED.getCode(),
                ApiResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void get_client_forbidden_when_user_has_no_role() {
        Client saved = repo.save(Client.builder().firstName("Carol").lastName("White").phone("+37064444444").build());

        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<ApiResponse<Object>> resp = requestGet(clientUrl + "/" + saved.getId(), token);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                ApiResponse.ErrorCode.FORBIDDEN.getCode(),
                ApiResponse.ErrorCode.FORBIDDEN.getDescription());
    }

    @Test
    void get_client_invalid_id_returns_bad_request() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResponse<Object>> resp = requestGet(clientUrl + "/invalid-id", token);

        assertErrorStatusAndBody(resp, HttpStatus.BAD_REQUEST,
                ApiResponse.ErrorCode.BAD_REQUEST.getCode(),
                "id is invalid: invalid-id");
    }

    // negative tests
    @Test
    void create_client_unauthorized() {
        CreateClientRequest req = new CreateClientRequest("No", "Token", "+37061111111");

        ResponseEntity<ApiResponse<Object>> resp = requestPost(clientUrl, req);

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                ApiResponse.ErrorCode.UNAUTHORIZED.getCode(),
                ApiResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void create_client_forbidden_when_user_has_no_role() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);
        CreateClientRequest req = new CreateClientRequest("No", "Role", "+37062222222");

        ResponseEntity<ApiResponse<Object>> resp = requestPost(clientUrl, token, req);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                ApiResponse.ErrorCode.FORBIDDEN.getCode(),
                ApiResponse.ErrorCode.FORBIDDEN.getDescription());
    }

    @Test
    void create_client_validation_error() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        // invalid phone and missing firstName
        CreateClientRequest req = new CreateClientRequest("", DOE, "abc");

        ResponseEntity<ApiResponse<Object>> response = requestPost(clientUrl, token, req);

        Set<String> expected = Set.of(
                "phone: phone must be valid",
                "firstName: firstName is required"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiResponse.ErrorCode.BAD_REQUEST.getCode(),
                expected);
    }
}
