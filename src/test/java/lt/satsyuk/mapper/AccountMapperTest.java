package lt.satsyuk.mapper;

import lt.satsyuk.dto.AccountResponse;
import lt.satsyuk.model.Account;
import lt.satsyuk.model.Client;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private final AccountMapper mapper = AccountMapper.INSTANCE;

    @Test
    void toResponseMapsAccountIdClientIdAndBalance() {
        Client client = Client.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .phone("+37060000001")
                .build();
        Account account = Account.builder()
                .id(20L)
                .client(client)
                .balance(new BigDecimal("123.45"))
                .version(1L)
                .build();

        AccountResponse response = mapper.toResponse(account);

        assertThat(response.accountId()).isEqualTo(20L);
        assertThat(response.clientId()).isEqualTo(10L);
        assertThat(response.balance()).isEqualByComparingTo("123.45");
    }
}
