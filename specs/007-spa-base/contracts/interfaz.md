# Contrato: interfaz de la SPA base

Lo que garantiza la SPA en esta historia (escenarios 1 a 3; FR-001 a FR-013; D-50 y D-51). Las decisiones están en `research.md`.

## Rutas

| Ruta | Acceso | Muestra |
|---|---|---|
| `/iniciar-sesion` | Pública | Formulario de usuario y contraseña; con `?destino=/ruta` vuelve a esa ruta interna después de iniciar sesión |
| `/` | Con sesión | Inicio: «Hola, <usuario>» y la lista de sus roles |
| Cualquier otra | — | «Página no encontrada», con un enlace a Inicio |

| Situación | Comportamiento |
|---|---|
| Ruta interna sin sesión | Redirige a `/iniciar-sesion?destino=<ruta>` (D-50) |
| `destino` que no empieza con `/` o empieza con `//` | Se ignora y se va a `/` (sin redirección abierta) |
| 401 en cualquier petición con sesión | Cierra la sesión y va a `/iniciar-sesion` con «Tu sesión terminó. Inicia sesión de nuevo.» |
| Token vencido | Lo mismo, sin esperar un 401 |
| «Cerrar sesión» | Borra el token y la caché y va a `/iniciar-sesion` |
| Recarga de la página | Va a `/iniciar-sesion` (el token estaba solo en memoria, D-51) |

## Marco común

- Encabezado con «MDD · Distributivo docente», el usuario de la sesión y el botón «Cerrar sesión».
- Menú lateral con «Inicio». Cada historia agrega sus entradas en `src/navegacion/`.
- Cada pantalla tiene un único `h1`, una región `main` y su título en `document.title` («Inicio · MDD»).
- Interfaz en español (tema de MUI con `esES`, `<html lang="es">`), desde 1366 px de ancho.

## Errores

| Respuesta | Lo que ve el usuario |
|---|---|
| 401 de `USUARIOS_CREDENCIALES_INVALIDAS` en el inicio de sesión | «Credenciales inválidas — El usuario o la contraseña no son correctos.» |
| 400 con `detalles` | El título y la descripción, y cada detalle bajo su campo |
| 500 con `idIncidente` | El título y la descripción, e «Identificador del incidente: <id>» |
| 503 del API Gateway | «Servicio no disponible — El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.» |
| Respuesta sin el esquema `Error` | El título y la descripción de `COMUN_ERROR_INTERNO` |
| Sin conexión | El título y la descripción de `COMUN_SERVICIO_NO_DISPONIBLE` |

En ningún caso se muestran el `codigo`, el estado HTTP, trazas ni el texto de una excepción. El aviso tiene `role="alert"`.

## Accesibilidad

`e2e/accesibilidad.spec.ts` analiza `/iniciar-sesion` con axe-core en Chromium y las reglas `wcag2a`, `wcag2aa`, `wcag21a` y `wcag21aa`: en su estado inicial, con el error de credenciales inválidas y con los errores de campo vacío. `violations` debe estar vacío en los tres.

## Comandos de `frontend/package.json`

| Comando | Qué hace | Dónde corre |
|---|---|---|
| `generar:api` | openapi-typescript desde `../contratos/` a `src/api/generado/` | Antes de `tipos`, `build`, `test` y `dev`; en `1 · lint-frontend`, `2 · test-frontend` y la imagen |
| `lint` | `eslint . --max-warnings 0` | `1 · lint-frontend` |
| `formato:verificar` | `prettier --check .` | `1 · lint-frontend` |
| `tipos` | `tsc --noEmit` | `1 · lint-frontend` |
| `test` | Vitest (con `--run --coverage` en la CI; umbral de 60 % de líneas) | `2 · test-frontend` |
| `test:e2e` | Playwright en Chromium sobre `vite preview` | `2 · test-frontend` |
| `verificar:ruptura` | El escenario 1 con dos cambios de contrato (research §3) | `2 · test-frontend` |
| `sbom` | `cyclonedx-npm --omit dev --output-file sbom-frontend.json` | `4 · sbom-frontend` |
| `build`, `dev`, `preview` | Vite | Imagen y desarrollo local |
