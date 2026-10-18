# Contrato: Roles, la lista de usuarios y el 403 común

Se cambia el contrato antes que el código (principio IV, ADR-015). Cada operación protegida declara su permiso con `x-mdd-permiso` (research §4).

## `contratos/roles/openapi.yaml` (nuevo)

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarFuncionalidades` | `GET /api/roles/funcionalidades` | `USUARIOS_ROLES:LECTURA` | 200 `[Funcionalidad]` |
| `listarRoles` | `GET /api/roles` | `USUARIOS_ROLES:LECTURA` | 200 `[Rol]` |
| `crearRol` | `POST /api/roles` | `USUARIOS_ROLES:ESCRITURA` | 201 `Rol` · 400 · 409 `ROLES_NOMBRE_REPETIDO` |
| `consultarRol` | `GET /api/roles/{identificador}` | `USUARIOS_ROLES:LECTURA` | 200 `Rol` · 404 |
| `editarRol` | `PUT /api/roles/{identificador}` | `USUARIOS_ROLES:ESCRITURA` | 200 `Rol` · 400 · 404 · 409 `ROLES_ROL_PREDEFINIDO`, `ROLES_NOMBRE_REPETIDO` o `ROLES_SIN_ADMINISTRADOR` |
| `borrarRol` | `DELETE /api/roles/{identificador}` | `USUARIOS_ROLES:ESCRITURA` | 204 · 404 · 409 `ROLES_ROL_PREDEFINIDO` o `ROLES_ROL_ASIGNADO` |
| `listarRolesDeUsuarios` | `GET /api/roles/usuarios` | `USUARIOS_ROLES:LECTURA` | 200 `[RolesDeUsuario]` |
| `asignarRoles` | `PUT /api/roles/usuarios/{usuario}` | `USUARIOS_ROLES:ESCRITURA` | 200 `RolesDeUsuario` · 400 (rol desconocido) · 409 `ROLES_SIN_ADMINISTRADOR` |
| `consultarAutorizacion` | `GET /interno/roles/autorizaciones/{usuario}` | — (token de proceso, ADR-018) | 200 `Autorizacion` |

Todas las operaciones de `/api/` responden además 401 `NoAutenticado`, 403 `AccesoNoAutorizado`, 500 y 503.

**Esquemas:**

| Esquema | Campos |
|---|---|
| `Funcionalidad` | `codigo`, `nombre`, `tipos` (`[LECTURA, ESCRITURA, APROBACION, FIRMA]`, los que admite) |
| `Permiso` | `funcionalidad`, `tipo`, `alcance` (`TODO` o `PROPIO`) |
| `Rol` | `identificador` (solo lectura), `nombre` (1 a 80, con al menos una letra o un dígito: `pattern`), `descripcion` (hasta 300), `predefinido` (solo lectura), `permisos` (`minItems: 1`), `usuariosAsignados` (solo lectura, entero) |
| `RolNuevo` / `RolEditado` | `nombre`, `descripcion`, `permisos` |
| `RolesDeUsuario` | `usuario`, `roles` (identificadores, puede estar vacía). Son las asignaciones de rol de un usuario (research §12) |
| `Autorizacion` | `usuario`, `roles`, `permisos` (texto `FUNCIONALIDAD:TIPO[:PROPIO]`, ordenados) |

Un permiso con un tipo que la funcionalidad no admite responde 400 con el detalle del campo. Un rol sin permisos (`permisos: []`) responde 400 por `minItems`.

El identificador de un rol nuevo se deriva del nombre (data-model.md). Dos nombres con el mismo identificador derivado son el mismo nombre: 409 `ROLES_NOMBRE_REPETIDO`, también contra los 10 de D-16.

## `contratos/usuarios/openapi.yaml` (cambia)

- **Nuevo:** `listarUsuarios`, `GET /api/usuarios/usuarios`, `x-mdd-permiso: USUARIOS_ROLES:LECTURA`. Responde 200 `[UsuarioResumen]` (`usuario`, `nombre`, `activo`), ordenados por usuario.
- **`recibirEvento`:** el 403 deja de ser «sin cuerpo hasta F05-H02» y pasa a `AccesoNoAutorizado`. Lo mismo en los demás contratos que lo mencionan.

## `contratos/comun/seguridad.yaml` (cambia)

- `SesionActual` gana `permisos` (opcional, `[string]`, como en el token). Solo Usuarios lo devuelve.
- Nueva respuesta `AccesoNoAutorizado`: 403 con el esquema `Error` y el código `SEGURIDAD_ACCESO_NO_AUTORIZADO`, igual para todas las causas.

## `contratos/comun/errores.md`

| Código | Estado | Título | Descripción | Cuándo |
|---|---|---|---|---|
| `SEGURIDAD_ACCESO_NO_AUTORIZADO` | 403 | Acceso no autorizado | No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles. | Token válido sin el permiso de la operación, un token de usuario en `/interno/**` o un token de proceso fuera de él. Lo responden los 4 procesos y el API Gateway, sin decir qué faltó (F05-H02). |
| `ROLES_NOMBRE_REPETIDO` | 409 | Nombre repetido | Ya existe un rol con ese nombre, o con uno que solo cambia en mayúsculas, tildes o signos. Elige otro. | Crear o renombrar un rol con un nombre que da el mismo identificador que otro, incluidos los 10 de D-16. |
| `ROLES_ROL_PREDEFINIDO` | 409 | Rol de la matriz D-16 | Los roles de la matriz D-16 no se pueden cambiar ni borrar. Crea un rol nuevo con los permisos que necesitas. | Editar o borrar uno de los 10 roles de D-16 (D-63). |
| `ROLES_ROL_ASIGNADO` | 409 | Rol asignado | El rol está asignado a usuarios. Quítaselo antes de borrarlo. | Borrar un rol asignado. La pantalla muestra a cuántos usuarios con `usuariosAsignados`; el registro del servidor también lo anota. |
| `ROLES_SIN_ADMINISTRADOR` | 409 | Sin administrador | El cambio dejaría al sistema sin nadie que administre los roles. | Un cambio de los roles de un usuario, o la edición de un rol, quitaría el último `USUARIOS_ROLES:ESCRITURA` o se lo quitaría a quien hace el cambio. |

`CodigosComunes` (plataforma) suma el primero; `ErroresRoles` suma los otros cuatro, y una prueba los compara con este catálogo (ADR-016).

## Token de sesión

`permisos` nuevo y `roles` desde Roles ([data-model.md](../data-model.md#token-de-sesión-cambia)). Los procesos los convierten en autoridades `ROLE_<rol>` y `PERMISO_<funcionalidad>:<tipo>`.
