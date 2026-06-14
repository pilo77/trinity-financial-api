package com.trinity.financial.account.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAccountRepository implements AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsByCustomerId(UUID customerId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM accounts WHERE customer_id = ?)",
                Boolean.class,
                customerId);
        return Boolean.TRUE.equals(exists);
    }
}
