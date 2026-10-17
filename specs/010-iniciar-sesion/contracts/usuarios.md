# Contrato: cambios en Usuarios y en la sesión común

Se cambia el contrato antes que el código (principio IV, ADR-015).

## `contratos/usuarios/openapi.yaml`

### `POST /api/usuarios/sesion` (cambia)

| Respuesta | Cuándo | Cuerpo |
|---|---|---|
| `200` | Credenciales correctas de un usuario activo, sin bloqueo | `TokenSesion` (sin cambios) |
| `400` | Datos vacíos (D-42) | `Error` `COMUN_SOLICITUD_INVALIDA` con `detalles` (sin cambios) |
| `401` | Usuario inexistente, inactivo o contraseña incorrecta | `Error` `USUARIOS_CREDENCIALES_INVALIDAS` (sin cambios) |
| **`429`** | **Nuevo.** El nombre está bloqueado por 5 intentos fallidos seguidos (D-60) | `Error` `USUARIOS_INICIO_BLOQUEADO`; encabezado `Retry-After` (segundos) |

### `POST /api/usuarios/sesion/renovacion` (nuevo)

`operationId: renovarSesion`. Requiere `Authorization: Bearer` con un token de sesión vigente.

| Respuesta | Cuándo | Cuerpo |
|---|---|---|
| `200` | Token vigente y `inicio` + 8 h sin pasar | `TokenSesion`: token nuevo, mismo `inicio`, `venceEn` = ahora + 30 min |
| `401` | Sin token, token vencido o inválido, o pasaron las 8 horas | `NoAutenticado` (`SEGURIDAD_NO_AUTENTICADO`) |

Sin cuerpo de solicitud. Un token de proceso (`roles: [PROCESO]`) recibe 401.

## `contratos/comun/seguridad.yaml`

`SesionActual` gana `nombre` (**opcional**, `string`, el nombre para mostrar del claim `nombre`). `required` sigue siendo `[usuario, roles]`, así que los «sesión actual» de los demás procesos (D-39) no cambian.

## `contratos/comun/errores.md`

| Código | Estado | Título | Descripción | Cuándo |
|---|---|---|---|---|
| `USUARIOS_INICIO_BLOQUEADO` | 429 | Demasiados intentos | Espera unos minutos y vuelve a intentarlo. | Inicio de sesión con un nombre de usuario que tuvo 5 intentos fallidos seguidos, exista o no; el bloqueo dura 15 minutos (D-60). |

Una prueba compara la fila con `ErroresUsuarios` (ADR-016).

## Configuración de `identidad`

| Propiedad | Variable | Por defecto |
|---|---|---|
| `mdd.usuarios.token.vigencia` | — | `PT30M` (sin cambios) |
| `mdd.usuarios.token.maximo` | — | `PT8H` |
| `mdd.usuarios.intentos.maximo` | — | `5` |
| `mdd.usuarios.intentos.bloqueo` | — | `PT15M` |
| `mdd.usuarios.demo.claves` | `MDD_USUARIOS_DEMO_CLAVES` | vacío (ruta del archivo `usuario=clave`) |
| `mdd.usuarios.demo.clave` | `MDD_USUARIOS_DEMO_CLAVE` | vacío (clave común de local y CI) |
