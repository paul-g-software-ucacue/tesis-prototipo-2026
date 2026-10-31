# Contrato: cambios en `contratos/`

## `contratos/roles/openapi.yaml`

### `indicarCarrera` · `PUT /api/roles/usuarios/{usuario}/carrera`

- **Permiso**: `x-mdd-permiso: USUARIOS_ROLES:ESCRITURA`.
- **Cuerpo**: `CarreraDelCoordinador`: `{ carrera }`, con la regla de `CodigoDeCarrera` de Académico.
- **200**: `RolesDeUsuario`.
- **400**: `COMUN_SOLICITUD_INVALIDA`.
- **409**: `ROLES_CARRERA_SIN_ROL`.
- **401, 403, 500, 503**: los comunes.

### `quitarCarrera` · `DELETE /api/roles/usuarios/{usuario}/carrera`

- **Permiso**: `x-mdd-permiso: USUARIOS_ROLES:ESCRITURA`.
- **200**: `RolesDeUsuario`.
- **404**: `ROLES_USUARIO_SIN_CARRERA`.
- **401, 403, 500, 503**: los comunes.

### Esquemas que cambian

- **`RolesDeUsuario`:** suma `carrera`, opcional.
- **`Autorizacion`:** suma `carrera`, opcional.

## `contratos/academico/openapi.yaml`

- **`consultarOferta`:** para quien solo tiene «lo propio», responde solo su carrera. **403** `COMUN_CARRERA_SIN_INDICAR`.
- **`consultarAsignaturaParalelo`:** **403** con otra carrera o sin carrera.
- **`consultarAsignaturasOfertadas`** (interna): suma el parámetro opcional `carrera`.

## `contratos/generacion/openapi.yaml`

- **`consultarSugerencia`:** **403** con otra carrera o sin carrera.
- **`consultarAfinidadDelDocente`:** para quien solo tiene «lo propio», solo su carrera. **403** `COMUN_CARRERA_SIN_INDICAR`.

## Token

Reclamo nuevo y opcional `carrera`, con el código de la carrera. Sin carrera, el reclamo no va.

## Catálogo (`contratos/comun/errores.md`), con sus 8 columnas (D-72) y en «tú» (D-74)

| Código | Estado | Título | Descripción | Clasificación |
|---|---|---|---|---|
| `COMUN_CARRERA_SIN_INDICAR` | 403 | Falta tu carrera | Todavía no tienes una carrera indicada. Pide al Administrador del sistema que la indique en «Roles de usuarios». | Autorización |
| `ROLES_CARRERA_SIN_ROL` | 409 | Usuario sin rol de coordinador | Solo un usuario con el rol Coordinador de carrera puede tener carrera. Dale ese rol primero. | Negocio |
| `ROLES_USUARIO_SIN_CARRERA` | 404 | Usuario sin carrera | Este usuario no tiene una carrera indicada. | Negocio |
