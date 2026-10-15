# Implementation Plan: SPA base con el cliente generado

**Branch**: `007-spa-base` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-spa-base/spec.md` (issue #23, B00-H06)

## Summary

`frontend/` nace con la base común de la SPA:
- **Stack:** React 19 con TypeScript 5.9 y Vite 8, React Router 8, TanStack Query, React Hook Form con Zod y MUI 9 en español.
- **Cliente generado:** los tipos salen de `contratos/` con openapi-typescript y el cliente es openapi-fetch. Un cambio de contrato que toca un campo usado rompe `tsc`, y `npm run verificar:ruptura` lo prueba en cada pull request.
- **Errores:** un traductor único convierte cualquier fallo en `titulo` y `descripcion` en español, con `detalles` por campo e `idIncidente`, y nunca muestra el `codigo` ni texto técnico.
- **Sesión:** el token vive solo en memoria (D-51). Las rutas internas están protegidas, con redirección y vuelta al destino (D-50). Un 401 o el vencimiento cierran la sesión.
- **Pantallas:** inicio de sesión, inicio con el usuario y sus roles, marco con menú y «Cerrar sesión», y «no encontrada».
- **Pruebas:**
  - Vitest con Testing Library, con el 60 % de líneas;
  - Playwright con axe-core en Chromium para WCAG 2.1 AA (también el contraste) y para los flujos con la API simulada;
  - ESLint con jsx-a11y y una regla que prohíbe guardar el token en el navegador.
- **Despliegue:** la imagen de Nginx (contexto en la raíz, con CSP) entra a `compose.yaml` en el puerto 8081 y envía `/api` al API Gateway (D-52). `deploy/probar_spa.sh` se suma a `perfil-consolidado`, y R-19 mide el contenedor nuevo.
- **Node:** se queda Node 24 LTS y se cierra el PR #73 (D-53).

Los detalles y las alternativas están en [research.md](research.md). La decisión que afecta a todas las historias con interfaz es **ADR-019**, aprobada por el autor el 2026-10-15 junto con D-53 (`docs/decisiones.md`).

## Technical Context

**Language/Version**: TypeScript 5.9.3 · Node 24 LTS · HTML/CSS · Nginx (configuración) · Bash

**Primary Dependencies**: las de research §1, con versiones exactas en `frontend/package.json`:
- react 19.3.0, vite 8.3.1, react-router 8.4.0, @tanstack/react-query 5.104.0, react-hook-form 7.89.0, zod 4.6.5 y @mui/material 9.4.0;
- openapi-typescript 7.13.0 y openapi-fetch 0.17.0.

**Storage**: ninguno. El token y la sesión viven en memoria (D-51).

**Testing**:
- Vitest 5 con jsdom y Testing Library (cobertura v8, 60 % de líneas);
- Playwright 1.63 con @axe-core/playwright 4.13 en Chromium;
- `scripts/verificar-ruptura.mjs` (escenario 1);
- ESLint 9 (jsx-a11y) y Prettier;
- `deploy/probar_spa.sh` sobre el perfil.

**Target Platform**: las dos últimas versiones de Chrome, Firefox y Edge, desde 1366 px (RNF-COM-01). En el perfil consolidado, en el puerto 8081; en el VPS, detrás de Caddy (B00-H08).

**Project Type**: monorepo web: SPA en `frontend/`, contratos en `contratos/`, despliegue en `deploy/` y `compose.yaml`.

**Performance Goals**: el inicio de sesión y la pantalla de inicio, en menos de 1 minuto de punta a punta para el autor (SC-005). La imagen de Nginx, con 64 MiB de límite.

**Constraints**:
- sin almacenamiento del token en el navegador;
- sin CDN externos (fuentes en el paquete);
- CSP sin `unsafe-inline` en los scripts;
- R-19 por debajo de 7 GiB con el contenedor nuevo;
- los nombres de los 10 checks no cambian (D-31).

**Scale/Scope**:
- 3 pantallas, 1 marco y 1 traductor de errores;
- 1 contrato consumido (Usuarios), sin contratos nuevos;
- 1 contenedor nuevo y 1 script de punta a punta;
- cambios en `ci.yml` (job `test-frontend`), `frontend.Dockerfile`, `compose.yaml`, `medir_memoria.sh`, `perfil-consolidado.yml` y `dependabot.yml`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I y II | Sin dominio ni capas del backend; la SPA no tiene reglas de negocio. | ✅ N/A |
| III. Lenguaje ubicuo | La interfaz está en español con los términos del glosario; los identificadores técnicos del frontend, en inglés solo donde es convención (`App`, `main`). | ✅ |
| IV. API primero | La SPA consume `contratos/usuarios/openapi.yaml` con tipos generados; si el contrato cambia un campo usado, no compila (escenario 1). Sin contratos nuevos. | ✅ |
| V. Límites | La SPA solo habla con el API Gateway por `/api`, nunca con un proceso. | ✅ |
| VI y VII | Sin reglas ni datos de negocio. | ✅ N/A |
| VIII. Pruebas | Las pruebas van primero: el traductor, la sesión, las rutas protegidas, el formulario, la ruptura de contrato, la accesibilidad y los flujos. 60 % de líneas. Datos ficticios. | ✅ |
| IX. Seguridad | Token solo en memoria (D-51), con una regla de ESLint que lo hace cumplir; CSP y cabeceras en Nginx; sin redirección abierta; SBOM y Grype del frontend (jobs que ya existen); Gitleaks. | ✅ |
| X. Simplicidad | Sin MSW, sin vitest-axe, sin código de cliente generado (solo tipos). | ✅ |
| XI. Diseño pragmático | Una carpeta por responsabilidad (`sesion`, `errores`, `navegacion` y `pantallas`), sin capas extra. | ✅ |
| XII. JavaDoc | No aplica al frontend; los módulos llevan su comentario de propósito en español. | ✅ |
| XIII. Errores | La contraparte del advice único: muestra `titulo` y `descripcion` y nunca el `codigo` ni texto técnico; los genéricos salen del catálogo y una prueba los compara con `errores.md`. | ✅ |
| §3 Restricciones técnicas | SPA en español con WCAG 2.1 AA desde 1366 px; React con TypeScript y Vite, React Router, TanStack Query, React Hook Form con Zod, MUI en español, cliente desde OpenAPI y Nginx; Vitest, Testing Library, axe-core y Playwright. | ✅ |
| §6.4 ADR | ADR-019 y D-53, aprobados por el autor el 2026-10-15. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. ADR-019 y D-53 quedaron aprobados el 2026-10-15.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/007-spa-base/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: sesión, mensaje de error, formulario y cliente generado
├── quickstart.md            # fase 1
├── contracts/
│   ├── interfaz.md              # rutas, marco, errores, accesibilidad y comandos de npm
│   └── perfil.md                # contenedor frontend, Nginx y deploy/probar_spa.sh
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
frontend/                        # nuevo (research §2)
├── package.json  package-lock.json  .nvmrc  .gitignore
├── index.html  vite.config.ts  vitest.config.ts  playwright.config.ts
├── eslint.config.js  .prettierrc.json  .prettierignore  tsconfig.json  tsconfig.node.json
├── nginx.conf                   # plantilla con MDD_URI_GATEWAY (research §10)
├── scripts/verificar-ruptura.mjs
├── e2e/                         # accesibilidad.spec.ts, sesion.spec.ts y simulacion.ts
└── src/
    ├── main.tsx  App.tsx  rutas.tsx  tema.ts
    ├── api/cliente.ts           # openapi-fetch con el token y el cierre ante un 401
    ├── api/generado/            # ignorado por Git
    ├── sesion/                  # SesionProvider, useSesion y RutaProtegida
    ├── errores/                 # traducir.ts, mensajes.ts y AvisoDeError.tsx
    ├── navegacion/              # Marco.tsx y menú
    └── pantallas/               # IniciarSesion.tsx, Inicio.tsx y NoEncontrada.tsx
deploy/
├── docker/frontend.Dockerfile   # cambia: contexto en la raíz, con contratos/ y la plantilla de Nginx
├── medir_memoria.sh             # cambia: + frontend
└── probar_spa.sh                # nuevo
compose.yaml                     # cambia: + servicio frontend (8081:8080, 64 MiB)
.github/workflows/ci.yml         # cambia: test-frontend + Playwright y verificar:ruptura
.github/workflows/perfil-consolidado.yml   # cambia: + paso «SPA de punta a punta»
.github/dependabot.yml           # cambia: docker ignora versiones mayores de node (D-53)
specs/002-compose-cuatro-procesos/contracts/medicion-r19.md   # cambia: + frontend
```

