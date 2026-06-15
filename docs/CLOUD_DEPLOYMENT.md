# Cloud Deployment

## Frontend Angular en Vercel

El frontend permanece dentro del monorepo. En Vercel:

| Campo | Valor |
|---|---|
| Root Directory | `frontend` |
| Framework Preset | Angular |
| Install Command | `npm ci` |
| Build Command | `npm run build:vercel` |
| Output Directory | `dist/trinity-financial-web/browser` |

Variable requerida:

| Variable | Ejemplo seguro |
|---|---|
| `API_URL` | `https://<backend-public-domain>/api/v1` |

`API_URL` debe usar HTTPS. El script
`frontend/scripts/generate-environment.mjs` valida el valor y genera un archivo
ignorado por Git. `environment.prod.ts` solo conserva el token de plantilla y
no contiene una URL productiva.

`frontend/vercel.json` configura el build y el fallback de rutas SPA hacia
`index.html`.

Validacion local equivalente:

```powershell
cd frontend
$env:API_URL="https://api.example.invalid/api/v1"
npm run build:vercel
Remove-Item Env:API_URL
```

El dominio `.invalid` se usa exclusivamente para validar el build y no apunta a
un servicio real.

## Backend Spring Boot en Render

Crear un Web Service con runtime Docker usando el `Dockerfile` del repositorio.
El servicio debe vincularse a una instancia PostgreSQL administrada en la misma
region.

Variables:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<private-host>:5432/<database>` |
| `SPRING_DATASOURCE_USERNAME` | Gestionado como secreto |
| `SPRING_DATASOURCE_PASSWORD` | Gestionado como secreto |
| `FRONTEND_ALLOWED_ORIGINS` | `https://<project>.vercel.app` |
| `PORT` | El asignado por Render |

No copiar una URL `postgresql://` directamente a Spring JDBC. Construir
`SPRING_DATASOURCE_URL` con prefijo `jdbc:postgresql://` y los datos privados
entregados por la plataforma.

Configuracion recomendada:

- Health Check Path: `/actuator/health`
- Dockerfile Path: `./Dockerfile`
- Region: la misma de PostgreSQL
- Auto Deploy: deshabilitado durante la validacion inicial

## Backend Spring Boot en Railway

Crear PostgreSQL y el servicio backend en el mismo proyecto. Railway detecta el
`Dockerfile`; configurar:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL` en formato JDBC
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `FRONTEND_ALLOWED_ORIGINS`
- `PORT`, solo si Railway no lo inyecta automaticamente

Preferir referencias de variables de Railway sobre duplicar credenciales.

## Docker local

Crear `.env` desde `.env.example` y completar valores locales. Luego:

```powershell
docker compose config
docker compose up --build -d
docker compose ps
curl.exe http://localhost:8080/actuator/health
```

Servicios:

- `postgres`: PostgreSQL 17 con volumen persistente.
- `backend`: imagen Spring Boot construida localmente.

Para detener sin eliminar datos:

```powershell
docker compose down
```

No usar `docker compose down -v` sin respaldo y autorizacion.

## CORS

`FRONTEND_ALLOWED_ORIGINS` acepta uno o varios origenes separados por coma:

```text
https://trinity-financial.vercel.app,https://preview.example.com
```

Desarrollo local:

```text
FRONTEND_ALLOWED_ORIGINS=http://localhost:4200
```

No usar `*` en produccion.

## Checklist posterior al despliegue

1. Backend `GET /actuator/health` responde `UP`.
2. Flyway reporta las tres migraciones aplicadas.
3. OpenAPI responde sin exponer configuracion sensible.
4. Frontend carga directamente y al refrescar rutas internas.
5. CORS permite solo el dominio Vercel configurado.
6. CRUD de clientes funciona.
7. Apertura y cambio de estado de cuentas funciona.
8. Consignacion, retiro y transferencia funcionan.
9. Estado de cuenta refleja movimientos.
10. Logs no contienen credenciales ni cadenas completas de conexion.
