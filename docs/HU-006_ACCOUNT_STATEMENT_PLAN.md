# HU-006 - Account Statement Plan

## Estado

PLANNED - READY FOR IMPLEMENTATION

Este documento define la implementación de consultas de estado de cuenta
utilizando el historial de movimientos generado por HU-005 (Financial
Transactions).

## Objetivo

Implementar la consulta de estado de cuenta (account statement) para una cuenta
específica, devolviendo el balance actual, saldos disponibles e historial
detallado de movimientos con soporte para filtrado temporal y paginación.

## Alcance funcional

- Consultar una cuenta por número de cuenta.
- Devolver balance y availableBalance actuales de la cuenta.
- Listar todos los movimientos (`AccountMovement`) asociados a la cuenta.
- Filtrar movimientos por rango de fechas (startDate, endDate).
- Validar que startDate ≤ endDate.
- Soportar paginación con page, size y sort.
- Devolver lista vacía si no hay movimientos.
- Retornar 404 si la cuenta no existe.
- Retornar 400 si el rango de fechas es inválido.

## Fuera de alcance

- Generación de reportes en PDF/Excel.
- Exportación de movimientos.
- Reversos o anulación de transacciones.
- Transacciones programadas.
- Movimientos simulados o proyectados.
- Intereses, comisiones, GMF o cálculos adicionales.
- Frontend, autenticación JWT y despliegue.
- Modificación de saldos o movimientos mediante este endpoint.

## Reglas de negocio

### Validación de cuenta

- La cuenta debe existir en la base de datos.
- Si no existe, devolver HTTP 404 con código de error `ACCOUNT_NOT_FOUND`.
- No es necesario que la cuenta esté en estado ACTIVE; se permite consultar
  cuentas canceladas.

### Consulta de movimientos

- Los movimientos son la fuente única de verdad sobre el historial transaccional.
- Cada transacción (depósito, retiro, transferencia) genera uno o dos
  movimientos contables.
- Un depósito genera un movimiento `CREDIT`.
- Un retiro genera un movimiento `DEBIT`.
- Una transferencia genera un movimiento `DEBIT` en la cuenta origen y un
  movimiento `CREDIT` en la cuenta destino.
- Si no hay movimientos, devolver una lista vacía (no es un error).

### Filtrado temporal

- `startDate` (opcional): incluir solo movimientos con `createdAt >= startDate`.
- `endDate` (opcional): incluir solo movimientos con `createdAt <= endDate`.
- Si se proporcionan ambas, validar que `startDate <= endDate`.
- Si `startDate > endDate`, devolver HTTP 400 con código de error
  `INVALID_DATE_RANGE`.
- Las fechas se interpreta en la zona horaria del servidor (UTC-5 por defecto).

### Paginación

- `page` (opcional, default: 0): página desde 0.
- `size` (opcional, default: 10): cantidad de registros por página.
- `sort` (opcional, default: `createdAt,desc`): ordenamiento por columna(s) en
  formato Spring Data.
- El response debe incluir metadatos de paginación (totalElements, totalPages,
  number, size, etc.).

### Datos devueltos

- Balance actual y availableBalance de la cuenta.
- Número de cuenta (accountNumber).
- Tipo de cuenta (accountType).
- Estado de la cuenta (status).
- Timestamp de consulta.
- Listado paginado de movimientos con:
  - UUID del movimiento.
  - Tipo de movimiento (DEBIT o CREDIT).
  - Monto (amount).
  - UUID de la transacción relacionada.
  - Timestamp del movimiento (createdAt).
  - Descripción de la transacción (si aplica).

### Comportamiento transaccional

- La consulta es de lectura pura (readOnly = true).
- No modifica saldos, estados ni movimientos.
- No crea transacciones nuevas.
- No genera efectos secundarios.

## Contrato REST previsto

### Endpoint principal

```text
GET /api/v1/accounts/number/{accountNumber}/statement
```

