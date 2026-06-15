# Frontend Demo

## Requisitos

- Node.js compatible con Angular 21.
- Java 21.
- Docker Desktop para PostgreSQL local.
- Variables locales definidas a partir de `.env.example`, sin versionar `.env`.

## Instalar el frontend

```powershell
Set-Location frontend
npm install
```

## Levantar PostgreSQL y backend

Desde la raiz del repositorio:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Backend y Swagger:

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Levantar Angular

```powershell
Set-Location frontend
npm start
```

La aplicacion queda disponible en `http://localhost:4200`. El desarrollo usa
`proxy.conf.json` para enviar `/api` a `http://localhost:8080`; los componentes
no contienen URLs del backend.

## Flujo funcional

1. Crear un cliente mayor de edad en **Clientes**.
2. Crear una cuenta de ahorro o corriente para el cliente.
3. Registrar una consignacion sobre una cuenta activa.
4. Registrar un retiro con saldo suficiente.
5. Transferir entre dos cuentas activas diferentes.
6. Copiar el UUID de la operacion y consultarlo en **Transacciones**.
7. Consultar el estado de cuenta usando un rango de fechas valido.

Las operaciones consumen el backend real. No se utilizan datos mock para los
flujos financieros principales.

## Validacion

```powershell
Set-Location frontend
npm run build
npm test -- --watch=false
npm audit --omit=dev
npm audit
```
