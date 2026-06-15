# Trinity Financial API

API REST para administrar clientes, cuentas de ahorro, cuentas corrientes y
transacciones financieras. El proyecto se desarrollará como un monolito modular
en Java y Spring Boot, con PostgreSQL y arquitectura hexagonal simplificada.

## Estado actual

El repositorio contiene la base técnica completa del backend obligatorio y la
documentación principal de la prueba técnica. Incluye configuración ejecutable,
Docker Compose, perfiles por ambiente, Flyway, OpenAPI, Actuator y manejo
global de errores. El backend funcional está implementado en sus módulos de
clientes, cuentas, transacciones y estado de cuenta.

Módulos implementados:

- Clientes.
- Cuentas de ahorro y cuentas corrientes.
- Transacciones financieras.
- Consulta de estado de cuenta.
- Frontend Angular financiero para demostración profesional.

## Alcance MVP

- CRUD de clientes.
- Administración de cuentas de ahorro y cuentas corrientes.
- Consignaciones, retiros y transferencias entre cuentas.
- Consulta de estado de cuenta por número de cuenta.
- Actualización atómica de saldo y saldo disponible.
- Registro de movimientos débito y crédito.
- Validaciones de entrada y manejo global de excepciones.
- Documentación Swagger/OpenAPI.
- PostgreSQL con migraciones Flyway.
- Pruebas unitarias de servicios y controladores.
- Docker Compose para PostgreSQL local.

No forman parte del MVP backend obligatorio: microservicios, autenticación JWT,
créditos, intereses, comisiones, sobregiros e integraciones externas.

El frontend de demostración se encuentra en `frontend/`. Mantiene el foco
principal de la prueba en el backend y consume exclusivamente el contrato REST.

El despliegue cloud también queda fuera del backend obligatorio y se reservará
para una fase posterior si el alcance lo permite.

## Arquitectura

Se utilizará un monolito modular con los módulos internos:

- `customer`
- `account`
- `transaction`
- `shared`

Cada módulo funcional conservará paquetes visibles `entity`, `service`,
`controller`, `repository` y `dto`. Los controladores no contendrán lógica de
negocio y las operaciones financieras se ejecutarán de forma transaccional.

Las entidades principales serán:

- `Customer`
- `Account`
- `FinancialTransaction`
- `AccountMovement`

Las decisiones y reglas completas están en:

- [Reglas de negocio](docs/BUSINESS_RULES.md)
- [Contrato de API](docs/API_CONTRACT.md)
- [Demostración local](docs/LOCAL_DEMO.md)
- [Estrategia de pruebas](docs/TESTING_STRATEGY.md)

## Tecnologías

- Java 21.
- Spring Boot 3.5.15.
- Maven Wrapper 3.9.16.
- Spring Web
- Spring Data JPA
- Jakarta Validation
- PostgreSQL 17 para desarrollo local.
- Flyway
- Springdoc OpenAPI 2.8.17.
- Spring Boot Actuator
- JUnit 5, Mockito y MockMvc
- Docker Compose
- Angular 21
- Docker multi-stage para Spring Boot
- Vercel como destino del frontend
- Render o Railway como destino del backend y PostgreSQL

## Configuración

La aplicación utilizará variables de entorno:

| Variable | Propósito |
|---|---|
| `SERVER_PORT` | Puerto HTTP de la aplicación |
| `SPRING_PROFILES_ACTIVE` | Perfil activo |
| `DATABASE_URL` | URL JDBC de PostgreSQL |
| `DB_NAME` | Nombre de la base usada por Docker Compose |
| `DB_PORT` | Puerto local publicado por PostgreSQL |
| `DB_USERNAME` | Usuario de base de datos |
| `DB_PASSWORD` | Contraseña de base de datos |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS permitidos |
| `FRONTEND_ALLOWED_ORIGINS` | Orígenes CORS del frontend; variable preferida |
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL en producción |
| `SPRING_DATASOURCE_USERNAME` | Usuario PostgreSQL en producción |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña PostgreSQL en producción |
| `PORT` | Puerto inyectado por la plataforma cloud |

`.env.example` solo contiene nombres de variables. Los valores reales deben
permanecer en `.env` o en el gestor de secretos del ambiente.