**Structure Decision**: un solo proyecto de frontend en `frontend/`, como espera la CI desde D-29. La imagen se construye desde la raíz, igual que las de Java (ADR-015).

## Fases de implementación (guía para /speckit-tasks)

1. **ADR-019 y D-53:** aprobados el 2026-10-15 (`docs/decisiones.md`).
2. **Preparación:** Node 24 local (research §13), `frontend/` con `package.json` y las versiones exactas, la configuración de TypeScript, Vite, ESLint, Prettier, Vitest y Playwright, y `generar:api`.
3. **Cliente y errores**, con sus pruebas primero: el traductor y sus mensajes comparados con `errores.md`, y el cliente openapi-fetch con su *middleware*. Después `verificar:ruptura` (escenario 1).
4. **Sesión y rutas**, con sus pruebas primero: `SesionProvider`, `RutaProtegida`, el 401 y el vencimiento.
5. **Pantallas:** inicio de sesión (React Hook Form con Zod), inicio, marco y «no encontrada».
6. **Accesibilidad y flujos:** Playwright con axe-core (escenario 3) y los flujos de sesión.
7. **CI (escenario 4):** `test-frontend` con Playwright y `verificar:ruptura`, y la prueba del check con un error de ESLint en el pull request.
8. **Imagen y perfil:** `frontend.Dockerfile`, `nginx.conf`, el servicio en `compose.yaml`, `medir_memoria.sh`, `probar_spa.sh` y su paso en `perfil-consolidado`.
9. **Node (D-53):** `.nvmrc`, `dependabot.yml` y el cierre del PR #73 con la aprobación del autor.
10. **Cierre:** `evidencia.md`, el pull request con los 10 checks en verde, la fusión con la aprobación del autor y el PR de evidencia con `perfil-consolidado` sobre `main`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Playwright en el job `test-frontend` (con Chromium) | axe-core necesita un navegador real para el contraste de color, que es parte de WCAG 2.1 AA | axe-core en jsdom no calcula el contraste, y el escenario 3 quedaría a medias. |
| `verificar:ruptura` (un script propio) | El escenario 1 pide demostrar que un cambio de contrato rompe la compilación, y la CI debe repetirlo | Comprobarlo una vez a mano no detectaría si un día los tipos dejan de generarse desde el contrato. |
