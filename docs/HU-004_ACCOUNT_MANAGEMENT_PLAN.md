# HU-004 - Account Management Plan

## Estado

APPROVED - IMPLEMENTED

Este documento resolvió los hallazgos P0, P1 y P2 de la auditoría técnica. El
plan fue aprobado antes de iniciar la implementación funcional de HU-004.

## Objetivo

Implementar, después de la aprobación de este plan, la administración de
cuentas de ahorro y cuentas corrientes asociadas a clientes existentes.

La historia cubrirá creación, consulta, listado paginado, cambio entre estados
operativos y cancelación lógica. Las operaciones financieras pertenecen a
HU-005 y quedan fuera de este alcance.

## Alcance previsto

- Crear cuentas `SAVINGS` y `CHECKING`.
- Asociar cada cuenta a un cliente existente.
- Generar un número de cuenta único de 10 dígitos.
- Consultar una cuenta por su identificador.
- Listar cuentas con paginación, ordenamiento y filtros permitidos.
- Cambiar el estado operativo entre `ACTIVE` e `INACTIVE`.
- Cancelar una cuenta mediante una operación independiente.
- Exponer DTOs de entrada y salida sin publicar entidades JPA.
- Mantener manejo global y consistente de errores.

## Fuera de alcance

- Consignaciones, retiros y transferencias.
- Modificación de saldos por transacciones.
- Estado de cuenta y movimientos.
- Frontend.
- Autenticación o JWT.
- Despliegue.

## Decisiones de negocio

### Estado inicial

- `SAVINGS` se crea en estado `ACTIVE` por requisito explícito del enunciado.
- `CHECKING` se crea en estado `ACTIVE` por decisión documentada del MVP.

La segunda decisión permite administrar la cuenta corriente inmediatamente y
mantiene un comportamiento consistente en la demostración. No se infiere como
requisito externo; es una decisión interna, explícita y revisable del MVP.

### Saldos

`Account` tendrá dos valores monetarios independientes:

- `balance`: saldo contable.
- `availableBalance`: saldo disponible para operaciones.

Ambos:

- se representarán con `BigDecimal`;
- se inicializarán con `BigDecimal.ZERO`;
- se persistirán como `NUMERIC(19,2)`;
- no serán recibidos en `CreateAccountRequest`;
- no serán modificados funcionalmente durante HU-004.

La cancelación solo será válida cuando `balance.compareTo(BigDecimal.ZERO) == 0`
y `availableBalance.compareTo(BigDecimal.ZERO) == 0`.

### Exención de GMF

`gmfExempt` estará presente en `CreateAccountRequest`. Será opcional para el
consumidor de la API y su valor por defecto será `false` cuando no se envíe.
En el dominio y en la persistencia será un booleano no nulo.

### Estados y cancelación

- Los estados del dominio serán `ACTIVE`, `INACTIVE` y `CANCELLED`.
- `PATCH /api/v1/accounts/{id}/status` solo aceptará `ACTIVE` o `INACTIVE`.
- El endpoint de estado no podrá cancelar ni reactivar una cuenta cancelada.
- `PATCH /api/v1/accounts/{id}/cancel` será una operación separada porque exige
  validar ambos saldos en cero.
- La cancelación será lógica: cambiará el estado a `CANCELLED` y conservará la
  cuenta.
- Cancelar una cuenta ya cancelada producirá un error de negocio controlado.

### Generación del número de cuenta

- El número tendrá exactamente 10 dígitos.
- `SAVINGS` utilizará el prefijo `53`.
- `CHECKING` utilizará el prefijo `33`.
- Los ocho dígitos restantes serán generados por `AccountNumberGenerator`.
- La aplicación realizará como máximo 5 intentos para obtener un número único.
- La restricción única de base de datos seguirá siendo la garantía final ante
  concurrencia.
- Si se agotan los 5 intentos, se lanzará una excepción de negocio controlada
  con el código `ACCOUNT_NUMBER_GENERATION_FAILED`, sin filtrar detalles
  internos.

### Validación de tipos

`accountType` solo aceptará `SAVINGS` o `CHECKING`. Un valor desconocido o con
formato inválido responderá `400 Bad Request` con un mensaje claro y estable;
nunca deberá convertirse en un error `500`.

## Contrato REST previsto

```text
POST  /api/v1/accounts
GET   /api/v1/accounts
GET   /api/v1/accounts/{id}
PATCH /api/v1/accounts/{id}/status
PATCH /api/v1/accounts/{id}/cancel
```

El listado podrá recibir:

```text
customerId, accountType, status, page, size, sort
```

Los valores exactos, respuestas y códigos HTTP se mantienen en
`docs/API_CONTRACT.md`.

## Diseño técnico previsto

La implementación seguirá la organización modular existente:

```text
account/
├── domain/
├── application/
├── infrastructure/
└── web/
```

