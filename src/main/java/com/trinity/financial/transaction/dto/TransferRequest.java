package com.trinity.financial.transaction.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "La cuenta origen es obligatoria.")
        @Pattern(regexp = "\\d{10}", message = "La cuenta origen debe tener 10 dígitos.")
        String sourceAccountNumber,

        @NotBlank(message = "La cuenta destino es obligatoria.")
        @Pattern(regexp = "\\d{10}", message = "La cuenta destino debe tener 10 dígitos.")
        String destinationAccountNumber,

        @NotNull(message = "El monto es obligatorio.")
        @Positive(message = "El monto debe ser mayor que cero.")
        @Digits(integer = 17, fraction = 2, message = "El monto admite máximo 17 enteros y 2 decimales.")
        BigDecimal amount,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres.")
        String description) {
}
