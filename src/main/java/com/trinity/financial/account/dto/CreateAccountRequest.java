package com.trinity.financial.account.dto;

import com.trinity.financial.account.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull(message = "El cliente es obligatorio.")
        UUID customerId,

        @NotNull(message = "El tipo de cuenta es obligatorio.")
        AccountType accountType,

        Boolean gmfExempt) {

    public boolean resolvedGmfExempt() {
        return Boolean.TRUE.equals(gmfExempt);
    }
}
