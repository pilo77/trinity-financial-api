# Trinity Financial Web

Frontend Angular 21 para la demostracion profesional de Trinity Financial API.

## Modulos

- Dashboard financiero.
- Clientes.
- Cuentas de ahorro y corrientes.
- Consignaciones, retiros y transferencias.
- Estados de cuenta.
- Configuracion del entorno.

## Requisitos

- Node.js 20.19+, 22.12+ o 24.x.
- Backend disponible en `http://localhost:8080`.

## Desarrollo local

```powershell
npm ci
npm start
```

El comando `npm start` usa `proxy.conf.json`. La URL base del cliente HTTP se
define en `src/environments/environment.ts`; ningun componente contiene URLs.

## Validacion

```powershell
npm test -- --watch=false
npm run build
```

El build productivo genera el ambiente a partir de `API_URL`:

```powershell
$env:API_URL="https://api.example.invalid/api/v1"
npm run build:vercel
Remove-Item Env:API_URL
```

Vercel debe usar `frontend` como Root Directory. Los detalles completos estan
en `../docs/CLOUD_DEPLOYMENT.md`.
