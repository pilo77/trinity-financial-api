# Estrategia de pruebas

## Objetivo

Validar las reglas de negocio, el contrato HTTP y la atomicidad de las
operaciones financieras sin convertir la prueba técnica en una plataforma de
testing innecesariamente compleja.

## Herramientas

- JUnit 5
- Mockito
- AssertJ
- MockMvc
- Spring Boot Test

Testcontainers PostgreSQL se evaluará como mejora posterior. No es obligatorio
para la primera iteración del MVP.

## Pruebas de servicios

Los servicios se probarán de forma unitaria con dependencias simuladas.

### CustomerService

- Crear un cliente válido.
- Rechazar documento duplicado.
- Rechazar correo duplicado.
- Rechazar menor de edad.
- Rechazar actualización que deje al cliente como menor de edad.
- Actualizar un cliente existente.
- Eliminar un cliente sin cuentas.
- Rechazar eliminación cuando tenga cuentas vinculadas.

### AccountService

- Crear cuenta de ahorro activa.
- Generar 10 dígitos con prefijo `53` para ahorro.
- Generar 10 dígitos con prefijo `33` para corriente.
- Evitar números duplicados.
- Inicializar ambos saldos en cero.
- Guardar el indicador `gmfExempt`.
- Activar e inactivar una cuenta.
- Cancelar una cuenta con saldo cero.
- Rechazar cancelación con saldo distinto de cero.

### TransactionService

- Consignar y actualizar ambos saldos.
- Crear movimiento crédito al consignar.
- Retirar y actualizar ambos saldos.
- Rechazar retiro con saldo insuficiente.
- Crear movimiento débito al retirar.
- Rechazar operaciones sobre cuentas no habilitadas.
- Rechazar transferencia entre la misma cuenta.
- Transferir y actualizar las dos cuentas.
- Crear un débito y un crédito para la transferencia.
- Comprobar que una falla no deje cambios parciales.

### StatementService

- Consultar movimientos por número de cuenta.
- Filtrar por rango de fechas.
- Rechazar rangos inválidos.
- Paginar y ordenar de forma estable.
- Rechazar números de cuenta inexistentes.

## Pruebas de controladores

Se utilizará `@WebMvcTest` y MockMvc para validar:

- Rutas y métodos HTTP.
- Serialización y deserialización.
- Jakarta Validation.
- Códigos `200`, `201`, `204`, `400`, `404` y `409`.
- Delegación al servicio.
- Respuesta del manejador global de excepciones.
- Ausencia de trazas y detalles internos en errores.

## Pruebas de integración

La primera integración debe cubrir como mínimo:

- Migraciones Flyway sobre PostgreSQL.
- Restricciones únicas.
- Relaciones y claves foráneas.
- Persistencia de una transferencia completa.
- Rollback de saldos y movimientos ante una falla.

Estas pruebas pueden ejecutarse contra PostgreSQL de Docker Compose al inicio.
Testcontainers será un plus si el tiempo disponible permite automatizar el
entorno sin aumentar de forma desproporcionada la complejidad.

## Cobertura

No se impondrá una cifra artificial como único indicador. La prioridad será
cubrir:

- Reglas financieras.
- Casos negativos.
- Límites de cada módulo.
- Manejo de errores.
- Atomicidad de transferencias.

## Estado actual de la suite

- Total actual: 77 pruebas.
- Resultado actual: `BUILD SUCCESS`.
- Cobertura por módulo:
	- `CustomerService` y `AccountService` validan reglas de negocio y casos
		negativos de cliente y cuenta.
	- `TransactionService` e `TransactionServiceIntegrationTest` validan saldos,
		movimientos y rollback.
	- `AccountStatementService` valida consultas, filtros, paginación y errores.
	- `AccountController`, `CustomerController`, `TransactionController` y el
		controlador de estado de cuenta validan contrato HTTP con MockMvc.

## Comandos objetivo

Una vez creado el proyecto:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

## Criterio de aprobación

- Todas las pruebas pasan.
- No hay pruebas ignoradas sin justificación.
- Los casos críticos tienen pruebas positivas y negativas.
- La aplicación compila desde un checkout limpio.
- Los resultados no dependen de credenciales personales.
