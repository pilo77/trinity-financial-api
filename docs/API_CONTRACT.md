# Contrato de API

## Convenciones

- Base path: `/api/v1`
- Contenido: `application/json`
- Identificadores internos: UUID
- Número de cuenta: cadena numérica de 10 caracteres
- Importes: número decimal positivo con máximo dos decimales
- Fechas de nacimiento: `YYYY-MM-DD`
- Fechas y horas: formato ISO 8601
- Listados: paginados cuando aplique

Las entidades JPA no se exponen directamente.

## Clientes

Resumen de endpoints:

- `POST /api/v1/customers`
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `PUT /api/v1/customers/{id}`
- `DELETE /api/v1/customers/{id}`

### Crear cliente

```http
POST /api/v1/customers
```

Entrada:

```json
{
  "documentType": "CC",
  "documentNumber": "123456789",
  "firstName": "Carlos",
  "lastName": "Villamil",
  "email": "carlos@example.com",
  "phone": "3000000000",
  "birthDate": "1990-01-01"
}
```

Respuestas:

- `201 Created`: cliente creado.
- `400 Bad Request`: datos inválidos o cliente menor de edad.
- `409 Conflict`: documento o correo duplicado.

### Listar clientes

```http
GET /api/v1/customers?page=0&size=20
```

- `200 OK`: página de clientes.

La respuesta paginada contiene:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

### Consultar cliente

```http
GET /api/v1/customers/{id}
```

- `200 OK`: cliente encontrado.
- `404 Not Found`: cliente inexistente.

### Actualizar cliente

```http
PUT /api/v1/customers/{id}
```

La actualización vuelve a validar mayoría de edad, documento y correo.
El cuerpo utiliza los mismos campos obligatorios de la creación.

- `200 OK`: cliente actualizado.
- `400 Bad Request`: datos inválidos o menor de edad.
- `404 Not Found`: cliente inexistente.
- `409 Conflict`: documento o correo duplicado.

### Eliminar cliente

```http
DELETE /api/v1/customers/{id}
```

- `204 No Content`: cliente eliminado.
- `404 Not Found`: cliente inexistente.
- `409 Conflict`: el cliente tiene cuentas vinculadas.

## Cuentas

Resumen de endpoints:

- `POST /api/v1/accounts`
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `GET /api/v1/accounts/number/{accountNumber}`
- `PATCH /api/v1/accounts/{id}/status`
- `PATCH /api/v1/accounts/{id}/cancel`
- `GET /api/v1/accounts/number/{accountNumber}/statement`

### Crear cuenta

```http
POST /api/v1/accounts
```

Entrada:

```json
{
  "customerId": "UUID",
  "accountType": "SAVINGS",
  "gmfExempt": false
}
```

`gmfExempt` es opcional y toma `false` por defecto cuando no se envía.

El número y el estado son asignados por el sistema. Tanto `SAVINGS` como
`CHECKING` se crean en estado `ACTIVE`; para `SAVINGS` es un requisito del
enunciado y para `CHECKING` es una decisión documentada del MVP.

`balance` y `availableBalance` se inicializan con `BigDecimal.ZERO` y no pueden
ser proporcionados por el consumidor.

- `201 Created`: cuenta creada.
- `400 Bad Request`: solicitud inválida. Un `accountType` desconocido debe
  producir un mensaje claro y nunca un error `500`.
- `404 Not Found`: cliente inexistente.
- `409 Conflict`: no fue posible generar un número único después de 5 intentos.

### Listar cuentas

```http
GET /api/v1/accounts?customerId={customerId}&accountType=SAVINGS&status=ACTIVE&page=0&size=20&sort=createdAt,desc
```

- `200 OK`: página de cuentas.
- `400 Bad Request`: filtros, paginación u ordenamiento inválidos.

### Consultar cuenta

```http
GET /api/v1/accounts/{id}
```

- `200 OK`: cuenta encontrada.
- `404 Not Found`: cuenta inexistente.

### Consultar cuenta por número

```http
GET /api/v1/accounts/number/{accountNumber}
```

- `200 OK`: cuenta encontrada.
- `400 Bad Request`: el número no contiene exactamente 10 dígitos.
- `404 Not Found`: número de cuenta inexistente.

### Cambiar estado

```http
PATCH /api/v1/accounts/{id}/status
```