### Parámetros de consulta

```text
GET /api/v1/accounts/number/{accountNumber}/statement?
    startDate=2026-01-01T00:00:00&
    endDate=2026-12-31T23:59:59&
    page=0&
    size=20&
    sort=createdAt,desc
```

### Respuesta exitosa (HTTP 200)

```json
{
  "accountNumber": "1234567890",
  "accountType": "SAVINGS",
  "status": "ACTIVE",
  "balance": "50000.00",
  "availableBalance": "48500.00",
  "statementDate": "2026-06-14T13:15:30.123Z",
  "movements": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "movementType": "CREDIT",
      "amount": "5000.00",
      "transactionId": "660e8400-e29b-41d4-a716-446655440001",
      "createdAt": "2026-06-14T10:30:00.000Z",
      "description": "Deposit from payroll"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "movementType": "DEBIT",
      "amount": "1500.00",
      "transactionId": "660e8400-e29b-41d4-a716-446655440003",
      "createdAt": "2026-06-14T09:15:00.000Z",
      "description": "Transfer to savings account"
    }
  ],
  "page": {
    "totalElements": 25,
    "totalPages": 2,
    "number": 0,
    "size": 20,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### Respuesta de error - Cuenta no encontrada (HTTP 404)

```json
{
  "error": "ACCOUNT_NOT_FOUND",
  "message": "The account with number '9999999999' was not found.",
  "timestamp": "2026-06-14T13:15:30.123Z"
}
```

### Respuesta de error - Rango de fechas inválido (HTTP 400)

```json
{
  "error": "INVALID_DATE_RANGE",
  "message": "Start date must be before or equal to end date.",
  "timestamp": "2026-06-14T13:15:30.123Z"
}
```

## DTOs esperados

### `AccountStatementResponse`

```java
@Data
@Builder
public class AccountStatementResponse {
    private String accountNumber;              // "1234567890"
    private AccountType accountType;           // SAVINGS, CHECKING
    private AccountStatus status;              // ACTIVE, INACTIVE, CANCELLED
    private BigDecimal balance;                // BigDecimal con precision=19, scale=2
    private BigDecimal availableBalance;       // BigDecimal con precision=19, scale=2
    private LocalDateTime statementDate;       // Timestamp de la consulta
    private List<AccountMovementResponse> movements;  // Lista de movimientos
    private PageInfo page;                     // Metadatos de paginación
}
```

### `AccountMovementResponse`

```java
@Data
@Builder
public class AccountMovementResponse {
    private UUID id;                           // UUID del movimiento
    private MovementType movementType;         // DEBIT, CREDIT
    private BigDecimal amount;                 // Monto del movimiento
    private UUID transactionId;                // UUID de la transacción relacionada
    private LocalDateTime createdAt;           // Timestamp del movimiento
    private String description;                // Descripción de la transacción
}
```

### `PageInfo`

```java
@Data
@Builder
public class PageInfo {
    private long totalElements;
    private int totalPages;
    private int number;                 // Current page (0-indexed)
    private int size;                   // Page size
    private boolean hasNext;
    private boolean hasPrevious;
}
```

## Cambios en repositorio

### `AccountMovementRepository` - Método nuevo

```java
Page<AccountMovementEntity> findByAccountIdAndCreatedAtBetween(
    UUID accountId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Pageable pageable
);
```

Alternativa: si se necesita filtrar por número de cuenta:

```java
@Query("""
    SELECT m FROM AccountMovementEntity m
    JOIN m.account a
    WHERE a.accountNumber = :accountNumber
    AND m.createdAt BETWEEN :startDate AND :endDate
    ORDER BY m.createdAt DESC
""")
Page<AccountMovementEntity> findByAccountNumberAndDateRange(
    @Param("accountNumber") String accountNumber,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate,
    Pageable pageable
);
```

### `AccountRepository` - Sin cambios

Usa `findByAccountNumber(String accountNumber)` existente de HU-004.

## Capa de servicio

### `AccountStatementService` (nuevo)

```java
@Service
public class AccountStatementService {
    
    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final AccountStatementMapper mapper;
    
