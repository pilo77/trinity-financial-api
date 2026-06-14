package com.trinity.financial.account.entity;

import com.trinity.financial.customer.entity.CustomerEntity;
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
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private UUID id;

    @Column(
            name = "account_number",
            nullable = false,
            unique = true,
            length = 10,
            columnDefinition = "char(10)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "gmf_exempt", nullable = false)
    private boolean gmfExempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AccountEntity() {
    }

    public AccountEntity(
            UUID id,
            String accountNumber,
            AccountType accountType,
            AccountStatus status,
            BigDecimal balance,
            BigDecimal availableBalance,
            boolean gmfExempt,
            CustomerEntity customer,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.accountType = Objects.requireNonNull(accountType);
        this.status = Objects.requireNonNull(status);
        this.balance = requireNonNegative(balance, "balance");
        this.availableBalance = requireNonNegative(availableBalance, "availableBalance");
        this.gmfExempt = gmfExempt;
        this.customer = Objects.requireNonNull(customer);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void updateStatus(AccountStatus status, LocalDateTime updatedAt) {
        this.status = Objects.requireNonNull(status);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void credit(BigDecimal amount, LocalDateTime updatedAt) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void debit(BigDecimal amount, LocalDateTime updatedAt) {
        requirePositive(amount);
        if (this.availableBalance.compareTo(amount) < 0
                || this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    private static void requirePositive(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount is required");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public boolean isGmfExempt() {
        return gmfExempt;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
