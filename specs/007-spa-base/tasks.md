---

description: "Tareas de B00-H06: SPA base con el cliente generado"
---

# Tasks: SPA base con el cliente generado

**Input**: documentos de diseño en `specs/007-spa-base/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas. En cada grupo, las pruebas se escriben antes que el código que las hace pasar.

**Decisiones**: D-29, D-31, D-37, D-50, D-51, D-52, D-53, ADR-014, ADR-015, ADR-016 y ADR-019 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**: la base de la SPA (cliente, errores, sesión, rutas, pantallas y marco) es la fase fundacional, porque los cuatro escenarios la usan. Las historias de usuario son las de la especificación:
- **US1:** cambio de contrato;
- **US2:** errores en español;
- **US3:** accesibilidad;
- **US4:** ESLint en la CI.

La imagen, el perfil consolidado y Node (D-52, D-53) van en una fase transversal.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2, US3 o US4.
- Las rutas son relativas a la raíz del repositorio. Los comandos de npm corren en `frontend/` con Node 24 (T001). Docker, con `sg docker -c '…'` mientras haga falta.

## Convenciones para todas las tareas

- **Versiones:** exactas (sin `^` ni `~`), las de research §1. `package-lock.json` se versiona; `src/api/generado/`, `dist/`, `coverage/`, `test-results/` y `playwright-report/` se ignoran.
- **Español:** textos, mensajes y nombres de pantallas, componentes y funciones propias en español (`IniciarSesion`, `traducirError`, `useSesion`); en inglés solo lo que es convención de la herramienta (`main.tsx`, `App.tsx`, `vite.config.ts`).
- **Comentarios:** cada módulo de `src/` empieza con un comentario en español que dice su propósito y la decisión que aplica.
- **Sin dobles de librerías:** las pruebas de Vitest simulan la API con un `fetch` escrito a mano que responde según la ruta (`src/pruebas/apiSimulada.ts`); las de Playwright, con `page.route` (`e2e/simulacion.ts`). Sin MSW.
- **Token:** nunca en `localStorage`, `sessionStorage`, `document.cookie` ni en la URL (D-51); ESLint lo hace cumplir desde T004.
- **Herramientas en contenedor** (shellcheck, actionlint, Gitleaks y el CLI de openapi-generator): sobre una copia en el directorio temporal de la sesión, nunca montando el repositorio con `:z`.
- **Scripts de Bash:** con el patrón `verificar` de `deploy/probar_errores.sh`.
- **Formato:** `npm run formato` (Prettier) y `npm run lint` antes de cada commit. **Evidencia:** en `specs/007-spa-base/evidencia.md`.

---

## Phase 1: Setup

- [X] T001 Dejar Node 24 disponible sin `sudo` (research §13): el binario oficial de la última 24.x con más de dos semanas (comprobado con `SHASUMS256.txt` de nodejs.org) en `~/.local/node-24`, agregado al `PATH` solo en los comandos de esta historia. Si no se puede, usar `node:24-alpine` sobre una copia en el scratchpad. Anotar la versión en el «entorno» de la evidencia.
- [X] T002 [P] Crear `specs/007-spa-base/evidencia.md` con estas secciones:
  - entorno;
  - construcción y pruebas;
  - escenario 1 (cambio de contrato);
  - escenario 2 (errores);
  - escenario 3 (accesibilidad);
  - escenario 4 (ESLint);
  - perfil y punta a punta;
  - memoria (R-19);
  - notas.
- [X] T003 Crear `frontend/package.json` (`"private": true`, `"type": "module"`, `engines.node` `>=24 <25`) con las dependencias y versiones exactas de research §1, y estos scripts:
  - `generar:api`: openapi-typescript desde `../contratos/usuarios/openapi.yaml` a `src/api/generado/usuarios.d.ts`;
  - `pretipos`, `prebuild`, `pretest` y `predev`: `npm run generar:api`;
  - `lint`: `eslint . --max-warnings 0`;
  - `formato`: `prettier --write .`, y `formato:verificar`: `prettier --check .`;
  - `tipos`: `tsc --noEmit`;
  - `test`: `vitest`, y `test:e2e`: `playwright test`;
  - `verificar:ruptura`: `node scripts/verificar-ruptura.mjs`;
  - `sbom`: `cyclonedx-npm --omit dev --output-file sbom-frontend.json`;
  - `dev`, `build` (`tsc --noEmit && vite build`) y `preview`.

  Correr `npm install` para crear `package-lock.json`, y `npm audit --omit dev` sin altas ni críticas. Agregar `frontend/.nvmrc` (`24`) y `frontend/.gitignore`.
- [X] T004 Agregar la configuración de `frontend/`:
  - **`tsconfig.json`:** estricto, `noUncheckedIndexedAccess`, `jsx: react-jsx` y `moduleResolution: bundler`; más `tsconfig.node.json` para los archivos de configuración;
  - **`index.html`:** `<html lang="es">` y el título «MDD · Distributivo docente»;
  - **`vite.config.ts`:** el *plugin* de React y, en desarrollo, el proxy de `/api` a `http://localhost:8080`;
  - **`vitest.config.ts`:**
    - jsdom, `setupFiles` con `@testing-library/jest-dom`;
    - cobertura v8 de `src/**` (`coverage.include`), con `thresholds.lines: 60`;
    - fuera de la cobertura: `src/api/generado/**`, `src/main.tsx` y `e2e/**`;
  - **`playwright.config.ts`:**
    - dos proyectos con `viewport` de 1366 × 768: `chromium` (todas las pruebas) y `firefox` (`sesion.spec.ts` y `errores.spec.ts`; la accesibilidad, solo en Chromium). Edge usa el motor de Chromium (FR-009);
    - `webServer` con `npm run build && npm run preview -- --port 4173`;
    - `baseURL` `http://localhost:4173`;
  - **`eslint.config.js`:** según research §9, con `no-restricted-globals` y `no-restricted-properties` para `localStorage`, `sessionStorage` y `document.cookie`, y `no-console` en `src/`;
  - **`.prettierrc.json` y `.prettierignore`.**

  Correr `npm run generar:api`, `npm run tipos`, `npm run lint` y `npm run formato:verificar` sobre un `src/main.tsx` mínimo: todo en verde.

