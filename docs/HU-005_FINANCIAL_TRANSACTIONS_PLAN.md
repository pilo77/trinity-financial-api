# HU-005 - Financial Transactions Plan

## Estado

PLAN UPDATED - PENDING REAPPROVAL

Este documento define la implementación de consignaciones, retiros y
transferencias. Incorpora los ajustes P1 y P2 de la segunda auditoría. No
autoriza todavía la creación de código funcional de HU-005.

## Objetivo

Implementar operaciones financieras sobre cuentas existentes, actualizando
`balance` y `availableBalance` de forma atómica y registrando la transacción y
sus movimientos contables.

## Alcance funcional

- Crear consignaciones.
- Crear retiros.
- Crear transferencias entre cuentas distintas.
- Consultar una transacción por UUID.
- Validar importes, existencia, estado y fondos disponibles.
- Actualizar ambos saldos en cada operación exitosa.
- Persistir `FinancialTransaction`.
- Persistir uno o dos `AccountMovement`, según el tipo de operación.
- Garantizar rollback completo ante cualquier fallo.

## Fuera de alcance

- Consulta de estado de cuenta y listado de movimientos, que pertenecen a
  HU-006.
- Reversos, anulaciones, transacciones programadas o idempotencia distribuida.
- Intereses, comisiones, GMF y sobregiros.
- Persistencia de intentos fallidos.
- Frontend, autenticación JWT y despliegue.

## Contrato REST previsto

```text
POST /api/v1/transactions/deposits
POST /api/v1/transactions/withdrawals
POST /api/v1/transactions/transfers
GET  /api/v1/transactions/{id}
```

### Consignación

```json
{
  "accountNumber": "5300000001",
  "amount": 100000.00,
  "description": "Consignación"
}
```

### Retiro

```json
{
  "accountNumber": "5300000001",
  "amount": 50000.00,
  "description": "Retiro"
}
```

### Transferencia

```json
{
  "sourceAccountNumber": "5300000001",
  "destinationAccountNumber": "3300000001",
  "amount": 25000.00,
  "description": "Transferencia"
}
```

Los tres `POST` responderán `201 Created`. La consulta responderá `200 OK`.

Errores previstos:

- `400 Bad Request`: importe cero, negativo, con escala inválida, números de
  cuenta mal formados o transferencia hacia la misma cuenta.
- `404 Not Found`: cuenta o transacción inexistente.
- `409 Conflict`: cuenta no activa o fondos insuficientes.

## Validación de solicitudes

- Los importes usarán `BigDecimal`.
- `DepositRequest.amount`, `WithdrawalRequest.amount` y
  `TransferRequest.amount` usarán exactamente:
  - `@NotNull`;
  - `@Positive`;
  - `@Digits(integer = 17, fraction = 2)`.
- Un importe con más de dos decimales será rechazado por Jakarta Validation con
  `400 Bad Request` y un error claro asociado a `amount`.
- La escala inválida no deberá alcanzar la persistencia ni producir un error
  de base de datos o una respuesta `500`.
- Los números de cuenta serán obligatorios y tendrán exactamente 10 dígitos.
- `description` será opcional y tendrá máximo 255 caracteres.
- No se aceptarán IDs, estados, saldos, fechas ni tipos de movimiento enviados
  por el cliente.

Se usarán Jakarta Validation y validaciones de negocio en el servicio.

## Reglas de negocio

### Reglas comunes

- Solo una cuenta `ACTIVE` permite operaciones financieras.
- Las cuentas `INACTIVE` y `CANCELLED` serán rechazadas.
- Toda operación exitosa actualizará `balance` y `availableBalance`.
- En este MVP, ambos saldos variarán por el mismo importe.
- Ningún saldo podrá quedar negativo.
- Las comparaciones monetarias usarán `BigDecimal.compareTo`.
- Las fechas se asignarán con `LocalDateTime` usando el `Clock` de la
  aplicación.

### Consignación

- Incrementa ambos saldos.
- Registra una transacción `DEPOSIT` con estado `SUCCESS`.
- Usa `destinationAccount` para identificar la cuenta acreditada.
- Mantiene `sourceAccount` en `null`.
- Genera un movimiento `CREDIT`.

### Retiro

- Requiere `availableBalance` suficiente.
- Disminuye `balance` y `availableBalance`.
- Registra una transacción `WITHDRAWAL` con estado `SUCCESS`.
- Usa `sourceAccount` para identificar la cuenta debitada.
- Mantiene `destinationAccount` en `null`.
- Genera un movimiento `DEBIT`.

