# Data Model: SPA base con el cliente generado (B00-H06)

La SPA no guarda datos de negocio ni datos persistentes: todo vive en la memoria de la pestaña (D-51). Estas son las estructuras del frontend; los detalles están en [research.md](research.md).

## Sesión (en memoria)

| Campo | Origen | Reglas |
|---|---|---|
| `token` | `TokenSesion.token` de `POST /api/usuarios/sesion` | Solo en memoria. Nunca en `localStorage`, `sessionStorage`, cookies ni en la URL (D-51). Va en `Authorization: Bearer` de cada petición a `/api` (ADR-014). |
| `venceEn` | `TokenSesion.venceEn` | Al llegar, la sesión se cierra sin esperar un 401. |
| `usuario`, `roles` | `SesionActual` de `GET /api/usuarios/sesion` | Caché de TanStack Query; se borra al cerrar la sesión. |

**Estados:**

```text
sin sesión ──(inicio de sesión correcto)──► con sesión
    ▲                                           │
    └──── cerrar sesión · 401 · vencimiento · recarga de la página
```

## Mensaje de error

Lo que la SPA muestra de cualquier fallo; lo arma solo `src/errores/traducir.ts`.

| Campo | Origen | Reglas |
|---|---|---|
| `titulo` | `Error.titulo`, o el genérico del catálogo | Siempre; en español. |
| `descripcion` | `Error.descripcion`, o el genérico del catálogo | Siempre; dice qué hacer (RNF-USA-01). |
| `detalles` | `Error.detalles` (`campo`, `descripcion`) | Opcional; se muestra junto al campo de la pantalla que se llama igual. |
| `idIncidente` | `Error.idIncidente` | Opcional; se muestra como «Identificador del incidente: …». |

Nunca incluye el `codigo`, el estado HTTP, trazas, nombres de clases ni el texto de una excepción.

| Situación | Título y descripción |
|---|---|
| Cuerpo con el esquema `Error` | Los del cuerpo |
| Cuerpo sin el esquema (HTML, vacío u otra forma) | Los de `COMUN_ERROR_INTERNO` en `contratos/comun/errores.md` |
| Fallo de red (sin respuesta) | Los de `COMUN_SERVICIO_NO_DISPONIBLE` |

## Formulario de inicio de sesión

| Campo | Reglas (Zod) | Mensaje |
|---|---|---|
| `usuario` | Obligatorio, sin espacios en los extremos | «Ingresa tu usuario.» |
| `clave` | Obligatoria | «Ingresa tu contraseña.» |

Los `detalles` de un 400 del backend (D-42) se muestran en el campo del mismo nombre.

## Cliente generado

`src/api/generado/<servicio>.d.ts`, generado por `npm run generar:api` desde `contratos/<servicio>/openapi.yaml`; Git lo ignora. En esta historia, solo `usuarios`: `paths['/api/usuarios/sesion']` (`post` y `get`), `components['schemas']['TokenSesion']`, `SesionActual`, `CredencialesSesion`, `Error` y `DetalleError`.
