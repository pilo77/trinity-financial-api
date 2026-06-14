package com.trinity.financial.transaction.dto;

import com.trinity.financial.transaction.entity.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountMovementResponse(
        UUID id,
        String accountNumber,
        MovementType movementType,
        BigDecimal amount,
        BigDecimal balanceAfterMovement,
        LocalDateTime createdAt) {
}
