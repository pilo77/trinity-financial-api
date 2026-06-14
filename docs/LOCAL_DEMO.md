# Demostración local

## Estado

La aplicación y `docker-compose.yml` todavía no han sido creados. Este documento
define el flujo objetivo y se actualizará con comandos verificables durante el
bootstrap técnico.

## Prerrequisitos previstos

- Java 21
- Maven Wrapper incluido en el repositorio
- Docker con Docker Compose
- Git

No será necesario instalar Maven globalmente si se utiliza `mvnw`.

## Configuración local prevista

1. Crear `.env` a partir de `.env.example`.
2. Completar las variables únicamente en `.env`.
3. No compartir ni versionar ese archivo.
4. Iniciar PostgreSQL con Docker Compose.
5. Ejecutar la aplicación con el perfil `local`.

Variables requeridas:

```text
SERVER_PORT
SPRING_PROFILES_ACTIVE
DATABASE_URL
DB_USERNAME
DB_PASSWORD
CORS_ALLOWED_ORIGINS
```

Los valores reales no deben aparecer en documentación, commits, logs o capturas.

## Flujo de demostración previsto

1. Levantar PostgreSQL.
2. Ejecutar migraciones Flyway al iniciar la aplicación.
3. Comprobar el health check.
4. Abrir Swagger UI.
5. Crear un cliente mayor de edad.
6. Crear una cuenta de ahorro y una cuenta corriente.
7. Verificar prefijos, longitud, estado, saldos y atributo GMF.
8. Realizar una consignación.
9. Realizar un retiro.
10. Realizar una transferencia.
11. Consultar el estado de cuenta por número.
12. Comprobar movimientos débito y crédito.
13. Intentar eliminar un cliente con cuentas y verificar el rechazo.
14. Dejar una cuenta en saldo cero y cancelarla.

## Validaciones esperadas

- Build exitoso.
- Pruebas aprobadas.
- PostgreSQL disponible.
- Health check correcto.
- Swagger accesible.
- Operaciones críticas con códigos HTTP correctos.
- Rollback completo ante una transferencia fallida.
- Ningún secreto visible.

Los comandos y URLs exactos se documentarán después de crear y validar la
aplicación.
