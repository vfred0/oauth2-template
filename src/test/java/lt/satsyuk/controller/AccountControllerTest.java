package lt.satsyuk.controller;

import lt.satsyuk.dto.AccountResponse;
import lt.satsyuk.dto.UpdateBalanceRequest;
import lt.satsyuk.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    private AccountController accountController;

    @BeforeEach
    void setUp() {
        accountController = new AccountController(accountService);
    }

    @Test
    void updateBalancePessimisticReturnsServiceResult() {
        UpdateBalanceRequest request = new UpdateBalanceRequest(1L, new BigDecimal("10.00"));
        AccountResponse response = new AccountResponse(2L, 1L, new BigDecimal("110.00"));
        when(accountService.updateBalancePessimistic(request)).thenReturn(response);

        var apiResponse = accountController.updateBalancePessimistic(request);

        assertThat(apiResponse.code()).isZero();
        assertThat(apiResponse.data()).isEqualTo(response);
        verify(accountService).updateBalancePessimistic(request);
    }

    @Test
    void updateBalanceOptimisticReturnsServiceResult() {
        UpdateBalanceRequest request = new UpdateBalanceRequest(1L, new BigDecimal("10.00"));
        AccountResponse response = new AccountResponse(2L, 1L, new BigDecimal("110.00"));
        when(accountService.updateBalanceOptimistic(request)).thenReturn(response);

        var apiResponse = accountController.updateBalanceOptimistic(request);

        assertThat(apiResponse.code()).isZero();
        assertThat(apiResponse.data()).isEqualTo(response);
        verify(accountService).updateBalanceOptimistic(request);
    }

    @Test
    void getByClientIdReturnsServiceResult() {
        AccountResponse response = new AccountResponse(2L, 1L, new BigDecimal("110.00"));
        when(accountService.getByClientId(1L)).thenReturn(response);

        var apiResponse = accountController.getByClientId(1L);

        assertThat(apiResponse.code()).isZero();
        assertThat(apiResponse.data()).isEqualTo(response);
        verify(accountService).getByClientId(1L);
    }
}
