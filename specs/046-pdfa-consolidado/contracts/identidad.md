# Contrato: Roles y Usuarios

## Roles: `GET /interno/roles/periodos/{periodo}/responsable`

- `operationId: consultarResponsableDelPeriodo`, etiqueta `Autorizacion`.
- Solo entre procesos, con token de proceso (ADR-018), como `/interno/roles/firmantes`.

| Respuesta | Cuándo |
|---|---|
| 200 `{usuario, nombre}` | El periodo tiene Responsable de consolidación designado |
| 400, 401, 403, 500 | Los comunes |
| 404 | Sin designación (`ROLES_SIN_RESPONSABLE`) |

`nombre` es el nombre para mostrar del usuario (D-59), por la interfaz publicada nueva de Usuarios, `NombreDeUsuario`. Si no tiene, el nombre de usuario.

## Usuarios

- `bitacora.servicio` admite `firma`: migración nueva y la enumeración `servicio` en `contratos/usuarios/openapi.yaml`.
- `NombreDeUsuario.nombre(String usuario)` → `Optional<String>` (API publicada en el proceso `identidad`).
- Después de cambiar los contratos: `./gradlew :servicios:usuarios:test --rerun`.

## Catálogo

| Código | HTTP | Título |
|---|---|---|
| `ROLES_SIN_RESPONSABLE` | 404 | Sin Responsable de consolidación |
