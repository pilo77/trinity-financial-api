# Worklog

| Historia | Descripción | Estado |
|---|---|---|
| HU-001 | Project foundation | DONE |
| HU-002 | Spring Boot bootstrap | DONE |
| HU-003 | Customer CRUD | DONE |
| HU-004 | Account management | IN PROGRESS |
| HU-005 | Financial transactions | PENDING |
| HU-006 | Account statement | PENDING |
| HU-007 | Tests and docs | PENDING |
| HU-008 | Frontend demo | PENDING |
| HU-009 | Cloud deploy | PENDING |

## HU-003 - Customer CRUD

### Objetivo

Implementar la administración completa de clientes mediante una API REST,
respetando validaciones de datos, mayoría de edad, unicidad y restricciones de
eliminación.

### Alcance

- Crear, listar, consultar, actualizar y eliminar clientes.
- Persistir clientes con Spring Data JPA.
- Separar entidades de persistencia y DTOs de API.
- Consultar únicamente si un cliente tiene cuentas vinculadas.
- Probar el servicio y el controlador.

No incluye endpoints, servicios ni lógica funcional de cuentas, transacciones,
movimientos, frontend, autenticación o despliegue.

### Reglas de negocio

- El cliente debe ser mayor de edad al crear y al actualizar.
- Documento y correo deben ser únicos.
- Nombres y apellidos deben tener al menos dos caracteres.
- El correo debe tener formato válido.
- `createdAt` y `updatedAt` se asignan automáticamente.
- La eliminación es física y solo se permite sin cuentas vinculadas.

### Endpoints

```text
POST   /api/v1/customers
GET    /api/v1/customers
GET    /api/v1/customers/{id}
PUT    /api/v1/customers/{id}
DELETE /api/v1/customers/{id}
```

### Criterios de aceptación

- Las operaciones exitosas devuelven códigos HTTP apropiados.
- Los datos inválidos devuelven errores claros por campo.
- Los clientes menores de edad son rechazados.
- Los duplicados de documento o correo son rechazados.
- Una actualización modifica `updatedAt` y conserva `createdAt`.
- Un cliente sin cuentas puede eliminarse.
- Un cliente con cuentas vinculadas no puede eliminarse.
- Existen pruebas unitarias de servicio y pruebas MockMvc del controlador.
- `mvn clean verify` finaliza correctamente.

### Riesgos

- Una validación previa de unicidad no sustituye el constraint de base de datos.
- El correo debe normalizarse para evitar duplicados por mayúsculas.
- La restricción de cuentas debe consultarse sin implementar el módulo Account.

### Commit esperado

```text
feat(customers): implement customer CRUD
```

### Validación

- CRUD HTTP validado de extremo a extremo con base H2 en memoria.
- Pruebas unitarias de servicio y pruebas MockMvc del controlador.
- Build completo validado con `mvn clean verify`.

## HU-004 - Account Management

### Estado

PLAN UPDATED - PENDING APPROVAL

### Actividad actual

- Se resolvieron documentalmente los hallazgos P0, P1 y P2 de la auditoría.
- Se definió que `SAVINGS` y `CHECKING` se crean en estado `ACTIVE`, indicando
  por separado el requisito y la decisión del MVP.
- Se formalizó la semántica de `balance` y `availableBalance`.
- Se fijó un máximo de 5 intentos para generar un número de cuenta único.
- Se separaron el cambio de estado operativo y la cancelación.
- Se validaron V1 y V2 sobre PostgreSQL real.
- Se confirmó que la tabla `accounts` ya tiene las columnas, constraints e
  índice requeridos, por lo que no se justifica una migración V3.

### Gate

No se ha iniciado implementación funcional. HU-004 permanece pendiente de
aprobación explícita antes de crear clases Java o pruebas.
