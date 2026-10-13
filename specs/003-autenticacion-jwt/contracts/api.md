# Contrato: endpoints y archivos de `contratos/`

Resume los 6 endpoints de esta historia. El contrato normativo son los archivos OpenAPI 3.0.3 de `contratos/`, que se escriben y revisan antes de implementar (principio IV; ADR-015). Este documento dice qué tienen que contener.

## Archivos

| Archivo | Contenido |
|---|---|
| `contratos/comun/errores.yaml` | `components.schemas.Error` (`titulo`, `descripcion` y `codigo` obligatorios; `detalles` e `idIncidente` opcionales) y `DetalleError` (`campo` y `descripcion`), según el principio XIII |
| `contratos/comun/errores.md` | Catálogo de códigos: `SEGURIDAD_NO_AUTENTICADO` y `USUARIOS_CREDENCIALES_INVALIDAS`, con estado, título, descripción y cuándo se usan (D-38) |
| `contratos/comun/seguridad.yaml` | `securitySchemes.bearer` (HTTP bearer, JWT); el esquema `SesionActual` (`usuario`, y `roles` como lista de textos); la respuesta `NoAutenticado` (401, con `Error` y el encabezado `WWW-Authenticate`) |
| `contratos/usuarios/openapi.yaml` | Inicio de sesión, sesión actual y JWKS |
| `contratos/cargas/openapi.yaml`, `contratos/generacion/openapi.yaml` y `contratos/dashboard/openapi.yaml` | Sesión actual |

Los contratos de servicio declaran `security: [bearer: []]` de forma global y lo anulan (`security: []`) en las operaciones sin token.

## Endpoints

| Método y ruta | Token | Solicitud | Respuestas |
|---|---|---|---|
| `POST /api/usuarios/sesion` | No | `CredencialesSesion`: `usuario` y `clave`, textos | `200 TokenSesion` (`token`, `tipo: "Bearer"` y `venceEn` en fecha y hora ISO 8601) · `401 Error` con `USUARIOS_CREDENCIALES_INVALIDAS` |
| `GET /api/usuarios/sesion` | Sí | — | `200 SesionActual` · `401 NoAutenticado` |
| `GET /api/usuarios/jwks` | No (lo piden el API Gateway y los procesos) | — | `200 Jwks` (`keys`: lista de JWK públicas con `kty`, `kid`, `use`, `alg`, `n` y `e`) |
| `GET /api/cargas/sesion` | Sí | — | `200 SesionActual` · `401 NoAutenticado` |
| `GET /api/generacion/sesion` | Sí | — | `200 SesionActual` · `401 NoAutenticado` |
| `GET /api/dashboard/sesion` | Sí | — | `200 SesionActual` · `401 NoAutenticado` |

- **Inicio de sesión:** datos vacíos, usuario inexistente, usuario inactivo y contraseña incorrecta dan el mismo 401 (research §5). Los errores de entrada con `detalles` llegan con B00-H04.
- **`operationId`:** `iniciarSesion`, `consultarSesion` y `consultarJwks`. Con `useTags`, cada servicio genera una interfaz `SesionApi`, más `JwksApi` en Usuarios.

## Pruebas de contrato

Por cada servicio con contrato: una prueba con MockMvc que hace cada petición y valida la respuesta (200 y 401) contra su `openapi.yaml` con el validador de `plataforma/seguridad`. Una respuesta que no cumple el contrato hace fallar la prueba.

## Garantías

1. Cada endpoint tiene su operación en el contrato antes de existir el controlador, y el controlador implementa la interfaz generada: si no cumple el contrato, no compila (principio IV).
2. Las respuestas 200 y 401 de cada endpoint pasan la validación contra el contrato.
3. Cada servicio genera sus propios modelos (`SesionActual`, `Error`): ningún servicio usa las clases de otro (principio V).
