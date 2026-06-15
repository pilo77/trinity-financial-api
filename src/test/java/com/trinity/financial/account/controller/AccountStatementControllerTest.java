package com.trinity.financial.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.financial.account.dto.AccountStatementMovementResponse;
import com.trinity.financial.account.dto.AccountStatementResponse;
import com.trinity.financial.account.dto.PageInfo;
import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import com.trinity.financial.account.service.AccountStatementService;
import com.trinity.financial.shared.exception.GlobalExceptionHandler;
import com.trinity.financial.shared.exception.InvalidRequestException;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import com.trinity.financial.transaction.entity.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountStatementController.class)
@Import(GlobalExceptionHandler.class)
class AccountStatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountStatementService accountStatementService;

    @Test
    void returnsStatementWithPaginationAndMovements() throws Exception {
        when(accountStatementService.getStatement(
                eq("5300000001"),
                eq(LocalDateTime.of(2026, 1, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 30, 23, 59, 59)),
                any(Pageable.class)))
                .thenReturn(response());

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}/statement", "5300000001")
                        .param("startDate", "2026-01-01T00:00:00")
                        .param("endDate", "2026-06-30T23:59:59")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("5300000001"))
                .andExpect(jsonPath("$.balance").value(250))
                .andExpect(jsonPath("$.availableBalance").value(250))
                .andExpect(jsonPath("$.movements[0].movementType").value("CREDIT"))
                .andExpect(jsonPath("$.page.page").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void returnsNotFoundWhenAccountDoesNotExist() throws Exception {
        when(accountStatementService.getStatement(any(), any(), any(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND", "No se encontró la cuenta solicitada."));

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}/statement", "5300000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestWhenDateRangeIsInvalid() throws Exception {
        when(accountStatementService.getStatement(any(), any(), any(), any(Pageable.class)))
                .thenThrow(new InvalidRequestException(
                        "INVALID_DATE_RANGE",
                        "La fecha inicial debe ser anterior o igual a la fecha final."));

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}/statement", "5300000001")
                        .param("startDate", "2026-06-30T00:00:00")
                        .param("endDate", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void passesPageableParametersToService() throws Exception {
        when(accountStatementService.getStatement(any(), any(), any(), any(Pageable.class)))
                .thenReturn(response());

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}/statement", "5300000001")
                        .param("page", "2")
                        .param("size", "10")
                        .param("sort", "amount,asc"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(accountStatementService).getStatement(
                eq("5300000001"),
                eq(null),
                eq(null),
                captor.capture());
        Pageable pageable = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(pageable.getPageNumber()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(pageable.getPageSize()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(pageable.getSort().getOrderFor("amount")).isNotNull();
    }

    private AccountStatementResponse response() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 17, 0);
        return new AccountStatementResponse(
                "5300000001",
                AccountType.SAVINGS,
                AccountStatus.ACTIVE,
                new BigDecimal("250.00"),
                new BigDecimal("250.00"),
                now,
                List.of(new AccountStatementMovementResponse(
                        java.util.UUID.fromString("11111111-2222-3333-4444-555555555555"),
                        MovementType.CREDIT,
                        new BigDecimal("50.00"),
                        java.util.UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa"),
                        now,
                        "Payroll deposit")),
                new PageInfo(1, 5, 2, 1, true, true, false, false));
    }
}