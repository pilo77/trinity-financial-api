# HU-008 - Angular Financial Frontend

## Objetivo

Construir una interfaz Angular financiera conectada al contrato REST existente,
sin introducir autenticacion, endpoints nuevos ni logica de negocio duplicada.

## Alcance

- Angular 21 standalone y strict mode.
- Rutas lazy para dashboard, clientes, cuentas, transacciones, estados y
  configuracion.
- HttpClient centralizado y modelos alineados con los DTO del backend.
- Formularios reactivos con estados de carga, error y exito.
- Layout responsive orientado a una entidad financiera.
- Ambientes Angular y proxy local.
- Build productivo y pruebas de shell.

## Decisiones

- Angular 21 se eligio porque Node 24.14.0 no cumple el minimo de Angular 22.
- No se agrego una libreria visual externa para mantener el bundle pequeno.
- Las reglas financieras permanecen exclusivamente en Spring Boot.
- El frontend muestra errores normalizados desde `ProblemDetail` sin trazas.

## Validacion

```powershell
cd frontend
npm test -- --watch=false
npm run build
```
