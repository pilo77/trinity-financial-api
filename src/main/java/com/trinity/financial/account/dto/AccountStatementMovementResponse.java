package com.trinity.financial.account.dto;

import com.trinity.financial.transaction.entity.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountStatementMovementResponse(
        UUID id,
        MovementType movementType,
        BigDecimal amount,
        UUID transactionId,
        LocalDateTime createdAt,
        String description) {
}