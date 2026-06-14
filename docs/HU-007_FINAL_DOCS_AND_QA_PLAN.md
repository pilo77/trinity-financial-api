# HU-007 - Final Docs and QA

## 1. Objetivo

Realizar el cierre de calidad del backend obligatorio de la prueba técnica,
dejando documentación final, validación local, evidencia de endpoints, guía de
ejecución y preparación para publicación en GitHub, sin introducir cambios
funcionales en clientes, cuentas, transacciones o estado de cuenta.

## 2. Alcance

Incluye revisión y actualización documental, validaciones locales y
preparación de una entrega final trazable para el backend ya implementado.

Fuera de alcance:

- No frontend.
- No deploy cloud.
- No JWT.
- No cambios funcionales grandes.
- No refactors amplios.
- No nuevas entidades.
- No migraciones nuevas.

## 3. Archivos a revisar o actualizar

Documentación base a revisar:

- `README.md`
- `docs/API_CONTRACT.md`
- `docs/BUSINESS_RULES.md`
- `docs/LOCAL_DEMO.md`
- `docs/TESTING_STRATEGY.md`
- `docs/WORKLOG.md`

Artefactos a crear o actualizar durante HU-007:

- `docs/FINAL_DELIVERY_CHECKLIST.md`
- `docs/API_REQUESTS.http` o equivalente

## 4. Checklist de calidad

- Confirmar que el backend obligatorio ya cubre clientes, cuentas,
  transacciones y estado de cuenta.
- Verificar que la documentación describa el comportamiento real del sistema.
- Revisar que los ejemplos de API coincidan con las rutas y payloads actuales.
- Verificar que el lenguaje de los documentos no prometa features no
  implementadas.
- Asegurar que no queden referencias obsoletas a módulos pendientes.

## 5. Checklist de seguridad

- Confirmar que no existan secretos versionados.
- Verificar que `.env` siga ignorado.
- Revisar que no haya credenciales, tokens o datos sensibles en ejemplos.
- Confirmar que no se agreguen artefactos basura del IDE o del sistema.
- Verificar que no aparezcan directivas internas `::git-*`.

## 6. Checklist de ejecución local

- Validar que `docker compose up -d` levanta PostgreSQL local.
- Validar que Flyway migra desde base limpia cuando aplica.
- Validar que la ejecución con Maven Wrapper finaliza correctamente.
- Validar que la aplicación arranca con el perfil local esperado.
- Validar que Swagger UI y Actuator Health responden localmente.

## 7. Checklist de endpoints

- `POST /api/v1/customers`
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `PUT /api/v1/customers/{id}`
- `DELETE /api/v1/customers/{id}`
- `POST /api/v1/accounts`
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `GET /api/v1/accounts/number/{accountNumber}`
- `PATCH /api/v1/accounts/{id}/status`
- `PATCH /api/v1/accounts/{id}/cancel`
- `GET /api/v1/accounts/number/{accountNumber}/statement`
- `POST /api/v1/transactions/deposits`
- `POST /api/v1/transactions/withdrawals`
- `POST /api/v1/transactions/transfers`

## 8. Validaciones automáticas

- Ejecutar la validación completa con Maven Wrapper.
- Revisar `git status` antes y después de cambios documentales.
- Confirmar que no se agreguen archivos inesperados.
- Confirmar que no queden errores de compilación o pruebas.
- Confirmar que la documentación nueva no introduce inconsistencias con el API
  real.

## 9. Validaciones manuales

- Crear cliente mayor de edad.
- Crear cuenta de ahorro.
- Crear cuenta corriente.
- Consignar.
- Retirar.
- Transferir.
- Consultar estado de cuenta.
- Validar rechazo de menor de edad.
- Validar retiro sin fondos.
- Validar cancelación con saldo cero.
- Revisar Swagger y comparar con el contrato escrito.
- Revisar la guía de demo local de punta a punta.

## 10. Riesgos

- Que la documentación se desalineé del código existente si no se contrasta
  contra los endpoints reales.
- Que queden ejemplos desactualizados o demasiado genéricos.
- Que aparezcan referencias a entregables aún no implementados, como frontend o
  despliegue cloud.
- Que se mezclen cambios funcionales con el cierre documental.

## 11. Criterios de aceptación

- La documentación final refleja el backend real implementado.
- El flujo completo de validación local queda descrito y reproducible.
- El checklist de calidad y seguridad está cerrado.
- No se introducen cambios funcionales nuevos.
- El repositorio queda listo para publicar en GitHub sin push todavía.

## 12. Commit esperado

```text
docs(qa): define final documentation and QA plan
```

## Resultado esperado de esta fase

HU-007 debe dejar el terreno listo para la documentación final y la validación
manual del backend, sin alterar el comportamiento de negocio ya aprobado.