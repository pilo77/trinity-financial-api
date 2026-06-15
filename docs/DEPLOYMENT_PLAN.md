# Deployment Plan

## Arquitectura objetivo

```text
Vercel (Angular)
        |
        | HTTPS / REST
        v
Render o Railway (Spring Boot container)
        |
        | red privada
        v
PostgreSQL administrado
```

## Principios

- Ningun secreto se versiona.
- El frontend recibe la URL publica del backend durante el build.
- El backend recibe datasource, CORS y puerto por variables de entorno.
- PostgreSQL usa una conexion privada cuando la plataforma la ofrece.
- `/actuator/health` es el health check del servicio backend.
- Flyway aplica migraciones versionadas al iniciar.

## Fases

1. Validar Angular y Spring Boot localmente.
2. Construir la imagen Docker del backend.
3. Crear PostgreSQL administrado en la misma region del backend.
4. Crear el servicio backend desde el `Dockerfile`.
5. Validar health, OpenAPI y endpoints principales.
6. Crear el proyecto Vercel con Root Directory `frontend`.
7. Configurar `API_URL` con la URL HTTPS del backend y desplegar Angular.
8. Configurar `FRONTEND_ALLOWED_ORIGINS` con el dominio final de Vercel.
9. Revalidar clientes, cuentas, transacciones y estados de cuenta.

## Gates

- `npm run build` y pruebas Angular exitosas.
- `mvnw verify` y 77 pruebas backend exitosas.
- `docker compose config` valido.
- Imagen Docker construida y ejecutada localmente.
- No hay `.env`, secretos, `target`, `dist` ni `node_modules` versionados.
- No se inicia despliegue real sin autorizacion.

## Riesgo aceptado de dependencias de desarrollo

La auditoria de dependencias debe evaluarse con ambos comandos:

```powershell
npm audit --omit=dev
npm audit
```

Un resultado limpio en `--omit=dev` confirma que el bundle productivo no tiene
vulnerabilidades conocidas en dependencias de runtime. Los avisos que permanezcan
exclusivamente en Angular CLI, Vite, esbuild u otras herramientas de build se
documentan como riesgo de desarrollo y se actualizan cuando exista una version
compatible, sin usar `npm audit fix --force`.

## Calidad frontend pendiente

El proyecto no incorpora ESLint actualmente. Agregarlo requiere dependencias y
configuracion adicionales; se mantiene como mejora P2 no bloqueante mientras
TypeScript, Angular build y la suite de pruebas finalicen correctamente.
