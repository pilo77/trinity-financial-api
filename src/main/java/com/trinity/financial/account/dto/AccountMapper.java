package com.trinity.financial.account.dto;

import com.trinity.financial.account.entity.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(AccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStatus(),
                account.getBalance(),
                account.getAvailableBalance(),
                account.isGmfExempt(),
                account.getCustomer().getId(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
