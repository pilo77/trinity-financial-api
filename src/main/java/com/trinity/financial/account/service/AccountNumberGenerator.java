package com.trinity.financial.account.service;

import com.trinity.financial.account.entity.AccountType;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberGenerator {

    private static final int RANDOM_BOUND = 100_000_000;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(AccountType accountType) {
        int suffix = secureRandom.nextInt(RANDOM_BOUND);
        return accountType.getPrefix() + String.format(Locale.ROOT, "%08d", suffix);
    }
}