    @Transactional(readOnly = true)
    public AccountStatementResponse getStatement(
            String accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        // 1. Validar rango de fechas
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException(
                "INVALID_DATE_RANGE",
                "Start date must be before or equal to end date."
            );
        }
        
        // 2. Obtener cuenta
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "ACCOUNT_NOT_FOUND",
                "The account with number '" + accountNumber + "' was not found."
            ));
        
        // 3. Obtener movimientos (paginado)
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.MIN;
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.MAX;
        
        Page<AccountMovementEntity> movements =
            movementRepository.findByAccountIdAndCreatedAtBetween(
                account.getId(),
                start,
                end,
                pageable
            );
        
        // 4. Mapear response
        return mapper.toStatementResponse(account, movements);
    }
}
```

## Capa de controlador

### `AccountStatementController` (nuevo)

```java
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Statements", description = "Account statement operations")
public class AccountStatementController {
    
    private final AccountStatementService statementService;
    
    @GetMapping("/number/{accountNumber}/statement")
    @Operation(summary = "Get account statement")
    @ApiResponse(responseCode = "200", description = "Account statement retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid date range")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountStatementResponse> getStatement(
            @PathVariable String accountNumber,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(...));
        AccountStatementResponse response =
            statementService.getStatement(accountNumber, startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }
}
```

## Pruebas mínimas esperadas

### Pruebas unitarias en `AccountStatementServiceTest`

1. **Consultar estado de cuenta con movimientos:**
   - Dado: una cuenta existente con 3 movimientos
   - Cuando: se solicita el estado
   - Entonces: se devuelve la cuenta, balance actual y lista de 3 movimientos

2. **Consultar cuenta existente sin movimientos:**
   - Dado: una cuenta existente sin movimientos
   - Cuando: se solicita el estado
   - Entonces: se devuelve la cuenta, balance y lista vacía de movimientos

3. **Consultar cuenta inexistente:**
   - Dado: un número de cuenta que no existe
   - Cuando: se solicita el estado
   - Entonces: se lanza `ResourceNotFoundException` con código `ACCOUNT_NOT_FOUND`

4. **Filtrar por rango de fechas válido:**
   - Dado: una cuenta con 5 movimientos entre enero y junio
   - Cuando: se consulta con startDate = marzo, endDate = mayo
   - Entonces: se devuelven solo los 2 movimientos en ese rango

5. **Rechazar rango de fechas inválido (startDate > endDate):**
   - Dado: startDate = 2026-06-14 y endDate = 2026-06-01
   - Cuando: se solicita el estado
   - Entonces: se lanza `InvalidRequestException` con código `INVALID_DATE_RANGE`

6. **Validar paginación:**
   - Dado: una cuenta con 25 movimientos
   - Cuando: se consulta con size = 10, page = 0
   - Entonces: se devuelven 10 movimientos, totalElements = 25, totalPages = 3

7. **Confirmar que no modifica saldos:**
   - Dado: una cuenta con balance = 50000.00
   - Cuando: se consulta el estado 5 veces
   - Entonces: el balance sigue siendo 50000.00

8. **Validar estructura del response:**
   - Cuando: se obtiene un statement
   - Entonces: contiene accountNumber, status, balance, availableBalance, movements y page

### Pruebas MockMvc en `AccountStatementControllerTest`

1. **GET /api/v1/accounts/number/{accountNumber}/statement - 200 OK:**
   - Verificar que devuelve HTTP 200
   - Verificar que el JSON contiene las claves esperadas
   - Verificar que movements es un array

2. **GET con startDate y endDate - 200 OK:**
   - Verificar que filtra por rango de fechas
   - Verificar que los movimientos están dentro del rango

3. **GET con startDate > endDate - 400 Bad Request:**
   - Verificar que devuelve HTTP 400
   - Verificar que error = "INVALID_DATE_RANGE"

4. **GET con accountNumber inexistente - 404 Not Found:**
   - Verificar que devuelve HTTP 404
   - Verificar que error = "ACCOUNT_NOT_FOUND"

5. **GET con parámetros de paginación - 200 OK:**
   - Verificar que page.number = 0 cuando se solicita page = 0
   - Verificar que page.size = 10 cuando se solicita size = 10
   - Verificar que movements.length = 10

6. **GET sin filtros - 200 OK:**
   - Verificar que devuelve todos los movimientos con paginación por defecto

## Riesgos técnicos

### R1: Performance en cuentas con muchos movimientos

**Riesgo:** Una cuenta activa durante años puede tener miles de movimientos,
afectando la latencia.

**Mitigación:**
- Usar índices en `AccountMovement(account_id, created_at)`.
- Implementar paginación obligatoria (size máximo = 100).
- Evaluar desnormalización de agregados en futuras versiones.

### R2: Precisión temporal en filtros

**Riesgo:** Las comparaciones `createdAt BETWEEN` pueden incluir o excluir
movimientos según truncamiento o milisegundos.

**Mitigación:**
- Documentar que los filtros incluyen milisegundos exactos.
- Usar `LocalDateTime` en base de datos (TIMESTAMP) sin zona horaria.
- Incluir ejemplos en OpenAPI.

### R3: Cambios de saldos durante la consulta

**Riesgo:** Si una transacción ocurre entre obtener account y obtener movements,
los datos pueden no ser consistentes.

**Mitigación:**
- Usar `@Transactional(readOnly = true)` en servicio.
- Usar nivel de aislamiento READ_COMMITTED (por defecto en PostgreSQL).
- Evaluar SERIALIZABLE solo si se requiere consistencia estricta.

### R4: Interferencia con HU-007

**Riesgo:** Si HU-007 agrega pruebas de integración con datos, puede afectar
cuentas con movimientos predefinidos.

**Mitigación:**
- Usar fixtures con `@BeforeEach` aisladas por test.
- No compartir datos entre tests de HU-006 y HU-007.

## Criterios de aceptación

- ✓ La consulta devuelve HTTP 200 con estructura JSON correcta.
- ✓ Balance y availableBalance reflejan el estado actual de la cuenta.
- ✓ Los movimientos incluyen DEBIT y CREDIT en orden descending por defecto.
- ✓ Si no hay movimientos, la lista es vacía (no error).
- ✓ Cuentas inexistentes devuelven HTTP 404.
- ✓ Rango de fechas inválido devuelve HTTP 400.
- ✓ Paginación funciona correctamente (page, size, totalElements, totalPages).
- ✓ startDate y endDate son opcionales.
- ✓ La consulta no modifica saldos ni movimientos.
- ✓ La consulta es de lectura pura (@Transactional(readOnly = true)).
- ✓ Existen pruebas unitarias de servicio.
- ✓ Existen pruebas MockMvc de controlador.
- ✓ `mvn clean verify` finaliza correctamente con 75+ tests.
- ✓ La cobertura de código de HU-006 es >= 80%.
- ✓ OpenAPI/Swagger documentan el endpoint con ejemplos.

## Commit esperado

```text
docs(statement): define HU-006 account statement plan
```

## Validación

- Documentación aprobada antes de iniciar implementación.
- Plan alineado con HU-005 (AccountMovement como fuente).
- Diagrama de flujo incluido en revisión de código.
- DTOs y respuestas validadas contra contrato OpenAPI.
- Todas las reglas de negocio representadas en pruebas.

---

**Nota:** Este plan reemplaza la documentación anterior y sirve como referencia
única para la implementación de HU-006. Cualquier cambio debe documentarse
mediante enmienda a este archivo antes de iniciar la codificación.