### Transferencia

- Origen y destino deben ser cuentas diferentes.
- Ambas cuentas deben existir y estar `ACTIVE`.
- La cuenta origen debe tener `availableBalance` suficiente.
- Disminuye ambos saldos en el origen.
- Incrementa ambos saldos en el destino.
- Registra una transacción `TRANSFER` con estado `SUCCESS`.
- Genera un movimiento `DEBIT` para el origen.
- Genera un movimiento `CREDIT` para el destino.
- Los dos movimientos compartirán la misma transacción.

## Semántica de saldos y movimientos

`balance` representa el saldo contable y `availableBalance` el saldo disponible.
HU-005 no introduce retenciones, por lo que ambos se actualizan con el mismo
importe.

`AccountMovement.balanceAfterMovement` almacenará el `balance` contable de la
cuenta inmediatamente después de aplicar ese movimiento.

Los movimientos serán inmutables. HU-005 los persistirá, pero no expondrá aún
un endpoint para listarlos.

## Persistencia de transacciones fallidas

El MVP persistirá únicamente operaciones exitosas con estado `SUCCESS`.

Aunque el enum y el esquema admiten `FAILED`, guardar un intento fallido dentro
de la misma transacción haría que también se revierta. Persistir fallos exigiría
una transacción independiente y una política adicional de auditoría, por lo que
queda fuera de HU-005.

## Atomicidad y concurrencia

Cada consignación, retiro o transferencia se ejecutará dentro de un único
método de servicio anotado con `@Transactional`.

Para evitar actualizaciones perdidas:

- La cuenta afectada por consignación o retiro se consultará con bloqueo
  pesimista de escritura.
- La transferencia bloqueará origen y destino.
- Las dos cuentas se bloquearán en un orden determinista, por número de cuenta,
  comparado como cadena lexicográfica ascendente, para reducir el riesgo de
  deadlock en transferencias cruzadas.
- Las validaciones de estado y fondos se realizarán después de adquirir los
  bloqueos.

Si falla una actualización o persistencia:

- no se conservarán cambios de saldos;
- no se conservará `FinancialTransaction`;
- no se conservará ningún `AccountMovement`.

## Modelo previsto

### FinancialTransactionEntity

- `id`: UUID.
- `transactionType`: `DEPOSIT`, `WITHDRAWAL` o `TRANSFER`.
- `amount`: `BigDecimal`.
- `sourceAccount`: nullable.
- `destinationAccount`: nullable.
- `status`: `SUCCESS` o `FAILED`.
- `description`: nullable, máximo 255.
- `transactionDate`: `LocalDateTime`, fecha efectiva de la operación.
- `createdAt`: `LocalDateTime`.

### AccountMovementEntity

- `id`: UUID.
- `account`: obligatorio.
- `financialTransaction`: obligatorio.
- `movementType`: `CREDIT` o `DEBIT`.
- `amount`: `BigDecimal`.
- `balanceAfterMovement`: `BigDecimal`.
- `createdAt`: `LocalDateTime`.

### Enums

- `TransactionType`: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`.
- `TransactionStatus`: `SUCCESS`, `FAILED`.
- `MovementType`: `CREDIT`, `DEBIT`.

## Composición de respuestas

`TransactionResponse` tendrá:

- `id`;
- `transactionType`;
- `amount`;
- `sourceAccountNumber`, nullable;
- `destinationAccountNumber`, nullable;
- `status`;
- `description`, nullable;
- `transactionDate`;
- `createdAt`;
- `movements`: lista mínima de movimientos asociados.

Cada `AccountMovementResponse` incluirá:

- `id`;
- `accountNumber`;
- `movementType`;
- `amount`;
- `balanceAfterMovement`;
- `createdAt`.

La lista de movimientos pertenece únicamente al detalle de la transacción
consultada o recién creada. No implementa historial por cuenta, filtros por
fecha, paginación de movimientos ni estado de cuenta. Esas capacidades
permanecen en HU-006.

## Cambios previstos en Account

`AccountEntity` incorporará operaciones de dominio para acreditar y debitar
ambos saldos, validando que nunca queden negativos y actualizando `updatedAt`.

`AccountRepository` incorporará consultas con bloqueo pesimista para una cuenta
y para el par de cuentas de una transferencia. Se conservarán los métodos
utilizados por Customer y Account Management.

## Estructura prevista

```text
src/main/java/com/trinity/financial/transaction/
├── controller/
│   └── TransactionController.java
├── dto/
│   ├── AccountMovementResponse.java
│   ├── DepositRequest.java
│   ├── TransactionMapper.java
│   ├── TransactionResponse.java
│   ├── TransferRequest.java
│   └── WithdrawalRequest.java
├── entity/
│   ├── AccountMovementEntity.java
│   ├── FinancialTransactionEntity.java
│   ├── MovementType.java
│   ├── TransactionStatus.java
│   └── TransactionType.java
├── repository/
│   ├── AccountMovementRepository.java
│   └── FinancialTransactionRepository.java
└── service/
    └── TransactionService.java