---

## Phase 2: Foundational (bloquea a todas las historias)

**Purpose**: la base de la SPA de ADR-019: cliente, traductor de errores, sesión en memoria, rutas protegidas, pantallas y marco (FR-004 a FR-012, D-50, D-51).

**⚠️ CRITICAL**: ninguna historia puede empezar sin esta fase.

### Cliente y errores

- [X] T005 [P] Escribir primero `frontend/src/errores/traducir.test.ts` (data-model «Mensaje de error»):
  - un cuerpo `Error` da su `titulo` y su `descripcion`, y nunca su `codigo`;
  - con `detalles`, los conserva por campo; con `idIncidente`, lo conserva;
  - un cuerpo HTML, vacío o con otra forma da el mensaje de `COMUN_ERROR_INTERNO`, sin `idIncidente`;
  - un `TypeError` de `fetch` da el de `COMUN_SERVICIO_NO_DISPONIBLE`;
  - ningún resultado contiene el estado HTTP, «Error:», «Exception», «at » ni el texto del `TypeError`.

  Y `frontend/src/errores/mensajes.test.ts`: los textos de `mensajes.ts` coinciden con las filas de `COMUN_ERROR_INTERNO` y `COMUN_SERVICIO_NO_DISPONIBLE` de `contratos/comun/errores.md`, leídas del archivo.
- [X] T006 Implementar en `frontend/src/errores/`:
  - `mensajes.ts`, con los dos textos genéricos del catálogo;
  - `traducir.ts`, con `traducirError(fallo): MensajeDeError` y el esquema Zod del tipo `Error` generado;
  - `AvisoDeError.tsx`, una `Alert` de MUI con `role="alert"`, el título, la descripción e «Identificador del incidente: …» si viene.

  Así pasa T005.
- [X] T007 [P] Escribir primero `frontend/src/api/cliente.test.ts`, con `apiSimulada`:
  - con un token, la petición lleva `Authorization: Bearer <token>`; sin token, no lleva la cabecera;
  - un 401 llama al cierre de sesión registrado;
  - las rutas y los cuerpos usan los tipos generados (una prueba de tipos con `expectTypeOf` sobre `paths['/api/usuarios/sesion']`).
- [X] T008 Implementar `frontend/src/api/cliente.ts`: `createClient<paths>({ baseUrl: '/' })` de openapi-fetch, con un *middleware* que lee el token de la sesión en memoria (§5) y que, ante un 401, llama a `alCerrarSesion`. Así pasa T007.

### Sesión y rutas

