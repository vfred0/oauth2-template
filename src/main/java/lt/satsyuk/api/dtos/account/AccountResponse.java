package lt.satsyuk.api.dtos.account;

import java.math.BigDecimal;

public record AccountResponse(
        Long accountId,
        Long clientId,
        BigDecimal balance
) {}
