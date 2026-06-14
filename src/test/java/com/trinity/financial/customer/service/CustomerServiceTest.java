package com.trinity.financial.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-14T15:00:00Z");
    private static final LocalDateTime FIXED_DATE_TIME =
            LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        customerService = new CustomerService(
                customerRepository,
                accountRepository,
                new CustomerMapper(),
                clock);
    }

    @Test
    void createsAdultCustomerWithNormalizedDataAndTimestamps() {
        CreateCustomerRequest request = createRequest(
                "  CC ",
                " 123456 ",
                " Carlos ",
                " Villamil ",
                " CARLOS@EXAMPLE.COM ",
                " 3001234567 ",
                LocalDate.of(2000, 1, 1));
        when(customerRepository.saveAndFlush(any(CustomerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.create(request);

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository).saveAndFlush(captor.capture());
        CustomerEntity saved = captor.getValue();
        assertThat(response.id()).isNotNull();
        assertThat(saved.getDocumentType()).isEqualTo("CC");
        assertThat(saved.getDocumentNumber()).isEqualTo("123456");
        assertThat(saved.getFirstName()).isEqualTo("Carlos");
        assertThat(saved.getLastName()).isEqualTo("Villamil");
        assertThat(saved.getEmail()).isEqualTo("carlos@example.com");
        assertThat(saved.getPhone()).isEqualTo("3001234567");
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED_DATE_TIME);
        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_DATE_TIME);
    }

    @Test
    void rejectsUnderageCustomer() {
        CreateCustomerRequest request = createRequest(
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "carlos@example.com",
                null,
                LocalDate.of(2010, 1, 1));

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("El cliente debe ser mayor de edad.");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateDocumentOnCreate() {
        CreateCustomerRequest request = validCreateRequest();
        when(customerRepository.existsByDocumentNumber("123456")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ya existe un cliente con el número de documento indicado.");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEmailOnCreate() {
        CreateCustomerRequest request = validCreateRequest();
        when(customerRepository.existsByEmailIgnoreCase("carlos@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ya existe un cliente con el correo indicado.");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNameThatIsTooShortAfterNormalization() {
        CreateCustomerRequest request = createRequest(
                "CC",
                "123456",
                " C ",
                "Villamil",
                "carlos@example.com",
                null,
                LocalDate.of(2000, 1, 1));

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("El nombre y el apellido deben tener al menos 2 caracteres.");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesCustomerAndPreservesCreationTimestamp() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        CustomerEntity customer = customer(id, createdAt, createdAt);
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "CE",
                "654321",
                "Andrea",
                "Ramirez",
                "ANDREA@EXAMPLE.COM",
                null,
                LocalDate.of(1995, 5, 10));
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.saveAndFlush(customer)).thenReturn(customer);

        CustomerResponse response = customerService.update(id, request);

        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(FIXED_DATE_TIME);
        assertThat(response.email()).isEqualTo("andrea@example.com");
        assertThat(response.documentNumber()).isEqualTo("654321");
        verify(customerRepository).existsByDocumentNumberAndIdNot("654321", id);
        verify(customerRepository).existsByEmailIgnoreCaseAndIdNot("andrea@example.com", id);
    }

    @Test
    void rejectsUpdateThatWouldLeaveCustomerUnderage() {
        UUID id = UUID.randomUUID();
        CustomerEntity customer = customer(id, FIXED_DATE_TIME, FIXED_DATE_TIME);
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "carlos@example.com",
                null,
                LocalDate.of(2012, 1, 1));
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.update(id, request))
                .isInstanceOf(InvalidRequestException.class);
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEmailOnUpdate() {
        UUID id = UUID.randomUUID();
        CustomerEntity customer = customer(id, FIXED_DATE_TIME, FIXED_DATE_TIME);
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "duplicate@example.com",
                null,
                LocalDate.of(2000, 1, 1));
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("duplicate@example.com", id))
                .thenReturn(true);

        assertThatThrownBy(() -> customerService.update(id, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ya existe un cliente con el correo indicado.");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void throwsNotFoundWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró el cliente solicitado.");
    }

    @Test
    void deletesCustomerWithoutLinkedAccounts() {
        UUID id = UUID.randomUUID();
        CustomerEntity customer = customer(id, FIXED_DATE_TIME, FIXED_DATE_TIME);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.delete(id);

        verify(accountRepository).existsByCustomerId(id);
        verify(customerRepository).delete(customer);
        verify(customerRepository).flush();
    }

    @Test
    void rejectsDeletionWhenCustomerHasLinkedAccounts() {
        UUID id = UUID.randomUUID();
        CustomerEntity customer = customer(id, FIXED_DATE_TIME, FIXED_DATE_TIME);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByCustomerId(id)).thenReturn(true);

        assertThatThrownBy(() -> customerService.delete(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El cliente no puede eliminarse porque tiene cuentas vinculadas.");
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void rejectsDeletionWhenAccountIsLinkedConcurrently() {
        UUID id = UUID.randomUUID();
        CustomerEntity customer = customer(id, FIXED_DATE_TIME, FIXED_DATE_TIME);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        doThrow(new DataIntegrityViolationException("foreign key"))
                .when(customerRepository)
                .flush();

        assertThatThrownBy(() -> customerService.delete(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El cliente no puede eliminarse porque tiene cuentas vinculadas.");
        verify(customerRepository).delete(customer);
    }

    private CreateCustomerRequest validCreateRequest() {
        return createRequest(
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "carlos@example.com",
                "3001234567",
                LocalDate.of(2000, 1, 1));
    }

    private CreateCustomerRequest createRequest(
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate birthDate) {
        return new CreateCustomerRequest(
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                phone,
                birthDate);
    }

    private CustomerEntity customer(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new CustomerEntity(
                id,
                "CC",
                "123456",
                "Carlos",
                "Villamil",
                "carlos@example.com",
                "3001234567",
                LocalDate.of(2000, 1, 1),
                createdAt,
                updatedAt);
    }
}
