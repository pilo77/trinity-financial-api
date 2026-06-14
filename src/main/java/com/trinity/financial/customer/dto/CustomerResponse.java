package com.trinity.financial.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