`.env` no se versiona. Ese archivo debe permanecer local y nunca incluirse en
commits ni en capturas compartidas.

### Perfiles disponibles

- `application.yml`: configuración común.
- `application-local.yml`: desarrollo local mediante variables de entorno.
- `application-test.yml`: configuración aislada para pruebas.
- `application-cloud.yml`: configuración cloud basada en variables de entorno.
- `application-prod.yml`: configuración productiva para Render o Railway.

La configuración común está en `application.yml`; los perfiles `local`,
`test` y `cloud` separan las conexiones y ajustes por ambiente.

## Ejecución local

Requisitos:

- Java 21.
- Docker Desktop o Docker Engine con Compose.

Maven no necesita instalación global porque el repositorio incluye Maven
Wrapper.

1. Crear el archivo local de variables:

```powershell
Copy-Item .env.example .env
```

2. Configurar en `.env` valores locales no sensibles y una URL JDBC coherente
   con PostgreSQL:

```text
SPRING_PROFILES_ACTIVE=local
DATABASE_URL=jdbc:postgresql://localhost:5432/<database>
```

3. Levantar PostgreSQL y backend con Docker:

```powershell
docker compose up --build -d
docker compose ps
```

4. Ejecutar pruebas backend:

```powershell
.\mvnw.cmd clean verify
```

5. Ejecutar el frontend:

```powershell
Set-Location frontend
npm ci
npm start
```

6. Validar:

- Health: `http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Frontend: `http://localhost:4200`

Para detener la base de datos sin eliminar su volumen:

```powershell
docker compose down
```

No se debe usar `docker compose down -v` sin autorización, porque elimina los
datos locales del volumen.

## Despliegue

La arquitectura objetivo usa:

- Angular en Vercel.
- Spring Boot como contenedor Docker en Render o Railway.
- PostgreSQL administrado en la misma plataforma y region del backend.

Vercel usa `frontend` como Root Directory, `npm run build:vercel` como Build
Command y `dist/trinity-financial-web/browser` como Output Directory. La
variable `API_URL` se inyecta durante el build y no se versiona.

El backend usa el perfil `prod`, health check `/actuator/health` y CORS
configurable con `FRONTEND_ALLOWED_ORIGINS`.

Documentacion:

- [Plan de despliegue](docs/DEPLOYMENT_PLAN.md)
- [Guia cloud](docs/CLOUD_DEPLOYMENT.md)

## Git Flow simplificado

- `main`: versión estable y entregable.
- `develop`: integración.
- `feature/customer-module`: clientes.
- `feature/account-module`: cuentas.
- `feature/transaction-module`: transacciones.
- `feature/testing-and-docs`: pruebas transversales y documentación.

La foundation documental se creó en `chore/project-foundation` y la base técnica
se preparó en `chore/spring-boot-bootstrap`. No se realizarán merges, pushes ni
cambios sobre ramas protegidas sin autorización explícita.

Convención de commits:

```text
chore: initialize Spring Boot backend project
docs: add technical test scope and architecture
feat(customers): implement customer CRUD
feat(accounts): implement account management
feat(transactions): implement financial operations
test(customers): add service and controller tests
docs: add local setup and API usage guide
```

## Controles antes de cada commit

- El proyecto compila.
- Las pruebas relacionadas pasan.
- El cambio tiene una sola intención.
- No existen secretos ni credenciales reales.
- `.env`, `target/`, logs y archivos del IDE no están rastreados.
- `git status` y `git diff` fueron revisados.
- El mensaje sigue Conventional Commits y está escrito en inglés.

## Definition of Ready

- Arquitectura, alcance y reglas aprobados.
- Contrato API revisado.
- Modelo de datos definido.
- Estrategia de migraciones y pruebas acordada.
- `.gitignore` y `.env.example` validados.
- Rama de trabajo no protegida.
- Ausencia de secretos comprobada.

## Definition of Done

- La validación completa con Maven Wrapper finaliza correctamente.
- Migraciones Flyway funcionan desde una base vacía.
- Docker Compose levanta PostgreSQL.
- Swagger publica el contrato acordado.
- CRUD, cuentas, transacciones y estado de cuenta están validados.
- README y documentos están actualizados.
- No hay secretos ni archivos generados rastreados.
- El estado Git está limpio o explicado.
