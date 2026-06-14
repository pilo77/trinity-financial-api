package com.trinity.financial.account.repository;

import java.util.UUID;

public interface AccountRepository {

    boolean existsByCustomerId(UUID customerId);
}
