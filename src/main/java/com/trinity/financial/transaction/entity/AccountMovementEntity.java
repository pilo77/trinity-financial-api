package com.trinity.financial.transaction.entity;

import com.trinity.financial.account.entity.AccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_movements")
public class AccountMovementEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_transaction_id", nullable = false)
    private FinancialTransactionEntity financialTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType movementType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after_movement", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfterMovement;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AccountMovementEntity() {
    }

    public AccountMovementEntity(
            UUID id,
            AccountEntity account,
            FinancialTransactionEntity financialTransaction,
            MovementType movementType,
            BigDecimal amount,
            BigDecimal balanceAfterMovement,
            LocalDateTime createdAt) {
        this.id = id;
        this.account = account;
        this.financialTransaction = financialTransaction;
        this.movementType = movementType;
        this.amount = amount;
        this.balanceAfterMovement = balanceAfterMovement;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public AccountEntity getAccount() { return account; }
    public FinancialTransactionEntity getFinancialTransaction() { return financialTransaction; }
    public MovementType getMovementType() { return movementType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfterMovement() { return balanceAfterMovement; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