- [X] T009 [P] Escribir primero `frontend/src/sesion/sesion.test.tsx`:
  - `iniciar(token, venceEn)` deja la sesión activa y `cerrar()` la borra, junto con la caché de TanStack Query;
  - el vencimiento cierra la sesión (con temporizadores falsos de Vitest);
  - después de iniciar sesión, `localStorage`, `sessionStorage` y `document.cookie` siguen vacíos;
  - `RutaProtegida` sin sesión redirige a `/iniciar-sesion?destino=<ruta>`, y con sesión muestra la ruta;
  - `destinoSeguro` acepta `/inicio` y rechaza `https://otro`, `//otro` y `javascript:` (vuelve `/`).
- [X] T010 Implementar en `frontend/src/sesion/`:
  - `SesionProvider.tsx` y `useSesion`, con el token y `venceEn` en memoria y el temporizador de vencimiento;
  - `RutaProtegida.tsx`;
  - `destinoSeguro.ts`.

  Registrar el cierre de sesión en el cliente (T008). Así pasa T009.

### Pantallas y marco

- [X] T011 [P] Escribir primero, con `apiSimulada` y `MemoryRouter`:
  - `frontend/src/pantallas/IniciarSesion.test.tsx`:
    - los campos tienen etiqueta;
    - vacíos, muestran «Ingresa tu usuario.» e «Ingresa tu contraseña.» sin llamar a la API;
    - con credenciales correctas, guarda el token y navega al `destino` o a `/`;
    - con un 401 `USUARIOS_CREDENCIALES_INVALIDAS`, muestra «Credenciales inválidas» y su descripción, sin el código;
    - con un 400 con `detalles`, muestra cada detalle bajo su campo;
    - con `?motivo=sesion-terminada`, muestra «Tu sesión terminó. Inicia sesión de nuevo.»;
  - `frontend/src/pantallas/Inicio.test.tsx`: con sesión, muestra «Hola, autor.prueba» y los roles de `GET /api/usuarios/sesion`; con un 500, muestra el aviso con el `idIncidente`;
  - `frontend/src/navegacion/Marco.test.tsx`: tiene el encabezado con el usuario, el menú con «Inicio» y «Cerrar sesión», que vuelve al inicio de sesión;
  - `frontend/src/pantallas/NoEncontrada.test.tsx`: tiene el título y el enlace a Inicio;
  - `frontend/src/App.test.tsx`: `/` sin sesión termina en el inicio de sesión; un 401 de la sesión actual cierra la sesión y lleva al inicio de sesión con el aviso.
- [X] T012 Implementar:
  - `frontend/src/tema.ts`: MUI con `esES` y Roboto de `@fontsource/roboto`;
  - `frontend/src/pantallas/IniciarSesion.tsx`: React Hook Form con Zod (data-model «Formulario»), `AvisoDeError` y `setError` para los `detalles`;
  - `frontend/src/pantallas/Inicio.tsx`: TanStack Query sobre la sesión actual;
  - `frontend/src/pantallas/NoEncontrada.tsx`;
  - `frontend/src/navegacion/Marco.tsx`: `AppBar`, menú lateral con `NavLink` y «Cerrar sesión»;
  - `frontend/src/rutas.tsx`: `createBrowserRouter` con `/iniciar-sesion`, `/` protegida dentro del `Marco`, y `*`;
  - `frontend/src/App.tsx` y `frontend/src/main.tsx`: proveedores de tema, TanStack Query y sesión.

  Cada pantalla con un `h1`, una región `main` y su `document.title`. Así pasa T011.
- [X] T013 Correr `npm run lint`, `npm run formato:verificar`, `npm run tipos` y `npm run test -- --run --coverage`: todo en verde, con al menos el 60 % de líneas. Anotar la cobertura en la evidencia.

**Checkpoint**: la SPA inicia sesión contra la API simulada, protege sus rutas y muestra los errores en español.

---

## Phase 3: User Story 1 - Un cambio de contrato rompe la compilación del frontend (Priority: P1) 🎯 MVP

**Goal**: un cambio de contrato en un campo que la SPA usa hace fallar la compilación, y la CI lo comprueba en cada pull request (escenario 1; FR-001 a FR-003; SC-001).

**Independent Test**: `npm run verificar:ruptura` en verde.

