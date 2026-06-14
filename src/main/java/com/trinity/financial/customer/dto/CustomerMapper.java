package com.trinity.financial.customer.dto;

import com.trinity.financial.customer.entity.CustomerEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(CustomerEntity customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getDocumentType(),
                customer.getDocumentNumber(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getBirthDate(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
