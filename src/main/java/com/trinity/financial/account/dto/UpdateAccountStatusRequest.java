package com.trinity.financial.account.dto;

import com.trinity.financial.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "El estado es obligatorio.")
        AccountStatus status) {
}