- [X] T014 [US1] Escribir `frontend/scripts/verificar-ruptura.mjs` (research §3):
  1. respalda `src/api/generado/usuarios.d.ts`;
  2. copia `contratos/` a una carpeta temporal del sistema;
  3. **primer cambio:** renombra `token` por `jwt` en `TokenSesion`, genera con openapi-typescript sobre `usuarios.d.ts`, corre `tsc --noEmit` y exige que falle con un error que nombre `token`;
  4. **segundo cambio:** cambia `venceEn` a `type: integer` y exige otra vez el fallo;
  5. **campo que la SPA no usa:** renombra `kty` en `Jwk` (la SPA no lee el JWKS) y exige que `tsc` **pase** (caso límite de la especificación);
  6. restaura los tipos del contrato real y exige que `tsc` pase, también si un paso intermedio falla (`finally`).

  Imprime una línea por paso (`OK`/`FALLA`) y termina con 0 o 1. Para que el primer cambio rompa la compilación, la SPA tiene que leer `token` y `venceEn` con sus tipos (T012).
- [X] T015 [US1] En `.github/workflows/ci.yml`, job `2 · test-frontend`, agregar el paso «Un cambio de contrato rompe la compilación» (`npm run verificar:ruptura`) después de Vitest. Validar con actionlint, sobre una copia. Anotar la salida local en el escenario 1 de la evidencia.

**Checkpoint**: un cambio de contrato no pasa desapercibido.

---

## Phase 4: User Story 2 - El usuario ve los errores en español y sin detalles técnicos (Priority: P1)

**Goal**: todas las respuestas de error se muestran con el `titulo` y la `descripcion` en español y sin texto técnico, en la SPA construida (escenario 2; FR-010 a FR-012; SC-002).

**Independent Test**: `npx playwright test e2e/errores.spec.ts` en verde.

- [X] T016 [P] [US2] Escribir `frontend/e2e/simulacion.ts`, con `page.route` para `/api/usuarios/sesion`: respuesta correcta, 401 de credenciales, 400 con `detalles`, 500 con `idIncidente`, 503 del API Gateway, HTML de un proxy y fallo de red (`route.abort()`); y para `GET /api/usuarios/sesion`, un 404 `COMUN_RECURSO_NO_ENCONTRADO` (SC-002).
- [X] T017 [US2] Escribir `frontend/e2e/errores.spec.ts`: por cada respuesta de T016, el inicio de sesión muestra el título y la descripción esperados (contracts/interfaz.md «Errores»). La página no contiene el estado (`401`, `500`…), `COMUN_`, `USUARIOS_`, «Exception», «Error:» ni texto en inglés del navegador. El 500 muestra el `idIncidente`, y el 404 de la sesión actual se muestra en Inicio con su título y su descripción. Si encuentra un hueco en el traductor, corregirlo en `src/errores/` con su prueba unitaria.

**Checkpoint**: ningún error llega al usuario con texto técnico.

---

## Phase 5: User Story 3 - La pantalla de inicio de sesión es accesible (Priority: P1)

**Goal**: la pantalla de inicio de sesión no tiene fallas de WCAG 2.1 AA según axe-core, en un navegador real (escenario 3; FR-013; SC-003).

**Independent Test**: `npx playwright test e2e/accesibilidad.spec.ts` en verde.

- [X] T018 [US3] Escribir `frontend/e2e/accesibilidad.spec.ts` con `@axe-core/playwright` y `withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])` sobre `/iniciar-sesion`, en tres estados:
  - inicial;
  - con el error de credenciales inválidas;
  - con los dos errores de campo vacío.

  Exigir `violations` vacío y, si falla, imprimir el `id`, el impacto y el nodo de cada falla.
- [X] T019 [US3] Corregir lo que T018 encuentre (contraste del tema, etiquetas, foco, el `role="alert"`) en `src/tema.ts` o en las pantallas, sin desactivar reglas. Escribir `frontend/e2e/sesion.spec.ts` con los flujos de research §8:
  - redirección y vuelta al destino;
  - el token fuera del almacenamiento del navegador (SC-008);
  - la recarga vuelve al inicio de sesión;
  - un 401 cierra la sesión;
  - el ancho de 1366 px sin desplazamiento horizontal.
- [X] T020 [US3] En `.github/workflows/ci.yml`, job `2 · test-frontend`, agregar antes de `verificar:ruptura` los pasos `npx playwright install --with-deps chromium firefox` y «Accesibilidad y flujos (Playwright)» (`npm run test:e2e`), y subir `frontend/playwright-report/` como artefacto `if: failure()`. Validar con actionlint, sobre una copia. Anotar en la evidencia el resultado de los tres análisis.

**Checkpoint**: la base cumple WCAG 2.1 AA y los flujos de sesión están probados en un navegador.

---

## Phase 6: User Story 4 - ESLint bloquea la fusión de un pull request con errores (Priority: P2)