Responsabilidades previstas:

- `AccountEntity`: persistencia de la cuenta y auditoría temporal.
- `AccountRepository`: acceso JPA y consultas de existencia, filtros y número.
- `AccountNumberGenerator`: creación de candidatos sin acceso a persistencia.
- `AccountService`: reglas, transiciones, reintentos y límites transaccionales.
- `AccountController`: contrato HTTP, validación y delegación.
- DTOs y mapper: separación entre dominio, persistencia y API.

La interfaz mínima usada por HU-003 para validar cuentas vinculadas deberá
evolucionar sin romper `CustomerService`.

## Límite transaccional previsto

Las operaciones de creación, cambio de estado y cancelación se ejecutarán con
`@Transactional`. Las consultas usarán transacciones de solo lectura cuando
corresponda.

HU-004 no implementará operaciones financieras ni cambios de saldo.

## Evidencia de migraciones Flyway

Se revisaron las migraciones `V1__create_initial_schema.sql` y
`V2__enforce_unique_customer_document_number.sql`.

Además, se ejecutaron V1 y V2 sobre una instancia PostgreSQL local limpia y se
consultaron los catálogos reales del motor.

La tabla `accounts` resultante contiene:

| Elemento | Evidencia |
|---|---|
| `id` | `UUID`, clave primaria |
| `account_number` | `CHAR(10)`, no nulo |
| `account_type` | `VARCHAR(20)`, no nulo |
| `status` | `VARCHAR(20)`, no nulo |
| `balance` | `NUMERIC(19,2)`, no nulo, default `0` |
| `available_balance` | `NUMERIC(19,2)`, no nulo, default `0` |
| `gmf_exempt` | `BOOLEAN`, no nulo, default `false` |
| `customer_id` | `UUID`, no nulo |
| `created_at` | `TIMESTAMP`, no nulo |
| `updated_at` | `TIMESTAMP`, no nulo |
| Unicidad | `uk_accounts_number` sobre `account_number` |
| Relación | `fk_accounts_customer` hacia `customers(id)` |
| Índice | `idx_accounts_customer` sobre `customer_id` |

También se confirmaron checks para tipo, estado, formato y prefijo del número de
cuenta, y saldos no negativos.

Conclusión: el esquema requerido por HU-004 ya está completo. No se creará una
migración V3 porque no existe un cambio de esquema que la justifique.

## Estrategia de pruebas prevista

### Servicio

- Creación correcta de `SAVINGS` y `CHECKING` en `ACTIVE`.
- Inicialización de ambos saldos con `BigDecimal.ZERO`.
- Valor por defecto `false` para `gmfExempt`.
- Rechazo de cliente inexistente.
- Generación de prefijos y control de colisiones.
- Error controlado después de 5 intentos fallidos.
- Cambio permitido entre `ACTIVE` e `INACTIVE`.
- Rechazo de estado `CANCELLED` en el endpoint operativo.
- Cancelación con ambos saldos en cero.
- Rechazo de cancelación si cualquiera de los dos saldos no es cero.
- Rechazo de reactivación o cancelación repetida.

### Controlador

- Códigos HTTP y cuerpos de los cinco endpoints.
- Validaciones Jakarta de solicitudes.
- `accountType` inválido responde `400`, no `500`.
- Paginación y ordenamiento del listado.
- Errores `404` y conflictos de negocio sin información interna.

## Criterios de aceptación

- Solo se administran cuentas vinculadas a clientes existentes.
- Ambos tipos se crean en `ACTIVE` conforme a las decisiones documentadas.
- Ambos saldos comienzan en cero y HU-004 no los altera.
- Los números cumplen longitud, prefijo y unicidad.
- La generación se limita a 5 intentos.
- El cambio de estado y la cancelación tienen contratos separados.
- La cancelación exige ambos saldos en cero.
- Los listados soportan `page`, `size` y `sort`.
- Los tipos inválidos producen una respuesta `400` clara.
- Las pruebas de servicio y controlador cubren reglas y errores.
- `mvn clean verify` termina correctamente.

## Riesgos y mitigaciones

- **Colisiones de número:** reintentos limitados y constraint único en base de
  datos.
- **Condición de carrera al crear:** la unicidad de PostgreSQL es la garantía
  definitiva; el error se traduce a una respuesta controlada.
- **Transiciones inconsistentes:** reglas centralizadas en el servicio.
- **Confusión entre saldos:** campos separados y cancelación validando ambos.
- **Errores de enum como 500:** manejo global y prueba MockMvc específica.
- **Ampliación accidental de alcance:** HU-005 y HU-006 permanecen excluidas.

## Cierre del gate

La aprobación explícita fue recibida antes de crear `AccountEntity`, servicio,
controlador, DTOs, mapper, generador y pruebas. HU-004 se implementó respetando
el alcance definido en este plan.
