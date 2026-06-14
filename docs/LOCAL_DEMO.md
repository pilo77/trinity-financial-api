# Demostración local

## Estado

La infraestructura local, el CRUD de clientes y la administración de cuentas
pueden ejecutarse y validarse. Las transacciones financieras y el estado de
cuenta todavía no están implementados.

## Prerrequisitos

- Java 21
- Maven Wrapper incluido en el repositorio
- Docker con Docker Compose
- Git

No será necesario instalar Maven globalmente si se utiliza `mvnw`.

## Configuración local

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
DB_NAME
DB_PORT
DB_USERNAME
DB_PASSWORD
CORS_ALLOWED_ORIGINS
```

Los valores reales no deben aparecer en documentación, commits, logs o capturas.

## Flujo de demostración

1. Levantar PostgreSQL.
2. Ejecutar migraciones Flyway al iniciar la aplicación.
3. Comprobar el health check.
4. Abrir Swagger UI.
5. Crear, consultar, actualizar, listar y eliminar un cliente mayor de edad.
6. Comprobar validaciones de correo, nombres, edad y duplicados.
7. Crear una cuenta de ahorro y una cuenta corriente para el cliente.
8. Verificar prefijos, longitud, estado, saldos y atributo GMF.
9. Listar cuentas usando filtros por cliente, tipo y estado.
10. Consultar una cuenta por UUID y por número de cuenta.
11. Inactivar y volver a activar una cuenta.
12. Cancelar una cuenta con ambos saldos en cero y comprobar que no se reactive.
13. Intentar eliminar un cliente con cuentas y verificar el rechazo.
14. Realizar consignación, retiro y transferencia cuando HU-005 esté lista.
15. Consultar el estado de cuenta cuando HU-006 esté lista.

## Validaciones esperadas

- Build exitoso.
- Pruebas aprobadas.
- PostgreSQL disponible.
- Health check correcto.
- Swagger accesible.
- Operaciones críticas con códigos HTTP correctos.
- Rollback completo ante una transferencia fallida.
- Ningún secreto visible.

## Comandos

```powershell
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Validaciones de infraestructura:

```text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/v3/api-docs
GET http://localhost:8080/swagger-ui.html
```

La demostración backend completa se cerrará cuando existan los módulos de
transacciones y estado de cuenta.
