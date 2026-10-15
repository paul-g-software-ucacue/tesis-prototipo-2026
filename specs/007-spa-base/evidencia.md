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
| Corrida completa del frontend (T029) | confirmada | `lint`, `formato:verificar` y `tipos` en verde; Vitest 46 pruebas, 100 % de líneas; Playwright 33 pruebas (Chromium y Firefox); `verificar:ruptura` con los 4 pasos en `OK`; `npm audit --omit dev`: 0 vulnerabilidades. `./gradlew build` del backend, sin cambios, en verde. | 2026-10-15 |
| Herramientas (T026, T027, T030) | confirmada | ShellCheck sin hallazgos en `deploy/probar_spa.sh` y `deploy/medir_memoria.sh`; actionlint sin hallazgos en los workflows; Gitleaks v8.30.1 sobre un clon con los cambios: `no leaks found`. Todo sobre copias en el scratchpad. | 2026-10-15 |
| Dependencias (T003) | confirmada | `npm install` con las versiones exactas de research §1: 607 paquetes y `npm audit --omit dev` → `found 0 vulnerabilities`. npm 11.19 bloquea los scripts de instalación que no se aprobaron: el único es `libxmljs2` (opcional en `@cyclonedx/cyclonedx-npm`, que solo valida XML; el SBOM es JSON), denegado en `allowScripts`. | 2026-10-15 |
| Configuración (T004) | confirmada | Tres proyectos de TypeScript (`tsconfig.app.json` para `src/`, `tsconfig.pruebas.json` para las pruebas con los tipos de Node, y `tsconfig.node.json` para la configuración, `e2e/` y `scripts/`), así los tipos de Node no llegan al código de la SPA. ESLint con tipos (`projectService`), jsx-a11y y la regla del almacenamiento (D-51); en los archivos JavaScript, sin reglas con tipos. | 2026-10-15 |
| Base de la SPA (T005 a T013) | confirmada | Vitest: 45 pruebas en 9 archivos (traductor, mensajes del catálogo, cliente, sesión, rutas protegidas, inicio de sesión, inicio, marco, página no encontrada y la aplicación). Cobertura de `src/`: 122/122 líneas (100 %), ramas 96,4 %. `tipos`, `lint` y `formato:verificar` en verde. **Hallazgo:** con un vencimiento a más de 24,8 días, `setTimeout` desborda (2³¹ − 1 ms) y cerraba la sesión de inmediato; el cierre ahora se programa en tramos, con su prueba. | 2026-10-15 |

## Escenario 1: cambio de contrato

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `verificar:ruptura` (T014) | confirmada | `OK 1. Renombrar TokenSesion.token rompe la compilación` · `OK 2. Cambiar TokenSesion.venceEn a entero rompe la compilación` · `OK 3. Renombrar Jwk.kty, que la SPA no usa, no rompe la compilación` · `OK 4. Con el contrato real, la compilación pasa`. Trabaja sobre una copia de `contratos/` en una carpeta temporal; `git status contratos` queda limpio. **Hallazgo:** `new Date(token.venceEn)` también acepta un número, así que el segundo cambio no rompía nada; la SPA ahora lee `venceEn` con `Date.parse`, que solo acepta texto. | 2026-10-15 |
| En la CI (T015) | confirmada | Paso «Un cambio de contrato rompe la compilación» en `2 · test-frontend`, en verde en el run del borrador (38696340950) y en el del PR listo ([ci, run 38697471112](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38697471112)). | 2026-10-15 |

## Escenario 2: errores

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Playwright (T016, T017) | confirmada | `e2e/errores.spec.ts`, en Chromium y Firefox: 401 de credenciales, 400 con `detalles`, 500 con `idIncidente`, 503, HTML de un proxy y sin conexión al iniciar sesión, y 404 de la sesión actual en Inicio. Cada uno muestra su título y su descripción; ninguna página contiene un estado 4xx o 5xx, `COMUN_`, `USUARIOS_`, `SEGURIDAD_`, «Exception», «Error:», «Failed to fetch», «NetworkError» ni «Bad Gateway». | 2026-10-15 |

## Escenario 3: accesibilidad

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| axe-core en Chromium (T018) | confirmada | `e2e/accesibilidad.spec.ts` con `wcag2a`, `wcag2aa`, `wcag21a` y `wcag21aa`: 0 fallas en `/iniciar-sesion` en su estado inicial, con el error de credenciales inválidas y con los dos errores de campo vacío. T019 no tuvo nada que corregir en el tema ni en las pantallas. | 2026-10-15 |
| Flujos de sesión (T019) | confirmada | `e2e/sesion.spec.ts`, en Chromium y Firefox: redirección con `destino=%2F` y vuelta a Inicio; después de iniciar sesión, `localStorage`, `sessionStorage`, `document.cookie` y las cookies del contexto vacíos, y el token fuera de la URL (SC-008); la recarga vuelve al inicio de sesión; un 401 cierra la sesión con el aviso; «Cerrar sesión» vuelve sin aviso; a 1366 px no hay desplazamiento horizontal. Playwright: 33 pruebas en verde. **Hallazgos:** (1) al cerrar la sesión, `RutaProtegida` redirigía con `destino` antes de que llegara la navegación del botón, porque React Router 8 navega en una transición; el cierre voluntario tiene ahora su motivo (`cierre`) y la ruta protegida va al inicio de sesión sin destino ni aviso. (2) `@axe-core/playwright` instalaba `playwright-core` 1.64.1 (un día de antigüedad) para su dependencia *peer*, con tipos distintos de los de `@playwright/test`; `overrides` fija la 1.63.0. | 2026-10-15 |
| En la CI (T020) | confirmada | `test-frontend` instala Chromium y Firefox con sus dependencias y corre `test:e2e` (33 pruebas); sube `playwright-report/` si falla. Su `timeout-minutes` pasa de 10 a 20. En verde en los runs 38696340950 y 38697471112. | 2026-10-15 |

