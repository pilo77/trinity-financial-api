package com.trinity.financial.customer.repository;

import com.trinity.financial.customer.entity.CustomerEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentNumberAndIdNot(String documentNumber, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
}
