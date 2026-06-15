package com.trinity.financial.account.dto;

import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AccountStatementResponse(
        String accountNumber,
        AccountType accountType,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal availableBalance,
        LocalDateTime statementDate,
        List<AccountStatementMovementResponse> movements,
        PageInfo page) {
}