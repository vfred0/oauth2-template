package lt.satsyuk.api.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import lt.satsyuk.dto.AppResponse;
import lt.satsyuk.api.util.KeycloakIntegrationTest;
import lt.satsyuk.dto.ClientResponse;
import lt.satsyuk.dto.CreateClientRequest;
import lt.satsyuk.dto.KeycloakTokenResponse;
import lt.satsyuk.dto.RequestAcceptedResponse;
import lt.satsyuk.dto.RequestStatusResponse;
import lt.satsyuk.model.Client;
import lt.satsyuk.model.RequestStatus;
import lt.satsyuk.repository.ClientRepository;
import lt.satsyuk.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    @Autowired
    RequestRepository requestRepository;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        requestRepository.deleteAll();
    }

    @Test
    void create_client_success_and_persistence() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        CreateClientRequest req = new CreateClientRequest(JOHN, DOE, PHONE);

        RequestAcceptedResponse accepted = postAndReturnData(clientUrl, token, req, HttpStatus.ACCEPTED, RequestAcceptedResponse.class);

        assertThat(accepted.requestId()).isNotNull();
        assertThat(accepted.status()).isEqualTo(RequestStatus.CREATED);

        RequestStatusResponse statusResponse = awaitRequestStatus(token, accepted.requestId(), RequestStatus.PROCESSED);
        AppResponse<ClientResponse> finalResponse = readNestedResponse(statusResponse);
        ClientResponse data = objectMapper.convertValue(finalResponse.data(), ClientResponse.class);

        assertThat(data.phone()).isEqualTo(req.phone());
        assertThat(data.id()).isNotNull();
        assertThat(finalResponse.code()).isZero();

        // Verify persisted
        assertThat(repo.existsByPhone(req.phone())).isTrue();
        assertThat(requestRepository.findById(accepted.requestId())).isPresent();

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

        RequestAcceptedResponse accepted = postAndReturnData(clientUrl, token, req, HttpStatus.ACCEPTED, RequestAcceptedResponse.class);
        RequestStatusResponse statusResponse = awaitRequestStatus(token, accepted.requestId(), RequestStatus.PROCESSING_ERROR);
        AppResponse<Object> finalResponse = readNestedResponse(statusResponse);

        assertThat(finalResponse.code()).isEqualTo(AppResponse.ErrorCode.CONFLICT.getCode());
        assertThat(finalResponse.message()).isEqualTo("Client with phone=" + req.phone() + " already exists");
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

        ResponseEntity<AppResponse<Object>> resp = requestGet(clientUrl + "/999999", token);

        assertErrorStatusAndBody(resp, HttpStatus.NOT_FOUND,
                AppResponse.ErrorCode.NOT_FOUND.getCode(),
                "Client with id=999999 not found");
    }

    // Additional negative tests for GET
    @Test
    void get_client_unauthorized() {
        Client saved = repo.save(Client.builder().firstName("Bob").lastName("Brown").phone("+37063333333").build());

        ResponseEntity<AppResponse<Object>> resp = requestGet(clientUrl + "/" + saved.getId());

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                AppResponse.ErrorCode.UNAUTHORIZED.getCode(),
                AppResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void get_client_unauthorized_after_logout() {
        Client saved = repo.save(Client.builder().firstName(ALICE).lastName(SMITH).phone("+37060000000").build());
        KeycloakTokenResponse tokens = loginAndGetData(USERNAME, USER_PASSWORD);
        String accessToken = tokens.getAccessToken();

        ResponseEntity<AppResponse<Object>> resp = requestGet(clientUrl + "/" + saved.getId(), accessToken);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AppResponse<Void>> logoutResponse = logoutRequest(tokens.getRefreshToken());
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AppResponse<Object>> errorResponse = requestGet(clientUrl + "/" + saved.getId(), accessToken);
        assertErrorStatusAndBody(errorResponse, HttpStatus.UNAUTHORIZED,
                AppResponse.ErrorCode.UNAUTHORIZED.getCode(),
                AppResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void get_client_forbidden_when_user_has_no_role() {
        Client saved = repo.save(Client.builder().firstName("Carol").lastName("White").phone("+37064444444").build());

        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<AppResponse<Object>> resp = requestGet(clientUrl + "/" + saved.getId(), token);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                AppResponse.ErrorCode.FORBIDDEN.getCode(),
                AppResponse.ErrorCode.FORBIDDEN.getDescription());
    }

    @Test
    void get_client_invalid_id_returns_bad_request() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<AppResponse<Object>> resp = requestGet(clientUrl + "/invalid-id", token);

        assertErrorStatusAndBody(resp, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.BAD_REQUEST.getCode(),
                "Invalid value: invalid-id");
    }

    // negative tests
    @Test
    void create_client_unauthorized() {
        CreateClientRequest req = new CreateClientRequest("No", "Token", "+37061111111");

        ResponseEntity<AppResponse<Object>> resp = requestPost(clientUrl, null, req);

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                AppResponse.ErrorCode.UNAUTHORIZED.getCode(),
                AppResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void create_client_forbidden_when_user_has_no_role() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);
        CreateClientRequest req = new CreateClientRequest("No", "Role", "+37062222222");

        ResponseEntity<AppResponse<Object>> resp = requestPost(clientUrl, token, null, req);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                AppResponse.ErrorCode.FORBIDDEN.getCode(),
                AppResponse.ErrorCode.FORBIDDEN.getDescription());
    }

    @Test
    void create_client_validation_error() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        // invalid phone and missing firstName
        CreateClientRequest req = new CreateClientRequest("", DOE, "abc");

        ResponseEntity<AppResponse<Object>> response = requestPost(clientUrl, token, null, req);

        Set<String> expected = Set.of(
                "phone: phone must be valid",
                "firstName: firstName is required"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.BAD_REQUEST.getCode(),
                expected);
    }

    @Test
    void create_client_validation_error_russian_locale() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        // invalid phone and missing firstName
        CreateClientRequest req = new CreateClientRequest("", DOE, "abc");

        ResponseEntity<AppResponse<Object>> response = requestPost(clientUrl, token, "ru", req);

        Set<String> expected = Set.of(
                "phone: Неверный формат телефона",
                "firstName: Имя обязательно"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.BAD_REQUEST.getCode(),
                expected);
    }

    @Test
    void get_request_status_not_found_returns_404() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<AppResponse<Object>> resp = requestGet(requestUrl + "/" + UUID.randomUUID(), token);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(AppResponse.ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void get_request_status_unauthorized() {
        ResponseEntity<AppResponse<Object>> resp = requestGet(requestUrl + "/" + UUID.randomUUID());

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                AppResponse.ErrorCode.UNAUTHORIZED.getCode(),
                AppResponse.ErrorCode.UNAUTHORIZED.getDescription());
    }

    @Test
    void get_request_status_forbidden_when_user_has_no_role() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<AppResponse<Object>> resp = requestGet(requestUrl + "/" + UUID.randomUUID(), token);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                AppResponse.ErrorCode.FORBIDDEN.getCode(),
                AppResponse.ErrorCode.FORBIDDEN.getDescription());
    }

    @Test
    void get_request_status_invalid_id_returns_bad_request() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<AppResponse<Object>> resp = requestGet(requestUrl + "/invalid-id", token);

        assertErrorStatusAndBody(resp, HttpStatus.BAD_REQUEST,
                AppResponse.ErrorCode.BAD_REQUEST.getCode(),
                "Invalid value: invalid-id");
    }

    private RequestStatusResponse awaitRequestStatus(String token, UUID requestId, RequestStatus expectedStatus) {
        final RequestStatusResponse[] holder = new RequestStatusResponse[1];

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(250, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    RequestStatusResponse statusResponse = getAndReturnData(requestUrl + "/" + requestId, token, RequestStatusResponse.class);
                    assertThat(statusResponse.status()).isEqualTo(expectedStatus);
                    if (expectedStatus == RequestStatus.PROCESSED || expectedStatus == RequestStatus.PROCESSING_ERROR) {
                        assertThat(statusResponse.response()).isNotNull();
                    }
                    holder[0] = statusResponse;
                });

        return holder[0];
    }

    private <T> AppResponse<T> readNestedResponse(RequestStatusResponse statusResponse) {
        return objectMapper.convertValue(statusResponse.response(), new TypeReference<>() {});
    }
}
