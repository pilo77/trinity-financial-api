package com.trinity.financial.account.service;

import com.trinity.financial.account.dto.AccountMapper;
import com.trinity.financial.account.dto.AccountResponse;
import com.trinity.financial.account.dto.CreateAccountRequest;
import com.trinity.financial.account.dto.UpdateAccountStatusRequest;
import com.trinity.financial.account.entity.AccountEntity;
import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import com.trinity.financial.account.repository.AccountRepository;
import com.trinity.financial.customer.entity.CustomerEntity;
import com.trinity.financial.customer.repository.CustomerRepository;
import com.trinity.financial.shared.exception.BusinessRuleException;
import com.trinity.financial.shared.exception.InvalidRequestException;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id",
            "accountNumber",
            "accountType",
            "status",
            "balance",
            "availableBalance",
            "gmfExempt",
            "createdAt",
            "updatedAt");

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountMapper accountMapper;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Clock clock;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            AccountMapper accountMapper,
            AccountNumberGenerator accountNumberGenerator,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.accountMapper = accountMapper;
        this.accountNumberGenerator = accountNumberGenerator;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        CustomerEntity customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "No se encontró el cliente solicitado."));

        String accountNumber = generateUniqueAccountNumber(request.accountType());
        LocalDateTime now = LocalDateTime.now(clock);
        AccountEntity account = new AccountEntity(
                UUID.randomUUID(),
                accountNumber,
                request.accountType(),
                AccountStatus.ACTIVE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                request.resolvedGmfExempt(),
                customer,
                now,
                now);

        try {
            return accountMapper.toResponse(accountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            throw accountNumberGenerationFailed();
        }
    }

    private String generateUniqueAccountNumber(AccountType accountType) {
        for (int attempt = 1; attempt <= MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            String accountNumber = accountNumberGenerator.generate(accountType);
            if (accountRepository.existsByAccountNumber(accountNumber)) {
                continue;
            }
            return accountNumber;
        }

        throw accountNumberGenerationFailed();
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(
            UUID customerId,
            AccountType accountType,
            AccountStatus status,
            Pageable pageable) {
        validateSort(pageable);
        Specification<AccountEntity> specification = Specification.allOf(
                customerId == null ? null : customerIdEquals(customerId),
                accountType == null ? null : accountTypeEquals(accountType),
                status == null ? null : statusEquals(status));
        return accountRepository.findAll(specification, pageable).map(accountMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        return accountMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public AccountResponse findByAccountNumber(String accountNumber) {
        validateAccountNumber(accountNumber);
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "No se encontró la cuenta solicitada."));
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse updateStatus(UUID id, UpdateAccountStatusRequest request) {
        AccountEntity account = findEntity(id);
        if (request.status() == AccountStatus.CANCELLED) {
            throw new InvalidRequestException(
                    "ACCOUNT_STATUS_NOT_ALLOWED",
                    "El estado solo puede cambiarse a ACTIVE o INACTIVE.");
        }
        if (account.getStatus() == AccountStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "ACCOUNT_CANCELLED",
                    "Una cuenta cancelada no puede cambiar de estado.");
        }

        account.updateStatus(request.status(), LocalDateTime.now(clock));
        return accountMapper.toResponse(accountRepository.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse cancel(UUID id) {
        AccountEntity account = findEntity(id);
        if (account.getStatus() == AccountStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "ACCOUNT_ALREADY_CANCELLED",
                    "La cuenta ya se encuentra cancelada.");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0
                || account.getAvailableBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException(
                    "ACCOUNT_NON_ZERO_BALANCE",
                    "La cuenta solo puede cancelarse cuando ambos saldos sean cero.");
        }

        account.updateStatus(AccountStatus.CANCELLED, LocalDateTime.now(clock));
        return accountMapper.toResponse(accountRepository.saveAndFlush(account));
    }

    private AccountEntity findEntity(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "No se encontró la cuenta solicitada."));
    }

    private void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || !accountNumber.matches("\\d{10}")) {
            throw new InvalidRequestException(
                    "INVALID_ACCOUNT_NUMBER",
                    "El número de cuenta debe contener exactamente 10 dígitos.");
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalidSort = pageable.getSort().stream()
                .map(order -> order.getProperty())
                .anyMatch(property -> !ALLOWED_SORT_PROPERTIES.contains(property));
        if (invalidSort) {
            throw new InvalidRequestException(
                    "INVALID_ACCOUNT_SORT",
                    "El criterio de ordenamiento de cuentas no es válido.");
        }
    }

    private Specification<AccountEntity> customerIdEquals(UUID customerId) {
        return (root, query, builder) -> builder.equal(root.get("customer").get("id"), customerId);
    }

    private Specification<AccountEntity> accountTypeEquals(AccountType accountType) {
        return (root, query, builder) -> builder.equal(root.get("accountType"), accountType);
    }

    private Specification<AccountEntity> statusEquals(AccountStatus status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private BusinessRuleException accountNumberGenerationFailed() {
        return new BusinessRuleException(
                "ACCOUNT_NUMBER_GENERATION_FAILED",
                "No fue posible generar un número de cuenta único.");
    }
}
