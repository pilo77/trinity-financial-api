# Demostración local

## Estado

La infraestructura local y el CRUD de clientes pueden ejecutarse y validarse.
Los módulos de cuentas y transacciones todavía no están implementados.

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
7. Crear una cuenta de ahorro y una cuenta corriente cuando HU-004 esté lista.
8. Verificar prefijos, longitud, estado, saldos y atributo GMF.
9. Realizar consignación, retiro y transferencia cuando HU-005 esté lista.
10. Consultar el estado de cuenta por número cuando HU-006 esté lista.
11. Comprobar movimientos débito y crédito.
12. Intentar eliminar un cliente con cuentas y verificar el rechazo.
13. Dejar una cuenta en saldo cero y cancelarla.

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

La demostración funcional completa se cerrará cuando existan los módulos de
cuentas y transacciones.
