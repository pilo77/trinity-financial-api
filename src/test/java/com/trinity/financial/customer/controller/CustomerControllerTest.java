package com.trinity.financial.customer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.financial.customer.dto.CreateCustomerRequest;
import com.trinity.financial.customer.dto.CustomerResponse;
import com.trinity.financial.customer.dto.UpdateCustomerRequest;
import com.trinity.financial.customer.service.CustomerService;
import com.trinity.financial.shared.exception.GlobalExceptionHandler;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("2f9ca56d-fd0e-47a4-b09e-f6b43cb1769b");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void createsCustomerAndReturnsLocation() throws Exception {
        CreateCustomerRequest request = validCreateRequest();
        when(customerService.create(any(CreateCustomerRequest.class))).thenReturn(customerResponse());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/v1/customers/" + CUSTOMER_ID))
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.email").value("carlos@example.com"));
    }

    @Test
    void rejectsInvalidCustomerRequestWithFieldErrors() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CC",
                "123456",
                "C",
                "",
                "invalid-email",
                null,
                LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void rejectsEmailsWithoutDomainDot() throws Exception {
        for (String email : List.of("major@gmailcom", "jskdjflks", "plain-text")) {
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "CC",
                    UUID.randomUUID().toString(),
                    "Carlos",
                    "Villamil",
                    email,
                    null,
                    LocalDate.of(2000, 1, 1));

            mockMvc.perform(post("/api/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
        }
    }

    @Test
    void listsCustomers() throws Exception {
        when(customerService.findAll(any())).thenReturn(new PageImpl<>(List.of(customerResponse())));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CUSTOMER_ID.toString()));
    }

    @Test
    void getsCustomerById() throws Exception {
        when(customerService.findById(CUSTOMER_ID)).thenReturn(customerResponse());

        mockMvc.perform(get("/api/v1/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("123456"));
    }

    @Test
    void returnsNotFoundProblemWhenCustomerDoesNotExist() throws Exception {
        when(customerService.findById(CUSTOMER_ID)).thenThrow(new ResourceNotFoundException(
                "CUSTOMER_NOT_FOUND",
                "No se encontró el cliente solicitado."));

        mockMvc.perform(get("/api/v1/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No se encontró el cliente solicitado."));
    }

    @Test
    void updatesCustomer() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "CE",
                "654321",
                "Andrea",
                "Ramirez",
                "andrea@example.com",
                null,
                LocalDate.of(1995, 5, 10));
        CustomerResponse updated = new CustomerResponse(
                CUSTOMER_ID,
                "CE",
                "654321",
                "Andrea",
                "Ramirez",
                "andrea@example.com",
                null,
                request.birthDate(),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 6, 14, 15, 0));
        when(customerService.update(eq(CUSTOMER_ID), any(UpdateCustomerRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/customers/{id}", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Andrea"))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-14T15:00:00"));
    }

    @Test
    void deletesCustomer() throws Exception {
        doNothing().when(customerService).delete(CUSTOMER_ID);

        mockMvc.perform(delete("/api/v1/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsMalformedCustomerId() throws Exception {
        mockMvc.perform(get("/api/v1/customers/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    private CreateCustomerRequest validCreateRequest() {
        return new CreateCustomerRequest(
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "carlos@example.com",
                "3001234567",
                LocalDate.of(2000, 1, 1));
    }

    private CustomerResponse customerResponse() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 14, 15, 0);
        return new CustomerResponse(
                CUSTOMER_ID,
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "carlos@example.com",
                "3001234567",
                LocalDate.of(2000, 1, 1),
                timestamp,
                timestamp);
    }
}