**Goal**: el check `1 · lint-frontend` corre ESLint y un error bloquea la fusión (escenario 4; FR-014 a FR-016; SC-004).

**Independent Test**: en el pull request, un commit con un error de ESLint deja `1 · lint-frontend` en rojo y la fusión bloqueada; su reversión lo deja en verde.

- [X] T021 [US4] Comprobar en local que un error de ESLint (por ejemplo, `localStorage.setItem` en `src/`) hace fallar `npm run lint` con el mensaje de la regla, y que se revierte. Comprobar que `ci.yml` (job `1 · lint-frontend`) ya corre `generar:api`, `lint`, `formato:verificar` y `tipos`, y que `gh api repos/{owner}/{repo}/branches/main/protection` exige `1 · lint-frontend`. Si algo falta, ajustarlo sin renombrar los checks (D-31).
- [ ] T022 [US4] En el pull request (después de T029), con la aprobación del autor, empujar un commit que agrega un error de ESLint en `frontend/src/`. Comprobar que `1 · lint-frontend` falla y que `gh pr view` muestra la fusión bloqueada (`mergeStateStatus` `BLOCKED`). Después, revertirlo con otro commit y comprobar que el check vuelve a verde. Anotar los dos runs en el escenario 4 de la evidencia.

**Checkpoint**: el frontend tiene el mismo control de calidad que el backend.

---

## Phase 7: Transversal - imagen, perfil consolidado y Node (D-52, D-53)

**Purpose**: la SPA en el perfil consolidado detrás de Nginx, medida en R-19, y Node 24 LTS en todos lados (FR-017 y FR-018; SC-005, SC-009).

- [ ] T023 [P] Escribir `frontend/nginx.conf` como plantilla (`/etc/nginx/templates/default.conf.template`, con `MDD_URI_GATEWAY`), según contracts/perfil.md:
  - `/api/` al API Gateway, con un plazo de 30 s y las cabeceras `X-Forwarded-*`;
  - `/salud`;
  - `/assets/` con caché larga e `index.html` sin caché;
  - `try_files $uri /index.html`;
  - la CSP y las cabeceras de seguridad;
  - `server_tokens off`.
- [ ] T024 Cambiar `deploy/docker/frontend.Dockerfile` a contexto en la raíz:
  - `FROM node:24-alpine` (sin cambiar de versión mayor, D-53);
  - `WORKDIR /app/frontend`: copia `frontend/package*.json` y `npm ci`; después `frontend/` en `/app/frontend` y `contratos/` en `/app/contratos`, para que `generar:api` encuentre `../contratos/`, y `npm run build`;
  - `nginxinc/nginx-unprivileged:stable-alpine` con `dist/` y la plantilla;
  - comentario de cabecera con el ejemplo de `docker build -f deploy/docker/frontend.Dockerfile .`.

  En el `.dockerignore` de la raíz, que hoy solo admite `backend/` y `contratos/`, agregar `!frontend/` y excluir `frontend/node_modules/`, `frontend/dist/`, `frontend/coverage/`, `frontend/test-results/` y `frontend/playwright-report/`; actualizar su comentario (sirve a las dos imágenes). Construirla en local y comprobar con `docker run` que `/salud` responde 200 y `/` trae la CSP.
- [ ] T025 En `compose.yaml`, agregar el servicio `frontend` de contracts/perfil.md:
  - `build`, `8081:8080`, 64 MiB;
  - `depends_on: gateway: condition: service_healthy`;
  - *healthcheck* con `wget`, con `interval` y `retries` como los demás;
  - `MDD_URI_GATEWAY` desde `.env.ejemplo` (`http://gateway:8080`).

  Actualizar el comentario de cabecera: 7 contenedores y 3 008 MiB de límites (R-19, D-36).
- [ ] T026 [P] En `deploy/medir_memoria.sh`, agregar `frontend` a `ORDEN` (no a `JAVA`), y actualizar `specs/002-compose-cuatro-procesos/contracts/medicion-r19.md` con el contenedor nuevo. Validar con shellcheck, sobre una copia.
- [ ] T027 Crear `deploy/probar_spa.sh`, ejecutable, con los 5 pasos de contracts/perfil.md y el patrón `verificar`, sin imprimir el token. Validar con shellcheck, sobre una copia. En `.github/workflows/perfil-consolidado.yml`, agregar el paso «SPA de punta a punta» después de «Eventos de punta a punta» y antes de la medición; agregar `frontend/**` a sus `paths`. Validar con actionlint, sobre una copia.
- [ ] T028 Node (D-53):
  - en `.github/dependabot.yml`, ecosistema `docker`, agregar `ignore: [{ dependency-name: node, update-types: [version-update:semver-major] }]` con su comentario;
  - el PR #73 ya está cerrado: se cerró el 2026-10-15, con la aprobación del autor y un comentario que cita D-53;
  - anotarlo en las notas de la evidencia.