## Escenario 4: ESLint

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| En el pull request (T022) | confirmada | Con la aprobación del autor, el commit `7ca3220` agregó `localStorage.setItem` en `frontend/src/demostracionLint.ts`: [ci, run 38722830172](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38722830172) → `1 · lint-frontend: failure` con `Unexpected use of 'localStorage'. El token vive solo en memoria (D-51)…`, `2 · test-frontend: skipped`, y `gh pr view 93 --json mergeStateStatus` → `BLOCKED`. El commit `1c6ba76` lo revirtió: [ci, run 38724063846](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38724063846) → los 10 checks en verde, también `1 · lint-frontend` y `2 · test-frontend`, y `mergeStateStatus` → `CLEAN`. El commit `7ca3220` se hizo con `--no-verify` (sin el *hook* de pre-commit, que no hacía falta); Gitleaks lo revisó igual en `3 · secretos`. | 2026-10-15 |
| En local y en la regla (T021) | confirmada | Un `localStorage.setItem` agregado a `src/pantallas/Inicio.tsx` hace fallar `npm run lint` con `Unexpected use of 'localStorage'. El token vive solo en memoria (D-51)…`; revertido, pasa. `1 · lint-frontend` corre `generar:api`, `lint`, `formato:verificar` y `tipos`. La regla de la rama principal es un *ruleset* (`gh api repos/…/rules/branches/main`) y exige los 10 checks, también `1 · lint-frontend`. | 2026-10-15 |

## Perfil y punta a punta

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| CI del pull request (T031) | confirmada | [ci, run 38697471112](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38697471112) sobre `ed43eee`: los 10 checks en verde, por primera vez con los de frontend: `1 · lint-frontend`, `2 · test-frontend`, `4 · sbom-frontend` (`cyclonedx-npm --omit dev`) y `4 · sca` con los dos SBOM; Grype sin hallazgos en las dependencias del frontend. [perfil-consolidado, run 38697471266](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38697471266): autenticación, errores, eventos y SPA de punta a punta, todos en `OK`. | 2026-10-15 |
| Imagen de la SPA (T023, T024) | confirmada | `docker build -f deploy/docker/frontend.Dockerfile .` desde la raíz (el `.dockerignore` admite `frontend/` sin `node_modules` ni salidas). En un contenedor solo: `/salud` 200; `/` y una ruta de la SPA responden `index.html` con `no-cache`; `/assets/*.js` con `public, max-age=31536000, immutable`; la CSP, `nosniff` y `Server: nginx` sin la versión. **Hallazgo:** con `proxy_pass http://gateway:8080`, Nginx no arrancaba si no podía resolver `gateway` (`host not found in upstream`); ahora resuelve en cada petición con el DNS del contenedor (`NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1`, `resolver` y `proxy_pass` con variable): arranca igual y, sin API Gateway, `/api` responde 502. | 2026-10-15 |
| Perfil con la SPA (T025, T029) | confirmada | `docker compose down -v`, `build` y `up -d --wait`: los 7 contenedores `healthy`, también `frontend`. `probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh` y `probar_spa.sh`: todos los pasos en `OK`. `probar_spa.sh`: la SPA con la CSP y sin la versión de Nginx, una ruta de la SPA, el inicio de sesión a través de la SPA (200 con token), la sesión actual con el token (`autor.prueba` y sus roles) y sin token (401 `SEGURIDAD_NO_AUTENTICADO`). | 2026-10-15 |
| Recorrido real (SC-005) | confirmada | Chromium sin interfaz contra `http://localhost:8081`, a 1366 × 768: `/` → inicio de sesión → `autor.prueba` → «Hola, autor.prueba» con `COORDINADOR_CARRERA` y `DOCENTE` → «Cerrar sesión», en 0,6 s; `localStorage` y `sessionStorage` vacíos y sin cookies. Con una clave incorrecta: «Credenciales inválidas — El usuario o la contraseña no son correctos.». Queda la revisión manual del autor en su navegador (quickstart §3). | 2026-10-15 |

## Memoria (R-19)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Medición en el pull request (T031, FR-018, SC-009) | confirmada | [perfil-consolidado, run 38697471266](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38697471266): postgres 109,0; identidad 323,6; datos 180,9; distributivo 194,0; salida 191,4; gateway 200,5; **frontend 4,1 de 64**. **Total: 1 203,5 MiB (1,18 GiB)** de 3 008 MiB de límites, sin alerta. Con B00-H05 sobre `main` fueron 1 142,5 MiB: la SPA suma unos pocos MiB. | 2026-10-15 |
| Medición local (T029) | confirmada | `deploy/medir_memoria.sh --con-sesion`, local (Docker 29.8.2), después de las cuatro pruebas de punta a punta: postgres 102,8; identidad 360,2; datos 214,0; distributivo 222,4; salida 213,7; gateway 199,2; **frontend 11,6 de 64**. **Total: 1 323,9 MiB (1,29 GiB)** de 3 008 MiB de límites, sin alerta. | 2026-10-15 |

## Notas

- **PR #73 (D-53, T028):** cerrado el 2026-10-15 con la aprobación del autor y un comentario que cita D-53. `.github/dependabot.yml` ignora las versiones mayores de `node` en el ecosistema `docker`.
