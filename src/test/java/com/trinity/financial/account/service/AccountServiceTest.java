package com.trinity.financial.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-14T15:00:00Z");
    private static final LocalDateTime FIXED_DATE_TIME =
            LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
    private static final UUID CUSTOMER_ID =
            UUID.fromString("2f9ca56d-fd0e-47a4-b09e-f6b43cb1769b");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("2bd287db-4673-4dc0-81bd-c3f72d68c89f");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        accountService = new AccountService(
                accountRepository,
                customerRepository,
                new AccountMapper(),
                accountNumberGenerator,
                clock);
    }

    @Test
    void createsSavingsAccountWithPrefixZeroBalancesAndDefaultGmfExemption() {
        CustomerEntity customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(accountNumberGenerator.generate(AccountType.SAVINGS)).thenReturn("5300000001");
        when(accountRepository.saveAndFlush(any(AccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.create(
                new CreateAccountRequest(CUSTOMER_ID, AccountType.SAVINGS, null));

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).saveAndFlush(captor.capture());
        AccountEntity saved = captor.getValue();
        assertThat(saved.getAccountNumber()).startsWith("53").hasSize(10);
        assertThat(saved.getAccountNumber()).containsOnlyDigits();
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.availableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.gmfExempt()).isFalse();
        assertThat(response.createdAt()).isEqualTo(FIXED_DATE_TIME);
        assertThat(response.updatedAt()).isEqualTo(FIXED_DATE_TIME);
    }

    @Test
    void createsCheckingAccountWithPrefixAndExplicitGmfExemption() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(accountNumberGenerator.generate(AccountType.CHECKING)).thenReturn("3300000001");
        when(accountRepository.saveAndFlush(any(AccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.create(
                new CreateAccountRequest(CUSTOMER_ID, AccountType.CHECKING, true));

        assertThat(response.accountNumber()).startsWith("33").hasSize(10);
        assertThat(response.accountType()).isEqualTo(AccountType.CHECKING);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.gmfExempt()).isTrue();
    }

    @Test
    void generatorProducesTenNumericDigitsWithExpectedPrefixes() {
        AccountNumberGenerator generator = new AccountNumberGenerator();

        assertThat(generator.generate(AccountType.SAVINGS))
                .startsWith("53")
                .hasSize(10)
                .containsOnlyDigits();
        assertThat(generator.generate(AccountType.CHECKING))
                .startsWith("33")
                .hasSize(10)
                .containsOnlyDigits();
    }

    @Test
    void rejectsCreationForUnknownCustomer() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.create(
                new CreateAccountRequest(CUSTOMER_ID, AccountType.SAVINGS, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró el cliente solicitado.");
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void stopsAfterFiveAccountNumberCollisions() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(accountNumberGenerator.generate(AccountType.SAVINGS)).thenReturn("5300000001");
        when(accountRepository.existsByAccountNumber("5300000001")).thenReturn(true);

        assertThatThrownBy(() -> accountService.create(
                new CreateAccountRequest(CUSTOMER_ID, AccountType.SAVINGS, false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No fue posible generar un número de cuenta único.");
        verify(accountNumberGenerator, times(5)).generate(AccountType.SAVINGS);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentDatabaseCollisionToControlledBusinessError() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(accountNumberGenerator.generate(AccountType.SAVINGS)).thenReturn("5300000001");
        when(accountRepository.saveAndFlush(any(AccountEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> accountService.create(
                new CreateAccountRequest(CUSTOMER_ID, AccountType.SAVINGS, false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No fue posible generar un número de cuenta único.");
    }

    @Test
    void findsAccountById() {
        AccountEntity account = account(AccountStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.findById(ACCOUNT_ID);

        assertThat(response.id()).isEqualTo(ACCOUNT_ID);
        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void findsAccountByNumber() {
        AccountEntity account = account(AccountStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("5300000001")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.findByAccountNumber("5300000001");

        assertThat(response.accountNumber()).isEqualTo("5300000001");
    }

    @Test
    void rejectsInvalidAccountNumberFormat() {
        assertThatThrownBy(() -> accountService.findByAccountNumber("53ABC"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("El número de cuenta debe contener exactamente 10 dígitos.");
        verify(accountRepository, never()).findByAccountNumber(any());
    }

    @Test
    void listsAccountsWithFiltersAndPagination() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by("createdAt").descending());
        AccountEntity account = account(AccountStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        Page<AccountEntity> result = new PageImpl<>(List.of(account), pageable, 6);
        when(accountRepository.findAll(anyAccountSpecification(), eq(pageable))).thenReturn(result);

        Page<AccountResponse> response = accountService.findAll(
                CUSTOMER_ID,
                AccountType.SAVINGS,
                AccountStatus.ACTIVE,
                pageable);

        assertThat(response.getNumber()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(6);
        verify(accountRepository).findAll(anyAccountSpecification(), eq(pageable));
    }

    @Test
    void rejectsUnsupportedSortProperty() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("unknownField"));

        assertThatThrownBy(() -> accountService.findAll(null, null, null, pageable))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("El criterio de ordenamiento de cuentas no es válido.");
        verify(accountRepository, never()).findAll(
                anyAccountSpecification(),
                any(Pageable.class));
    }

    @Test
    void activatesInactiveAccount() {
        AccountEntity account = account(AccountStatus.INACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.saveAndFlush(account)).thenReturn(account);

        AccountResponse response = accountService.updateStatus(
                ACCOUNT_ID,
                new UpdateAccountStatusRequest(AccountStatus.ACTIVE));

        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.updatedAt()).isEqualTo(FIXED_DATE_TIME);
    }

    @Test
    void inactivatesActiveAccount() {
        AccountEntity account = account(AccountStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.saveAndFlush(account)).thenReturn(account);

        AccountResponse response = accountService.updateStatus(
                ACCOUNT_ID,
                new UpdateAccountStatusRequest(AccountStatus.INACTIVE));

        assertThat(response.status()).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void rejectsCancelledStatusThroughStatusOperation() {
        AccountEntity account = account(AccountStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.updateStatus(
                ACCOUNT_ID,
                new UpdateAccountStatusRequest(AccountStatus.CANCELLED)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("El estado solo puede cambiarse a ACTIVE o INACTIVE.");
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTransitionFromCancelledAccount() {
        AccountEntity account = account(AccountStatus.CANCELLED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.updateStatus(
                ACCOUNT_ID,
                new UpdateAccountStatusRequest(AccountStatus.ACTIVE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Una cuenta cancelada no puede cambiar de estado.");
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelsAccountWhenBothBalancesAreZero() {
        AccountEntity account = account(AccountStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.saveAndFlush(account)).thenReturn(account);

        AccountResponse response = accountService.cancel(ACCOUNT_ID);

        assertThat(response.status()).isEqualTo(AccountStatus.CANCELLED);
        assertThat(response.updatedAt()).isEqualTo(FIXED_DATE_TIME);
    }

    @Test
    void rejectsCancellationWhenAccountingBalanceIsPositive() {
        AccountEntity account = account(
                AccountStatus.ACTIVE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.cancel(ACCOUNT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La cuenta solo puede cancelarse cuando ambos saldos sean cero.");
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsCancellationWhenAvailableBalanceIsPositive() {
        AccountEntity account = account(
                AccountStatus.ACTIVE,
                BigDecimal.ZERO,
                new BigDecimal("10.00"));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.cancel(ACCOUNT_ID))
                .isInstanceOf(BusinessRuleException.class);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsRepeatedCancellation() {
        AccountEntity account = account(AccountStatus.CANCELLED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.cancel(ACCOUNT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La cuenta ya se encuentra cancelada.");
    }

    @Test
    void rejectsNegativeSavingsBalanceAtEntityBoundary() {
        assertThatThrownBy(() -> new AccountEntity(
                ACCOUNT_ID,
                "5300000001",
                AccountType.SAVINGS,
                AccountStatus.ACTIVE,
                new BigDecimal("-0.01"),
                BigDecimal.ZERO,
                false,
                customer(),
                FIXED_DATE_TIME,
                FIXED_DATE_TIME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("balance cannot be negative");
    }

    private AccountEntity account(
            AccountStatus status,
            BigDecimal balance,
            BigDecimal availableBalance) {
        return new AccountEntity(
                ACCOUNT_ID,
                "5300000001",
                AccountType.SAVINGS,
                status,
                balance,
                availableBalance,
                false,
                customer(),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    private CustomerEntity customer() {
        return new CustomerEntity(
                CUSTOMER_ID,
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "carlos@example.com",
                "3001234567",
                LocalDate.of(2000, 1, 1),
                FIXED_DATE_TIME,
                FIXED_DATE_TIME);
    }

    private Specification<AccountEntity> anyAccountSpecification() {
        return any();
    }
}
