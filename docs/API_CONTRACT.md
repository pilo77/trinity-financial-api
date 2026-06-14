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

### Crear cuenta

```http
POST /api/v1/accounts
```

Entrada prevista:

```json
{
  "customerId": "UUID",
  "accountType": "SAVINGS",
  "gmfExempt": false
}
```

El número, estado, saldo y saldo disponible son asignados por el sistema.

- `201 Created`: cuenta creada.
- `400 Bad Request`: solicitud inválida.
- `404 Not Found`: cliente inexistente.

### Listar cuentas

```http
GET /api/v1/accounts?customerId={customerId}&status=ACTIVE&page=0&size=20
```

- `200 OK`: página de cuentas.

### Consultar cuenta

```http
GET /api/v1/accounts/{id}
```

- `200 OK`: cuenta encontrada.
- `404 Not Found`: cuenta inexistente.

### Cambiar estado

```http
PATCH /api/v1/accounts/{id}/status
```

Permitirá transiciones entre `ACTIVE` e `INACTIVE`. La cancelación tiene una
operación independiente.

- `200 OK`: estado actualizado.
- `400 Bad Request`: transición inválida.
- `404 Not Found`: cuenta inexistente.

### Cancelar cuenta

```http
PATCH /api/v1/accounts/{id}/cancel
```

- `200 OK`: cuenta marcada como `CANCELLED`.
- `404 Not Found`: cuenta inexistente.
- `409 Conflict`: saldo diferente de cero o cuenta ya cancelada.

### Consultar estado de cuenta

```http
GET /api/v1/accounts/number/{accountNumber}/statement?from={dateTime}&to={dateTime}&page=0&size=20
```

- `200 OK`: movimientos construidos desde `AccountMovement`.
- `400 Bad Request`: rango de fechas inválido.
- `404 Not Found`: número de cuenta inexistente.

## Transacciones

### Consignar

```http
POST /api/v1/accounts/{id}/deposits
```

Entrada prevista:

```json
{
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
POST /api/v1/accounts/{id}/withdrawals
```

- `201 Created`: transacción y movimiento débito creados.
- `400 Bad Request`: importe inválido.
- `404 Not Found`: cuenta inexistente.
- `409 Conflict`: saldo insuficiente o estado inválido.

### Transferir

```http
POST /api/v1/transfers
```

Entrada prevista:

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