- [ ] T029 Correr todo:
  - `npm run lint`, `formato:verificar`, `tipos`, `test -- --run --coverage`, `test:e2e`, `verificar:ruptura` y `npm audit --omit dev`;
  - `./gradlew build` sin cambios en el backend;
  - `docker compose down -v && docker compose up -d --build --wait`;
  - `deploy/probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh` y `probar_spa.sh`, todo en `OK`;
  - `deploy/medir_memoria.sh --con-sesion` con el contenedor `frontend`;
  - la revisión manual del quickstart §3 en un navegador (SC-005).

  Anotar las salidas en la evidencia.

---

## Phase 8: Polish & cierre

- [ ] T030 Revisar los comentarios de propósito de `src/`, `frontend/README.md` (cómo generar, probar y agregar una pantalla, ADR-019) y `backend/plantillas/README.md` (una línea que remita a `frontend/README.md` para las pantallas). Comprobar con `git status` que no hay archivos temporales. Correr Gitleaks local sobre un clon: `no leaks found`.
- [ ] T031 Hacer commit y push, y abrir el pull request de `007-spa-base` hacia `main` **en borrador**, con `Refs #23`. Esperar a que terminen los runs del borrador y recién entonces marcarlo como listo. Comprobar que corren y pasan, por primera vez con frontend:
  - `1 · lint-frontend`, `2 · test-frontend` y `4 · sbom-frontend`;
  - `4 · sca`. Si Grype marca una dependencia del frontend con versión corregida, actualizarla y anotarlo;
  - `perfil-consolidado`. Pegar en la evidencia la medición de R-19 del run.

  Después, T022.
- [ ] T032 Completar `specs/007-spa-base/evidencia.md` y la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**. Después:
  - ejecutar `perfil-consolidado` sobre `main` y abrir el pull request de evidencia con `Closes #23`, como en B00-H05;
  - borrar las ramas fusionadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: T001 antes que todo lo de npm; T003 antes que T004.
- **Foundational (fase 2)**:
  - T005 va antes que T006, T007 antes que T008, T009 antes que T010 y T011 antes que T012;
  - T010 depende de T008, y T012 de T006 y T010;
  - T013 cierra la fase.
- **US1 (fase 3)**: depende de la fase 2. T014 va antes que T015.
- **US2 (fase 4)**: depende de la fase 2. T016 va antes que T017.
- **US3 (fase 5)**: depende de la fase 2 y de T016 (la simulación). T018 va antes que T019, y T020 depende de los dos.
- **US4 (fase 6)**: T021 depende de T004; T022 depende de T031.
- **Transversal (fase 7)**:
  - T023 y T026 son independientes;
  - T024 depende de T023 y de la fase 2, y T025 de T024;
  - T027 depende de T025;
  - T028 es independiente;
  - T029 cierra la fase.
- **Cierre (fase 8)**: T030 y T031 dependen de todo lo anterior; T032, de T022.

### Parallel Opportunities

- Fase 1: T002.
- Fase 2: T005, T007, T009 y T011 (pruebas de archivos distintos).
- US1, US2 y US3 en cualquier orden después de la fase 2.
- Fase 7: T023, T026 y T028.
- Como trabajas solo (límite de 1 en «Implementar»), [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. Fases 1 y 2: la base de la SPA con sus pruebas.
2. Fase 3: `verificar:ruptura`.
3. **Validar:** `npm run test -- --run --coverage` y `npm run verificar:ruptura`.

### Incremental

1. **US2:** los errores en la SPA construida.
2. **US3:** axe-core y los flujos de sesión en Chromium.
3. **Transversal:** imagen, perfil, R-19 y Node.
4. **Cierre:** pull request (con la demostración del escenario 4), evidencia y fusión.

---

## Notes

- La SPA no tiene contratos nuevos: si una tarea necesita un campo que el contrato no tiene, se cambia primero el contrato (principio IV) y se anota.
- El volumen local de PostgreSQL ya tiene los esquemas de B00-H05; esta historia no cambia la base.
- Hacer commit por tarea o por grupo lógico en `007-spa-base`, con `Refs #23`.
