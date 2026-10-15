# Evidencia: B00-H06

Registro de las comprobaciones de los cuatro escenarios de la historia (issue #23), de la SPA en el perfil consolidado (D-52) y de Node 24 (D-53). Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44, 12 núcleos, 31 GiB de RAM | El equipo de despliegue tiene 4 núcleos y 8 GiB (D-36). |
| Node y npm | 24.21.0 y 11.19.0 | Binario oficial en `~/.local/node-24`, verificado con `SHASUMS256.txt` (research §13). El Node del sistema es 20.20. |
| Docker Engine y Compose | 29.8.2 y 5.5.1 (Fedora) | Con `sg docker -c '…'` hasta volver a iniciar sesión. |

## Construcción y pruebas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Dependencias (T003) | confirmada | `npm install` con las versiones exactas de research §1: 607 paquetes y `npm audit --omit dev` → `found 0 vulnerabilities`. npm 11.19 bloquea los scripts de instalación que no se aprobaron: el único es `libxmljs2` (opcional en `@cyclonedx/cyclonedx-npm`, que solo valida XML; el SBOM es JSON), denegado en `allowScripts`. | 2026-10-15 |
| Configuración (T004) | confirmada | Tres proyectos de TypeScript (`tsconfig.app.json` para `src/`, `tsconfig.pruebas.json` para las pruebas con los tipos de Node, y `tsconfig.node.json` para la configuración, `e2e/` y `scripts/`), así los tipos de Node no llegan al código de la SPA. ESLint con tipos (`projectService`), jsx-a11y y la regla del almacenamiento (D-51); en los archivos JavaScript, sin reglas con tipos. | 2026-10-15 |
| Base de la SPA (T005 a T013) | confirmada | Vitest: 45 pruebas en 9 archivos (traductor, mensajes del catálogo, cliente, sesión, rutas protegidas, inicio de sesión, inicio, marco, página no encontrada y la aplicación). Cobertura de `src/`: 122/122 líneas (100 %), ramas 96,4 %. `tipos`, `lint` y `formato:verificar` en verde. **Hallazgo:** con un vencimiento a más de 24,8 días, `setTimeout` desborda (2³¹ − 1 ms) y cerraba la sesión de inmediato; el cierre ahora se programa en tramos, con su prueba. | 2026-10-15 |

## Escenario 1: cambio de contrato

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 2: errores

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 3: accesibilidad

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 4: ESLint

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Perfil y punta a punta

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Memoria (R-19)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- **PR #73 (D-53):** cerrado el 2026-10-15 con la aprobación del autor y un comentario que cita D-53.