Entrada prevista:

```json
{
  "status": "INACTIVE"
}
```

Solo permite `ACTIVE` o `INACTIVE`. No permite enviar `CANCELLED` ni reactivar
una cuenta cancelada. La cancelación tiene una operación independiente.

- `200 OK`: estado actualizado.
- `400 Bad Request`: estado solicitado inválido.
- `404 Not Found`: cuenta inexistente.
- `409 Conflict`: transición no permitida por el estado actual.

### Cancelar cuenta

```http
PATCH /api/v1/accounts/{id}/cancel
```

La cancelación requiere que `balance` y `availableBalance` sean ambos
exactamente cero.

- `200 OK`: cuenta marcada como `CANCELLED`.
- `404 Not Found`: cuenta inexistente.
- `409 Conflict`: alguno de los saldos es diferente de cero o la cuenta ya está
  cancelada.

### Consultar estado de cuenta

```http
GET /api/v1/accounts/number/{accountNumber}/statement?startDate={dateTime}&endDate={dateTime}&page=0&size=20&sort=createdAt,desc
```

- `200 OK`: estado de cuenta con balance actual, availableBalance actual,
  movimientos `DEBIT`/`CREDIT` y metadatos de paginación.
- `400 Bad Request`: número de cuenta inválido o rango de fechas inválido.
- `404 Not Found`: número de cuenta inexistente.

La respuesta del estado de cuenta incluye:

- Información básica de la cuenta (`accountNumber`, `accountType`, `status`).
- Saldos actuales (`balance`, `availableBalance`).
- Lista paginada de movimientos construida desde `AccountMovement`.
- Fecha de generación del statement.

## Transacciones

Resumen de endpoints:

- `POST /api/v1/transactions/deposits`
- `POST /api/v1/transactions/withdrawals`
- `POST /api/v1/transactions/transfers`
- `GET /api/v1/transactions/{id}`

### Consignar

```http
POST /api/v1/transactions/deposits
```

Entrada:

```json
{
  "accountNumber": "5300000001",
  "amount": 100000.00,
  "description": "Consignación"
}
```

- `201 Created`: transacción y movimiento crédito creados.
- `400 Bad Request`: importe inválido.
- `404 Not Found`: cuenta inexistente.
- `409 Conflict`: estado de cuenta no permite la operación.

### Retirar

```http
POST /api/v1/transactions/withdrawals
```

El cuerpo usa `accountNumber`, `amount` y `description` con las mismas
validaciones de la consignación.

- `201 Created`: transacción y movimiento débito creados.
- `400 Bad Request`: importe inválido.
- `404 Not Found`: cuenta inexistente.
- `409 Conflict`: saldo insuficiente o estado inválido.

### Transferir

```http
POST /api/v1/transactions/transfers
```

Entrada:

```json
{
  "sourceAccountNumber": "5300000001",
  "destinationAccountNumber": "3300000001",
  "amount": 50000.00,
  "description": "Transferencia"
}
```

La operación crea una `FinancialTransaction`, un movimiento `DEBIT` en el
origen y un movimiento `CREDIT` en el destino.

- `201 Created`: transferencia completada.
- `400 Bad Request`: importe inválido o cuentas iguales.
- `404 Not Found`: alguna cuenta no existe.
- `409 Conflict`: saldo insuficiente o estado inválido.

### Consultar transacción

```http
GET /api/v1/transactions/{id}
```

- `200 OK`: transacción encontrada.
- `404 Not Found`: transacción inexistente.

Los tres requests validan `amount` con `@NotNull`, `@Positive` y
`@Digits(integer = 17, fraction = 2)`. Una escala mayor a dos decimales produce
`400 Bad Request`.

La respuesta incluye datos de la transacción y sus movimientos mínimos. Estos
movimientos son detalle de la operación y no constituyen el estado de cuenta.

## Formato de errores

Se utilizará una respuesta compatible con `ProblemDetail`, con información como:

```json
{
  "type": "about:blank",
  "title": "Business rule violation",
  "status": 409,
  "detail": "The operation cannot be completed",
  "instance": "/api/v1/resource",
  "code": "BUSINESS_RULE_VIOLATION",
  "fieldErrors": []
}
```

Los mensajes no incluirán trazas, SQL ni información sensible.
