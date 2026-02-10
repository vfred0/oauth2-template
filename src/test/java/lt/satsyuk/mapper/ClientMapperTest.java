package lt.satsyuk.mapper;

import lt.satsyuk.dto.ClientResponse;
import lt.satsyuk.dto.CreateClientRequest;
import lt.satsyuk.model.Client;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {

    private final ClientMapper mapper = ClientMapper.INSTANCE;

    @Test
    void toEntityMapsFieldsAndIgnoresId() {
        CreateClientRequest request = new CreateClientRequest(
                "John",
                "Doe",
                "+12345678901"
        );

        Client entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getFirstName()).isEqualTo("John");
        assertThat(entity.getLastName()).isEqualTo("Doe");
        assertThat(entity.getPhone()).isEqualTo("+12345678901");
    }

    @Test
    void toResponseMapsAllFields() {
        Client client = Client.builder()
                .id(42L)
                .firstName("Jane")
                .lastName("Roe")
                .phone("+37060000000")
                .build();

        ClientResponse response = mapper.toResponse(client);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Roe");
        assertThat(response.phone()).isEqualTo("+37060000000");
    }
}

