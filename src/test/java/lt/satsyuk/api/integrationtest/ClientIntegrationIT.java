package lt.satsyuk.api.integrationtest;

import lt.satsyuk.api.dto.ApiResponse;
import lt.satsyuk.dto.ClientResponse;
import lt.satsyuk.dto.CreateClientRequest;
import lt.satsyuk.model.Client;
import lt.satsyuk.repository.ClientRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(
        classes = lt.satsyuk.MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ClientIntegrationIT extends KeycloakIntegrationTest {

    static final Object pg = TestPostgresContainer.getInstance();

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        if (pg != null) {
            try {
                Class<?> cls = pg.getClass();
                Method getJdbc = cls.getMethod("getJdbcUrl");
                Method getUser = cls.getMethod("getUsername");
                Method getPass = cls.getMethod("getPassword");

                registry.add("spring.datasource.url", () -> {
                    try { return (String) getJdbc.invoke(pg); } catch (Exception e) { return null; }
                });
                registry.add("spring.datasource.username", () -> {
                    try { return (String) getUser.invoke(pg); } catch (Exception e) { return null; }
                });
                registry.add("spring.datasource.password", () -> {
                    try { return (String) getPass.invoke(pg); } catch (Exception e) { return null; }
                });
            } catch (Exception e) {
                // ignore — properties won't be registered
            }
        }
    }

    @Autowired
    ClientRepository repo;

    @BeforeAll
    static void checkDocker() {
        try {
            // If Postgres or Keycloak containers are not available, skip tests via assumptions
            boolean ok = (pg == null) || (TestKeycloakContainer.getInstance() == null) || TestKeycloakContainer.getInstance().isRunning();
            assumeTrue(ok, "Docker/Testcontainers not available");
        } catch (Exception e) {
            assumeTrue(false, "Docker is not available");
        }
    }

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    void create_client_success_and_persistence() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        CreateClientRequest req = new CreateClientRequest("John", "Doe", "+37061234567");

        ResponseEntity<ApiResponse<ClientResponse>> resp = requestPost(
                clientUrl,
                token,
                req,
                new ParameterizedTypeReference<ApiResponse<ClientResponse>>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        ApiResponse<ClientResponse> body = resp.getBody();
        assertThat(body.getCode()).isZero();

        ClientResponse data = body.getData();
        assertThat(data).isNotNull();
        assertThat(data.phone()).isEqualTo(req.phone());

        // Verify persisted
        assertThat(repo.existsByPhone(req.phone())).isTrue();
    }

    @Test
    void create_client_duplicate_phone_conflict() {
        // prepare existing
        Client existing = Client.builder().firstName("Jane").lastName("Roe").phone("+37061234567").build();
        repo.save(existing);

        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        CreateClientRequest req = new CreateClientRequest("John", "Doe", "+37061234567");

        ResponseEntity<ApiResponse<Object>> resp = requestPost(clientUrl, token, req);

        assertErrorStatusAndBody(resp, HttpStatus.CONFLICT,
                ApiResponse.ErrorCode.CONFLICT.getCode(),
                "Client with phone=" + req.phone() + " already exists");
    }

    @Test
    void get_client_success() {
        Client saved = repo.save(Client.builder().firstName("Alice").lastName("Smith").phone("+37060000000").build());

        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResponse<ClientResponse>> resp = requestGet(
                clientUrl + "/" + saved.getId(),
                token,
                new ParameterizedTypeReference<ApiResponse<ClientResponse>>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        ApiResponse<ClientResponse> body = resp.getBody();
        assertThat(body.getCode()).isZero();

        ClientResponse data = body.getData();
        assertThat(data).isNotNull();
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
        CreateClientRequest req = new CreateClientRequest("", "Doe", "abc");

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
