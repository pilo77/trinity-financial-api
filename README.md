# Trinity Financial API

API REST para administrar clientes, cuentas de ahorro, cuentas corrientes y
transacciones financieras. El proyecto se desarrollará como un monolito modular
en Java y Spring Boot, con PostgreSQL y arquitectura hexagonal simplificada.

## Estado actual

El repositorio se encuentra en fase de foundation documental y de seguridad.
Todavía no contiene código funcional, dependencias, Docker Compose ni
configuración ejecutable.

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

No forman parte del MVP: frontend, microservicios, autenticación JWT, créditos,
intereses, comisiones, sobregiros e integraciones externas.

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

## Tecnologías previstas

- Java 21
- Spring Boot 3
- Maven
- Spring Web
- Spring Data JPA
- Jakarta Validation
- PostgreSQL
- Flyway
- Springdoc OpenAPI
- JUnit 5, Mockito y MockMvc
- Docker Compose

Las versiones concretas se fijarán durante el bootstrap técnico.

## Configuración

La aplicación utilizará variables de entorno:

| Variable | Propósito |
|---|---|
| `SERVER_PORT` | Puerto HTTP de la aplicación |
| `SPRING_PROFILES_ACTIVE` | Perfil activo |
| `DATABASE_URL` | URL JDBC de PostgreSQL |
| `DB_USERNAME` | Usuario de base de datos |
| `DB_PASSWORD` | Contraseña de base de datos |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS permitidos |

`.env.example` solo contiene nombres de variables. Los valores reales deben
permanecer en `.env` o en el gestor de secretos del ambiente.

Perfiles previstos:

- `application.yml`: configuración común.
- `application-local.yml`: desarrollo local mediante variables de entorno.
- `application-test.yml`: configuración aislada para pruebas.
- `application-cloud.yml`: configuración cloud basada en variables de entorno.

Estos archivos se crearán durante el bootstrap de Spring Boot.

## Git Flow simplificado

- `main`: versión estable y entregable.
- `develop`: integración.
- `feature/customer-module`: clientes.
- `feature/account-module`: cuentas.
- `feature/transaction-module`: transacciones.
- `feature/testing-and-docs`: pruebas transversales y documentación.

La foundation se prepara en `chore/project-foundation`. No se realizarán merges,
pushes ni cambios sobre ramas protegidas sin autorización explícita.

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

- `mvn clean verify` finaliza correctamente.
- Migraciones Flyway funcionan desde una base vacía.
- Docker Compose levanta PostgreSQL.
- Swagger publica el contrato acordado.
- CRUD y operaciones financieras están validados.
- README y documentos están actualizados.
- No hay secretos ni archivos generados rastreados.
- El estado Git está limpio o explicado.
