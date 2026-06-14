# Final Delivery Checklist

## Build

- La validación completa con Maven Wrapper finaliza correctamente.
- No hay errores de compilación.
- No hay fallos de pruebas.

## Pruebas

- La suite completa pasa.
- Los casos críticos tienen pruebas positivas y negativas.
- La validación de integración sigue funcionando con la base de prueba.

## Seguridad

- No hay secretos versionados.
- `.env` no está rastreado.
- No hay credenciales, tokens ni datos sensibles en documentos o ejemplos.
- No hay archivos basura del IDE o del sistema.

## Git

- `git status` está limpio o explicado.
- `git diff --stat` muestra solo cambios esperados.
- `git diff --name-status` coincide con el alcance aprobado.
- `git diff --check` no reporta problemas.

## Swagger

- Swagger UI abre correctamente.
- El contrato publicado coincide con la documentación.
- Los endpoints principales están visibles y navegables.

## Docker Compose

- PostgreSQL local levanta correctamente.
- El contenedor responde antes de ejecutar la aplicación.
- No se usa `docker compose down -v` sin autorización.

## Flyway

- Las migraciones corren desde una base limpia.
- El esquema final coincide con la aplicación.
- No hay migraciones nuevas fuera del alcance aprobado.

## Antes de crear remote

- El proyecto está validado localmente.
- La documentación refleja el comportamiento real.
- No hay cambios funcionales pendientes de cierre.

## Antes de push

- El commit final está revisado.
- El diff no contiene archivos no deseados.
- La rama está lista para compartirse.

## Entrega final

- README actualizado.
- Contrato de API actualizado.
- Reglas de negocio actualizadas.
- Guía local actualizada.
- Estrategia de pruebas actualizada.
- Checklist final disponible.
- Colección HTTP manual disponible.