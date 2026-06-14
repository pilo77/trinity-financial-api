# Reglas de negocio

## Clientes

1. El número de documento es obligatorio y único.
2. El correo debe tener formato válido y ser único.
3. La fecha de nacimiento se representa con `LocalDate`.
4. El cliente debe ser mayor de edad en la fecha de la operación.
5. La mayoría de edad se valida tanto al crear como al actualizar.
6. Ningún cliente menor de edad puede quedar almacenado.
7. Un cliente solo puede eliminarse cuando no tenga cuentas vinculadas.
8. La eliminación del cliente será física después de validar la restricción.

## Cuentas

1. Una cuenta pertenece a un único cliente existente.
2. Los tipos permitidos son `SAVINGS` y `CHECKING`.
3. Los estados permitidos son `ACTIVE`, `INACTIVE` y `CANCELLED`.
4. `balance` y `availableBalance` son campos independientes de tipo
   `BigDecimal`.
5. Toda cuenta nueva inicia ambos saldos con `BigDecimal.ZERO`.
6. HU-004 no modifica saldos mediante operaciones financieras.
7. Una cuenta `SAVINGS` se crea en estado `ACTIVE` por requisito del enunciado.
8. Una cuenta `CHECKING` se crea en estado `ACTIVE` por decisión del MVP, para
   permitir administración inmediata y una demo consistente.
9. El número de cuenta se genera automáticamente y es único.
10. El número de cuenta tiene exactamente 10 dígitos.
11. Las cuentas `SAVINGS` comienzan por `53`.
12. Las cuentas `CHECKING` comienzan por `33`.
13. La generación permite como máximo 5 intentos; si se agotan, se produce una
    excepción de negocio controlada.
14. `gmfExempt` es un booleano no nulo en el dominio. En la solicitud de
    creación es opcional y toma `false` por defecto.
15. El cambio de estado operativo solo permite `ACTIVE` o `INACTIVE`.
16. Una cuenta cancelada no puede reactivarse ni recibir nuevas transacciones.
17. La cancelación usa una operación separada y solo se permite cuando
    `balance` y `availableBalance` son ambos exactamente cero.
18. La cancelación cambia el estado a `CANCELLED`; no elimina la cuenta.

## Valores monetarios

1. Los importes, saldos y saldos disponibles usan `BigDecimal`.
2. La base de datos utilizará `NUMERIC(19,2)`.
3. No se utilizarán `float` ni `double` para cálculos monetarios.
4. Los importes de las transacciones deben ser mayores que cero.

## Transacciones

1. Los tipos son `DEPOSIT`, `WITHDRAWAL` y `TRANSFER`.
2. Las operaciones financieras deben ejecutarse con `@Transactional`.
3. Una consignación incrementa `balance` y `availableBalance`.
4. Un retiro disminuye `balance` y `availableBalance`.
5. Un retiro requiere saldo disponible suficiente.
6. Una transferencia requiere cuentas de origen y destino diferentes.
7. Ambas cuentas deben permitir la operación.
8. Una transferencia descuenta el importe de la cuenta origen y lo acredita en
   la cuenta destino dentro de una única transacción de base de datos.
9. Si una transferencia falla, ningún saldo ni movimiento debe quedar aplicado.
10. Toda transacción exitosa actualiza el saldo y el saldo disponible.
11. `FinancialTransaction` registra la operación general.
12. `AccountMovement` registra el efecto contable sobre cada cuenta.
13. Una transferencia exitosa genera un movimiento `DEBIT` en el origen y uno
    `CREDIT` en el destino.
14. Una consignación genera un movimiento `CREDIT`.
15. Un retiro genera un movimiento `DEBIT`.

## FinancialTransaction

Los atributos previstos son:

- `id`
- `transactionType`
- `amount`
- `sourceAccount`
- `destinationAccount`
- `status`: `SUCCESS` o `FAILED`
- `description`
- `createdAt` como `LocalDateTime`

El MVP persistirá las operaciones financieras exitosas de forma atómica. El
estado `FAILED` queda disponible para una estrategia posterior de auditoría de
intentos fallidos, que requerirá una transacción independiente para sobrevivir
al rollback de la operación principal.

## AccountMovement

Los atributos previstos son:

- `id`
- `account`
- `financialTransaction`
- `movementType`: `CREDIT` o `DEBIT`
- `amount`
- `balanceAfterMovement`
- `createdAt` como `LocalDateTime`

Los movimientos son inmutables y constituyen la fuente del estado de cuenta.

## Estado de cuenta

1. Se consulta por número de cuenta.
2. Se construye a partir de `AccountMovement`.
3. Puede filtrarse por rango de fechas.
4. Debe soportar paginación.
5. Los movimientos se ordenan por fecha y un criterio estable de desempate.
6. La respuesta incluye la identificación de la cuenta y los movimientos con
   tipo, importe, fecha y saldo posterior.

## Fechas y auditoría

- `birthDate` utiliza `LocalDate`.
- Creación, modificación, transacción y movimiento utilizan `LocalDateTime`.
- Las fechas son asignadas por la aplicación y no se aceptan libremente desde
  solicitudes externas cuando correspondan a auditoría.

## Errores

1. Jakarta Validation validará los DTO de entrada.
2. Un manejador global convertirá excepciones a respuestas HTTP consistentes.
3. Las respuestas no expondrán trazas, consultas SQL, credenciales ni detalles
   internos.
4. Los errores de negocio tendrán códigos y mensajes comprensibles.
