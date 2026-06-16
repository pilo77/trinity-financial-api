package com.trinity.financial.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateCustomerRequest(
        @NotBlank(message = "El tipo de documento es obligatorio.")
        @Size(max = 20, message = "El tipo de documento no puede superar 20 caracteres.")
        String documentType,

        @NotBlank(message = "El número de documento es obligatorio.")
        @Size(max = 30, message = "El número de documento no puede superar 30 caracteres.")
        String documentNumber,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres.")
        String lastName,

        @NotBlank(message = "El correo es obligatorio.")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Ingrese un correo válido con formato nombre@dominio.com.")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
        String email,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres.")
        String phone,

        @NotNull(message = "La fecha de nacimiento es obligatoria.")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy.")
        LocalDate birthDate) {
}
