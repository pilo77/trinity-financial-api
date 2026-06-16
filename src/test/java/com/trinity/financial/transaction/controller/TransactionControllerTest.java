package com.trinity.financial.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.financial.shared.exception.BusinessRuleException;
import com.trinity.financial.shared.exception.GlobalExceptionHandler;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import com.trinity.financial.transaction.dto.DepositRequest;
import com.trinity.financial.transaction.dto.TransactionResponse;
import com.trinity.financial.transaction.dto.TransferRequest;
import com.trinity.financial.transaction.dto.WithdrawalRequest;
import com.trinity.financial.transaction.entity.TransactionStatus;
import com.trinity.financial.transaction.entity.TransactionType;
import com.trinity.financial.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    private static final UUID TRANSACTION_ID =
            UUID.fromString("0bb4a5e9-531f-4b92-a1a5-a6b71de390a8");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransactionService transactionService;

    @Test
    void createsDepositWithdrawalAndTransfer() throws Exception {
        when(transactionService.deposit(any(DepositRequest.class))).thenReturn(response(TransactionType.DEPOSIT));
        when(transactionService.withdraw(any(WithdrawalRequest.class))).thenReturn(response(TransactionType.WITHDRAWAL));
        when(transactionService.transfer(any(TransferRequest.class))).thenReturn(response(TransactionType.TRANSFER));

        mockMvc.perform(post("/api/v1/transactions/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DepositRequest("5300000001", new BigDecimal("10.00"), null))))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "http://localhost/api/v1/transactions/" + TRANSACTION_ID));
        mockMvc.perform(post("/api/v1/transactions/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WithdrawalRequest("5300000001", new BigDecimal("10.00"), null))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/transactions/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferRequest(
                                        "5300000001", "3300000001",
                                        new BigDecimal("10.00"), null))))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsZeroNegativeAndExcessScaleAmountsWithFieldErrors() throws Exception {
        for (String amount : List.of("0", "-1.00", "1.001")) {
            mockMvc.perform(post("/api/v1/transactions/deposits")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"accountNumber":"5300000001","amount":%s}
                                    """.formatted(amount)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists());
        }
    }

    @Test
    void rejectsMalformedAccountNumber() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber":"ABC","amount":1.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsConflictForBusinessRule() throws Exception {
        when(transactionService.withdraw(any(WithdrawalRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "INSUFFICIENT_FUNDS", "La cuenta no tiene fondos suficientes."));

        mockMvc.perform(post("/api/v1/transactions/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber":"5300000001","amount":10.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void getsTransactionById() throws Exception {
        when(transactionService.findById(TRANSACTION_ID)).thenReturn(response(TransactionType.DEPOSIT));

        mockMvc.perform(get("/api/v1/transactions/{id}", TRANSACTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID.toString()));
    }

    @Test
    void listsTransactionsWithPagination() throws Exception {
        when(transactionService.findAll(any()))
                .thenReturn(new PageImpl<>(
                        List.of(response(TransactionType.DEPOSIT)),
                        PageRequest.of(0, 10),
                        1));

        mockMvc.perform(get("/api/v1/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsNotFoundForMissingAccount() throws Exception {
        when(transactionService.deposit(any(DepositRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND", "No se encontró la cuenta solicitada."));

        mockMvc.perform(post("/api/v1/transactions/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber":"5300000001","amount":10.00}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    private TransactionResponse response(TransactionType type) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 17, 0);
        return new TransactionResponse(
                TRANSACTION_ID, type, new BigDecimal("10.00"), null,
                "5300000001", TransactionStatus.SUCCESS, null, now, now, List.of());
    }
}
