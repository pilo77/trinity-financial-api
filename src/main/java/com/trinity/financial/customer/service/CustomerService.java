package com.trinity.financial.customer.service;

import com.trinity.financial.account.repository.AccountRepository;
import com.trinity.financial.customer.dto.CreateCustomerRequest;
import com.trinity.financial.customer.dto.CustomerMapper;
import com.trinity.financial.customer.dto.CustomerResponse;
import com.trinity.financial.customer.dto.UpdateCustomerRequest;
import com.trinity.financial.customer.entity.CustomerEntity;
import com.trinity.financial.customer.repository.CustomerRepository;
import com.trinity.financial.shared.exception.BusinessRuleException;
import com.trinity.financial.shared.exception.InvalidRequestException;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final int LEGAL_AGE = 18;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CustomerMapper customerMapper;
    private final Clock clock;

    public CustomerService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            CustomerMapper customerMapper,
            Clock clock) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.customerMapper = customerMapper;
        this.clock = clock;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String documentNumber = normalize(request.documentNumber());
        String email = normalizeEmail(request.email());
        validateAdult(request.birthDate());
        validateNormalizedNames(request.firstName(), request.lastName());
        validateUniqueForCreate(documentNumber, email);

        LocalDateTime now = LocalDateTime.now(clock);
        CustomerEntity customer = new CustomerEntity(
                UUID.randomUUID(),
                normalize(request.documentType()),
                documentNumber,
                normalize(request.firstName()),
                normalize(request.lastName()),
                email,
                normalizeNullable(request.phone()),
                request.birthDate(),
                now,
                now);

        return customerMapper.toResponse(save(customer));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return customerMapper.toResponse(findEntity(id));
    }

    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest request) {
        CustomerEntity customer = findEntity(id);
        String documentNumber = normalize(request.documentNumber());
        String email = normalizeEmail(request.email());
        validateAdult(request.birthDate());
        validateNormalizedNames(request.firstName(), request.lastName());
        validateUniqueForUpdate(id, documentNumber, email);

        customer.update(
                normalize(request.documentType()),
                documentNumber,
                normalize(request.firstName()),
                normalize(request.lastName()),
                email,
                normalizeNullable(request.phone()),
                request.birthDate(),
                LocalDateTime.now(clock));

        return customerMapper.toResponse(save(customer));
    }

    @Transactional
    public void delete(UUID id) {
        CustomerEntity customer = findEntity(id);
        if (accountRepository.existsByCustomerId(id)) {
            throw customerHasAccounts();
        }
        try {
            customerRepository.delete(customer);
            customerRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw customerHasAccounts();
        }
    }

    private CustomerEntity findEntity(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "No se encontró el cliente solicitado."));
    }

    private void validateAdult(LocalDate birthDate) {
        LocalDate today = LocalDate.now(clock);
        if (birthDate == null || birthDate.isAfter(today)
                || Period.between(birthDate, today).getYears() < LEGAL_AGE) {
            throw new InvalidRequestException(
                    "CUSTOMER_UNDERAGE",
                    "El cliente debe ser mayor de edad.");
        }
    }

    private void validateNormalizedNames(String firstName, String lastName) {
        if (normalize(firstName).length() < 2 || normalize(lastName).length() < 2) {
            throw new InvalidRequestException(
                    "CUSTOMER_INVALID_NAME",
                    "El nombre y el apellido deben tener al menos 2 caracteres.");
        }
    }

    private void validateUniqueForCreate(String documentNumber, String email) {
        if (customerRepository.existsByDocumentNumber(documentNumber)) {
            throw duplicateDocument();
        }
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw duplicateEmail();
        }
    }

    private void validateUniqueForUpdate(UUID id, String documentNumber, String email) {
        if (customerRepository.existsByDocumentNumberAndIdNot(documentNumber, id)) {
            throw duplicateDocument();
        }
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw duplicateEmail();
        }
    }

    private CustomerEntity save(CustomerEntity customer) {
        try {
            return customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException(
                    "CUSTOMER_DUPLICATE_DATA",
                    "Ya existe un cliente con el documento o correo indicado.");
        }
    }

    private BusinessRuleException duplicateDocument() {
        return new BusinessRuleException(
                "CUSTOMER_DOCUMENT_ALREADY_EXISTS",
                "Ya existe un cliente con el número de documento indicado.");
    }

    private BusinessRuleException duplicateEmail() {
        return new BusinessRuleException(
                "CUSTOMER_EMAIL_ALREADY_EXISTS",
                "Ya existe un cliente con el correo indicado.");
    }

    private BusinessRuleException customerHasAccounts() {
        return new BusinessRuleException(
                "CUSTOMER_HAS_ACCOUNTS",
                "El cliente no puede eliminarse porque tiene cuentas vinculadas.");
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return normalize(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
