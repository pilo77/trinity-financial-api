package com.trinity.financial.account.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.financial.account.dto.AccountResponse;
import com.trinity.financial.account.dto.CreateAccountRequest;
import com.trinity.financial.account.dto.UpdateAccountStatusRequest;
import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import com.trinity.financial.account.service.AccountService;
import com.trinity.financial.shared.exception.BusinessRuleException;
import com.trinity.financial.shared.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("2f9ca56d-fd0e-47a4-b09e-f6b43cb1769b");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("2bd287db-4673-4dc0-81bd-c3f72d68c89f");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    void createsAccountAndReturnsLocation() throws Exception {
        CreateAccountRequest request =
                new CreateAccountRequest(CUSTOMER_ID, AccountType.SAVINGS, null);
        when(accountService.create(any(CreateAccountRequest.class))).thenReturn(accountResponse());

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/v1/accounts/" + ACCOUNT_ID))
                .andExpect(jsonPath("$.accountNumber").value("5300000001"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.availableBalance").value(0));
    }

    @Test
    void rejectsInvalidAccountTypeInRequestBody() throws Exception {
        String body = """
                {
                  "customerId": "%s",
                  "accountType": "INVESTMENT"
                }
                """.formatted(CUSTOMER_ID);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"))
                .andExpect(jsonPath("$.detail").value(
                        "El valor del campo 'accountType' no es válido."));
    }

    @Test
    void listsAccountsWithFiltersAndPagination() throws Exception {
        PageRequest pageRequest = PageRequest.of(1, 5);
        when(accountService.findAll(
                eq(CUSTOMER_ID),
                eq(AccountType.SAVINGS),
                eq(AccountStatus.ACTIVE),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(accountResponse()), pageRequest, 6));

        mockMvc.perform(get("/api/v1/accounts")
                        .param("customerId", CUSTOMER_ID.toString())
                        .param("accountType", "SAVINGS")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void rejectsInvalidAccountTypeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/accounts").param("accountType", "INVESTMENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.detail").value(
                        "El parámetro 'accountType' no tiene un valor válido."));
    }

    @Test
    void rejectsInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/accounts").param("status", "BLOCKED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.detail").value(
                        "El parámetro 'status' no tiene un valor válido."));
    }

    @Test
    void getsAccountById() throws Exception {
        when(accountService.findById(ACCOUNT_ID)).thenReturn(accountResponse());

        mockMvc.perform(get("/api/v1/accounts/{id}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()));
    }

    @Test
    void getsAccountByNumber() throws Exception {
        when(accountService.findByAccountNumber("5300000001")).thenReturn(accountResponse());

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}", "5300000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("5300000001"));
    }

    @Test
    void updatesAccountStatus() throws Exception {
        AccountResponse inactive = responseWithStatus(AccountStatus.INACTIVE);
        when(accountService.updateStatus(
                eq(ACCOUNT_ID),
                any(UpdateAccountStatusRequest.class)))
                .thenReturn(inactive);

        mockMvc.perform(patch("/api/v1/accounts/{id}/status", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void rejectsInvalidStatusInRequestBody() throws Exception {
        mockMvc.perform(patch("/api/v1/accounts/{id}/status", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"BLOCKED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"))
                .andExpect(jsonPath("$.detail").value(
                        "El valor del campo 'status' no es válido."));
    }

    @Test
    void cancelsAccount() throws Exception {
        when(accountService.cancel(ACCOUNT_ID))
                .thenReturn(responseWithStatus(AccountStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/accounts/{id}/cancel", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void returnsConflictWhenCancelledAccountCannotTransition() throws Exception {
        when(accountService.updateStatus(
                eq(ACCOUNT_ID),
                any(UpdateAccountStatusRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "ACCOUNT_CANCELLED",
                        "Una cuenta cancelada no puede cambiar de estado."));

        mockMvc.perform(patch("/api/v1/accounts/{id}/status", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_CANCELLED"));
    }

    @Test
    void passesExpectedPagingParametersToService() throws Exception {
        when(accountService.findAll(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/accounts")
                        .param("page", "2")
                        .param("size", "10")
                        .param("sort", "accountNumber,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(accountService)
                .findAll(eq(null), eq(null), eq(null), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        Sort.Order order = captor.getValue().getSort().getOrderFor("accountNumber");
        assertThat(order).isNotNull();
        assertThat(order.isAscending()).isTrue();
    }

    private AccountResponse accountResponse() {
        return responseWithStatus(AccountStatus.ACTIVE);
    }

    private AccountResponse responseWithStatus(AccountStatus status) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 14, 15, 0);
        return new AccountResponse(
                ACCOUNT_ID,
                "5300000001",
                AccountType.SAVINGS,
                status,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                CUSTOMER_ID,
                timestamp,
                timestamp);
    }
}
