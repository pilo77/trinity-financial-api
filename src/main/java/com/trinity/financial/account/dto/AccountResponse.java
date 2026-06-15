package com.trinity.financial.account.dto;

import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        AccountType accountType,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal availableBalance,
        boolean gmfExempt,
        UUID customerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