```

Pruebas previstas:

```text
src/test/java/com/trinity/financial/transaction/
├── controller/TransactionControllerTest.java
├── service/TransactionServiceTest.java
└── service/TransactionServiceIntegrationTest.java
```

`TransactionServiceIntegrationTest` será obligatorio y demostrará rollback real
contra la base de datos. La garantía de atomicidad no se sustituirá únicamente
con mocks.

## Evidencia de migraciones

### Verificación real sobre PostgreSQL

Se levantó una instancia limpia de PostgreSQL 17 con Docker Compose, usando un
proyecto y volumen temporales aislados. Se ejecutaron V1 y V2 con
`ON_ERROR_STOP=1` y se consultaron `information_schema`, `pg_constraint` y
`pg_indexes`. Al finalizar se eliminaron el contenedor, la red y el volumen de
auditoría.

### financial_transactions

Columnas verificadas:

| Columna | Tipo real | Nullable | Resultado |
|---|---|---|---|
| `id` | `uuid` | no | presente |
| `transaction_type` | `varchar` | no | presente |
| `amount` | `numeric(19,2)` | no | presente |
| `source_account_id` | `uuid` | sí | presente |
| `destination_account_id` | `uuid` | sí | presente |
| `status` | `varchar` | no | presente |
| `description` | `varchar` | sí | presente |
| `transaction_date` | - | - | **faltante** |
| `created_at` | `timestamp without time zone` | no | presente |

Constraints verificados:

- PK sobre `id`;
- `ck_transactions_type` para `DEPOSIT`, `WITHDRAWAL` y `TRANSFER`;
- `ck_transactions_status` para `SUCCESS` y `FAILED`;
- `ck_transactions_amount` con `amount > 0`;
- `ck_transactions_different_accounts`;
- FK de `source_account_id` hacia `accounts(id)`;
- FK de `destination_account_id` hacia `accounts(id)`.

Índices verificados:

- PK de `id`;
- `idx_transactions_source_account`;
- `idx_transactions_destination_account`;
- `idx_transactions_created_at`.

### account_movements

Columnas verificadas:

| Columna | Tipo real | Nullable | Resultado |
|---|---|---|---|
| `id` | `uuid` | no | presente |
| `account_id` | `uuid` | no | presente |
| `financial_transaction_id` | `uuid` | no | presente |
| `movement_type` | `varchar` | no | presente |
| `amount` | `numeric(19,2)` | no | presente |
| `balance_after_movement` | `numeric(19,2)` | no | presente |
| `created_at` | `timestamp without time zone` | no | presente |

Constraints verificados:

- PK sobre `id`;
- `ck_movements_type` para `CREDIT` y `DEBIT`;
- `ck_movements_amount` con `amount > 0`;
- `ck_movements_balance` con saldo posterior no negativo;
- FK de `account_id` hacia `accounts(id)`;
- FK de `financial_transaction_id` hacia `financial_transactions(id)`.

Índices verificados:

- PK de `id`;
- `idx_movements_account_created_at`;
- `idx_movements_transaction`.

### Decisión de migración

El esquema no cumple completamente el contrato final porque falta
`financial_transactions.transaction_date`.

Durante la implementación será obligatoria:

```text
V3__add_transaction_date_to_financial_transactions.sql
```

La migración deberá:

1. agregar `transaction_date TIMESTAMP`;
2. respaldar filas existentes con el valor de `created_at`;
3. establecer la columna como `NOT NULL`;
4. crear `idx_transactions_transaction_date`.

`transactionDate` será la fecha efectiva de la operación financiera y
`createdAt` conservará la fecha de creación técnica del registro.

La migración V3 no se crea durante esta fase documental.

## Estrategia de implementación

1. Crear y validar la migración V3 documentada.
2. Crear enums y entidades JPA alineados con V1/V2/V3.
3. Añadir métodos de crédito y débito en `AccountEntity`.
4. Añadir consultas con bloqueo a `AccountRepository`.
5. Crear repositorios de transacciones y movimientos.
6. Crear DTOs con Jakarta Validation.
7. Implementar mapper y servicio transaccional.
8. Implementar los cuatro endpoints.
9. Crear pruebas unitarias, MockMvc y prueba obligatoria de rollback.
10. Actualizar contrato, reglas, demo y worklog.
11. Ejecutar `mvn clean verify` y auditoría de alcance.

## Estrategia de pruebas

### Servicio

- Consignación válida y movimiento `CREDIT`.
- Retiro válido y movimiento `DEBIT`.
- Retiro con fondos insuficientes.
- Monto cero o negativo.
- Cuenta inexistente.
- Cuenta `INACTIVE`.
- Cuenta `CANCELLED`.
- Transferencia válida con movimientos `DEBIT` y `CREDIT`.
- Transferencia hacia la misma cuenta.
- Transferencia con fondos insuficientes.
- Cuenta destino inexistente.
- Actualización de ambos saldos.
- Persistencia de relaciones source/destination según el tipo.
- En depósito, `sourceAccount` es `null` y `destinationAccount` corresponde a
  la cuenta receptora.
- En retiro, `sourceAccount` corresponde a la cuenta debitada y
  `destinationAccount` es `null`.
- Rollback completo ante un fallo después de iniciar una transferencia.

### Controlador

- `201` para las tres operaciones exitosas.
- `200` para consulta por UUID.
- `400` para solicitudes inválidas.
- `400` con error de campo para importes con más de dos decimales.
- `404` para recursos inexistentes.
- `409` para estado inválido o fondos insuficientes.
- Errores de enum o formato sin respuestas `500`.

### Integración

- Aplicación de V1/V2 y validación JPA.
- Aplicación de V3 y presencia de `transaction_date`.
- Persistencia atómica de transacción, movimientos y saldos.
- Rollback real obligatorio sin cambios parciales.
- Bloqueos verificados mediante las consultas configuradas.

## Riesgos y mitigaciones

- **Actualizaciones perdidas:** bloqueo pesimista antes de validar y modificar.
- **Deadlocks en transferencias cruzadas:** bloqueo en orden determinista.
- **Orden de bloqueo ambiguo:** comparación lexicográfica ascendente de
  `accountNumber`.
- **Cambios parciales:** un único límite `@Transactional` y prueba de rollback.
- **Errores de precisión:** `BigDecimal`, escala máxima de dos decimales y
  `NUMERIC(19,2)`.
- **Saldos divergentes:** métodos de dominio actualizan ambos saldos juntos.
- **Movimientos inconsistentes:** creación dentro de la misma transacción y
  relaciones obligatorias.
- **Auditoría engañosa de fallos:** no persistir `FAILED` hasta diseñar una
  transacción independiente.
- **Ampliación accidental a HU-006:** persistir movimientos sin exponer su
  consulta paginada.

## Archivos que se modificarían

- `src/main/java/com/trinity/financial/account/entity/AccountEntity.java`
- `src/main/java/com/trinity/financial/account/repository/AccountRepository.java`
- `src/main/resources/db/migration/V3__add_transaction_date_to_financial_transactions.sql`
- `docs/API_CONTRACT.md`
- `docs/BUSINESS_RULES.md`
- `docs/LOCAL_DEMO.md`
- `docs/WORKLOG.md`

También se crearían los archivos del módulo y las pruebas enumerados en la
estructura prevista.

## Criterios de aceptación

- Las tres operaciones financieras respetan las reglas documentadas.
- Saldos, transacción y movimientos se persisten atómicamente.
- No existen saldos negativos.
- Las cuentas no activas son rechazadas.
- Las transferencias no permiten cuentas iguales.
- El rollback evita cualquier cambio parcial.
- Los requests rechazan importes con más de dos decimales antes de persistir.
- La respuesta puede incluir movimientos de la transacción sin implementar
  estado de cuenta.
- Los cuatro endpoints responden con códigos y errores consistentes.
- No se implementa el estado de cuenta de HU-006.
- `mvn clean verify` termina correctamente.

## Commits esperados

Plan:

```text
docs(transactions): define HU-005 financial transactions plan
```

Implementación:

```text
feat(transactions): implement financial transactions
```

## Gate de aprobación

No se crearán entidades, repositorios, DTOs, servicios, controladores ni pruebas
de HU-005 hasta recibir aprobación explícita de este plan.
