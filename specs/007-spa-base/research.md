# Research: SPA base con el cliente generado (B00-H06)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Las versiones se eligieron el 2026-10-15 con el registro de npm, con la misma regla de las historias anteriores: la última versión estable con al menos dos semanas (publicada hasta el 2026-10-01) que cumple las dependencias *peer* de las demás.

## 1. Versiones

| Paquete | Versión | Nota |
|---|---|---|
| Node | 24 (LTS «Krypton») | La que ya fija la CI (`NODE_VERSION: "24"`) y `frontend.Dockerfile`; ver §12 |
| react, react-dom | 19.3.0 | 19.3.1 tiene un día |
| typescript | **5.9.3** | typescript-eslint 8.71 pide `<6.1.0` y openapi-typescript 7.13 pide `^5.x`; TS 6.0 y 7.0 (compilador nativo) rompen esas *peer* |
| vite | 8.3.1 | Node `^20.19 \|\| >=22.12` |
| @vitejs/plugin-react | 6.1.1 | Vite 8 |
| react-router | 8.4.0 | Modo biblioteca (`createBrowserRouter`); Node `>=22.22` |
| @tanstack/react-query | 5.104.0 | |
| react-hook-form | 7.89.0 | |
| zod | 4.6.5 | |
| @hookform/resolvers | 5.9.1 | Zod 4 |
| @mui/material | 9.4.0 | Con `esES` de `@mui/material/locale` |
| @emotion/react, @emotion/styled | 11.14.0, 11.14.1 | Las que pide MUI |
| @fontsource/roboto | 5.3.0 | La fuente va en el paquete: sin CDN externo |
| openapi-typescript | 7.13.0 | Tipos desde los contratos |
| openapi-fetch | 0.17.0 | Cliente tipado con esos tipos, sin generar código |
| vitest, @vitest/coverage-v8 | 5.0.3 | Node `^22.12 \|\| ^24` |
| jsdom | 30.1.1 | |
| @testing-library/react, /dom, /user-event, /jest-dom | 16.3.3, 10.4.2, 14.6.7, 7.0.1 | |
| @playwright/test | 1.63.0 | Solo Chromium en la CI |
| @axe-core/playwright, axe-core | 4.13.0 | 4.14.0 tiene diez días |
| eslint, @eslint/js | **9.39.5** | eslint-plugin-jsx-a11y 6.10 solo admite hasta ESLint 9 |
| typescript-eslint | 8.71.0 | |
| eslint-plugin-react-hooks, eslint-plugin-react-refresh, eslint-plugin-jsx-a11y | 7.1.1, 0.5.7, 6.10.2 | |
| eslint-config-prettier, prettier | 10.1.8, 3.9.9 | |
| globals, @types/node | 17.12.0, 24.19.0 | |
| @cyclonedx/cyclonedx-npm | 6.0.1 | SBOM del frontend (`ci.yml`) |

Playwright corre en Chromium (todas las pruebas) y en Firefox (los flujos y los errores); Edge usa el motor de Chromium (FR-009).

Las versiones se fijan exactas en `package.json` (sin `^`) y `package-lock.json` se versiona. Dependabot ya está configurado para `/frontend`.

**Alternativas.** TypeScript 6 o 7, que romperían typescript-eslint y openapi-typescript; ESLint 10 sin jsx-a11y, que perdería las reglas de accesibilidad en el lint.

## 2. Estructura de `frontend/`

**Decisión.**

```text
frontend/
├── package.json  package-lock.json  .nvmrc (24)
├── index.html                 # lang="es"
├── vite.config.ts             # proxy de /api al API Gateway en desarrollo
├── vitest.config.ts           # jsdom, cobertura v8 con umbral de 60 % de líneas
├── playwright.config.ts       # Chromium, sobre `vite preview`
├── eslint.config.js  .prettierrc.json  tsconfig.json
├── nginx.conf                 # la SPA y /api hacia el API Gateway (D-52)
├── scripts/verificar-ruptura.mjs
├── e2e/                       # Playwright: accesibilidad y flujos con la API simulada
└── src/
    ├── main.tsx  App.tsx  tema.ts  rutas.tsx
    ├── api/                   # generado/ (ignorado por Git), cliente.ts
    ├── sesion/                # SesionProvider, RutaProtegida, useSesion
    ├── errores/               # traducir.ts, MensajeDeError, AvisoDeError
    ├── navegacion/            # Marco (encabezado, menú y cerrar sesión)
    └── pantallas/             # IniciarSesion, Inicio, NoEncontrada
```

Cada historia agrega sus pantallas en `pantallas/` y sus entradas del menú en `navegacion/`.

**Por qué.** Es la base común que pide la historia, con una sola carpeta por responsabilidad: sesión, errores y navegación no se repiten en cada pantalla (FR-012).

## 3. Cliente generado (escenario 1)

**Decisión.**
- `npm run generar:api` corre `openapi-typescript` sobre `../contratos/<servicio>/openapi.yaml` por cada servicio que usa la SPA. En esta historia es solo `usuarios`, con su `$ref` a `comun/`. Escribe `src/api/generado/usuarios.d.ts`, que Git ignora.
- El cliente es `openapi-fetch` con esos tipos (`createClient<paths>`), con `baseUrl` `/` (mismo origen). Un *middleware* agrega `Authorization: Bearer <token>` (§5) y, ante un 401, cierra la sesión (§5).
- La compilación (`npm run tipos` = `tsc --noEmit`, y `npm run build`) usa esos tipos: si un contrato renombra, quita o cambia el tipo de un campo que la SPA usa (por ejemplo, `TokenSesion.token`), TypeScript falla.
- **Prueba automática del escenario** (`npm run verificar:ruptura`, `scripts/verificar-ruptura.mjs`):
  1. copia el contrato de Usuarios a una carpeta temporal y renombra `token` por `jwt` en `TokenSesion`;
  2. genera los tipos de esa copia sobre `src/api/generado/usuarios.d.ts`;
  3. corre `tsc --noEmit` y exige que falle, mencionando `token`;
  4. repite con un cambio de tipo (`venceEn` de `string` a `integer`);
  5. cambia un campo que la SPA no usa (`Jwk.kty`) y exige que `tsc` pase;
  6. restaura los tipos del contrato real y exige que `tsc` pase.

  Corre en el job `2 · test-frontend`.
- `generar:api` corre antes de `tipos`, `build`, `test` y `dev` (`pre`-scripts de npm), en la CI (ya está en `ci.yml`) y en la imagen (ya está en `frontend.Dockerfile`) (FR-003).

**Por qué.** openapi-typescript es lo que fija la constitución (§3, principio IV). openapi-fetch usa los mismos tipos sin generar código de runtime, así que no hay una segunda fuente de verdad.

**Alternativas.** openapi-generator `typescript-fetch` (genera clases y código de runtime, más pesado y con otro estilo de tipos); escribir el cliente con `fetch` a mano (pierde el escenario).

## 4. Errores (escenario 2)

**Decisión.** Un solo traductor, `src/errores/traducir.ts`, convierte cualquier fallo en un `MensajeDeError` (`titulo`, `descripcion`, `detalles` y `idIncidente` opcionales):
- **cuerpo con el esquema `Error`** (validado con un esquema Zod del tipo generado): toma `titulo`, `descripcion`, `detalles` e `idIncidente`. Nunca toma `codigo`;
- **cuerpo sin ese esquema** (HTML, vacío o JSON con otra forma): el mensaje de `COMUN_ERROR_INTERNO` del catálogo, sin `idIncidente`;
- **fallo de red** (`TypeError` de `fetch`): el mensaje de `COMUN_SERVICIO_NO_DISPONIBLE` del catálogo.

Los dos textos genéricos son los de `contratos/comun/errores.md`, copiados en `src/errores/mensajes.ts`, y una prueba los compara con el archivo. `AvisoDeError` (una `Alert` de MUI con `role="alert"`) muestra el título y la descripción, y el `idIncidente` como «Identificador del incidente: …». Los `detalles` se pasan a React Hook Form (`setError(campo, …)`) para mostrarlos junto a su campo. Ninguna pantalla lee el cuerpo de un error por su cuenta.

**Por qué.** Es la contraparte del advice único del backend (ADR-016): el `codigo` sirve a la SPA para decidir, pero no se muestra. Así nunca aparece un estado HTTP, una traza ni el texto de una excepción (FR-010 a FR-012).

**Alternativas.** Mostrar `error.message` de JavaScript (filtra textos técnicos); traducir por `codigo` en cada pantalla (repite y se desalinea del catálogo).

## 5. Sesión, token y rutas protegidas (D-50, D-51)

**Decisión.**
- `SesionProvider` guarda el token y su vencimiento **solo en memoria** (estado de React y una referencia del módulo que lee el *middleware* del cliente). Nunca usa `localStorage`, `sessionStorage`, `document.cookie` ni la URL. Una regla de ESLint (`no-restricted-globals` y `no-restricted-properties`) prohíbe esos almacenamientos en `src/`.
- **Inicio de sesión:** `POST /api/usuarios/sesion` con usuario y clave (React Hook Form con Zod: los dos obligatorios, con mensajes en español). Si sale bien, guarda el token y navega al destino pedido o a `/`. Si no, muestra el error con el traductor (§4).
- **Sesión actual:** la pantalla de inicio la consulta con TanStack Query (`GET /api/usuarios/sesion`) y muestra el usuario y sus roles.
- **Rutas protegidas:** `RutaProtegida` envuelve las rutas internas. Sin token, redirige a `/iniciar-sesion?destino=<ruta pedida>`. El destino solo se acepta si es una ruta interna de la SPA (empieza con `/` y no con `//`), para que no sirva de redirección abierta.
- **401 durante la navegación:** el *middleware* del cliente cierra la sesión (borra el token y la caché de TanStack Query) y la SPA vuelve al inicio de sesión con el aviso «Tu sesión terminó. Inicia sesión de nuevo.».
- **Vencimiento:** cuando el token vence, se cierra la sesión de la misma forma, sin esperar a un 401.
- **Cerrar sesión:** borra el token y la caché y vuelve al inicio de sesión. No hay llamada al backend: el token es *stateless* (ADR-014).
- **Recarga:** el token se pierde y la SPA vuelve al inicio de sesión (D-51).

**Por qué.** Es lo que fijan D-50 y D-51, con lo que ya existe en el backend (D-37, D-39), y la regla de ESLint lo vuelve verificable (SC-008).

**Alternativas.** Guardar el token en `sessionStorage` o en una cookie (descartado en D-51); proteger cada pantalla por su cuenta (lo descarta D-50).

## 6. Navegación, tema y español

**Decisión.**
- **Rutas:** `/iniciar-sesion` (pública), `/` (Inicio, protegida) y `*` (No encontrada).
- **Marco:** `AppBar` con «MDD · Distributivo docente», el usuario y el botón «Cerrar sesión», más un menú lateral con «Inicio». Las historias agregan sus entradas.
- **Tema** de MUI con `esES`, `<html lang="es">` y el título de cada pantalla en `document.title`.
- Cada pantalla tiene un `h1` y una región `main`; los enlaces de la navegación usan `NavLink`.
- Ancho mínimo de 1366 px probado en Playwright (RNF-COM-01).

**Por qué.** Cumple la constitución (§3: MUI en español y WCAG 2.1 AA) y da a las historias siguientes dónde colgar sus pantallas.

## 7. Accesibilidad (escenario 3)

**Decisión.** `e2e/accesibilidad.spec.ts` con Playwright y `@axe-core/playwright`, en Chromium, sobre la SPA construida (`vite preview`), con las reglas `wcag2a`, `wcag2aa`, `wcag21a` y `wcag21aa`:
- la pantalla de inicio de sesión en su estado inicial;
- la misma pantalla con el error de credenciales inválidas mostrado (la API simulada con `page.route`);
- con los dos errores de campo vacío.

Cada análisis exige `violations` vacío. ESLint suma `eslint-plugin-jsx-a11y` (recomendado) como primera barrera.

**Por qué.** axe en jsdom no calcula el contraste de color, que es parte de WCAG 2.1 AA; en un navegador real, sí. Playwright ya está en la constitución (§3, pruebas).

**Alternativas.** axe-core en Vitest con jsdom (sin contraste); vitest-axe (sin cambios desde 2022).

## 8. Pruebas y cobertura

**Decisión.**
- **Vitest con jsdom y Testing Library** para el traductor de errores, la sesión, `RutaProtegida`, el *middleware* del cliente, el formulario de inicio de sesión, la pantalla de inicio y el marco. La API se simula con un `fetch` escrito a mano que responde según la ruta (sin MSW, para no sumar dependencias).
- **Cobertura v8** con umbral de 60 % de líneas (`thresholds.lines: 60`), que hace fallar `npm run test -- --run --coverage` (principio VIII, D-32). Los tipos generados y `main.tsx` no cuentan.
- **Playwright** (`npm run test:e2e`) para la accesibilidad (§7) y los flujos completos con la API simulada:
  - sin sesión, `/` redirige al inicio de sesión y, después de iniciarla, vuelve a `/` (SC-007);
  - después de iniciar sesión, el token no está en `localStorage`, `sessionStorage`, cookies ni en la URL (SC-008);
  - una recarga vuelve al inicio de sesión;
  - un 401 de la sesión actual cierra la sesión.
- **`ci.yml`:** el job `2 · test-frontend` suma `npx playwright install --with-deps chromium`, `npm run test:e2e` y `npm run verificar:ruptura`. Los nombres de los 10 checks no cambian (D-31).

## 9. Lint y formato (escenario 4)

**Decisión.**
- **`eslint.config.js`** (configuración plana): `@eslint/js` recomendado, `typescript-eslint` recomendado con tipos, `react-hooks`, `react-refresh`, `jsx-a11y` recomendado y `eslint-config-prettier` al final.
- **Reglas propias:**
  - sin `localStorage`, `sessionStorage` ni `document.cookie` (§5);
  - sin `console` en `src/`;
  - `src/api/generado/` se ignora.
- **`npm run lint`** = `eslint . --max-warnings 0`: un aviso también falla.
- **Prettier** con `npm run formato:verificar`.

Los pasos ya están en el job `1 · lint-frontend` de `ci.yml` (D-29). La regla de la rama principal ya exige ese check (D-31), así que un error de ESLint bloquea la fusión sin cambiar la regla.

**Comprobación del escenario:** un commit de prueba en el pull request con un error de ESLint, que el check marca en rojo, y su reversión en verde. Se anota en la evidencia.

## 10. Imagen y Nginx

**Decisión.**
- **`deploy/docker/frontend.Dockerfile`:** el contexto pasa a ser la raíz del repositorio, como el de Java (ADR-015), porque `generar:api` necesita `contratos/`. Copia `frontend/` y `contratos/`, corre `npm ci`, `npm run build` (con `prebuild` → `generar:api`) y deja `dist/` en `nginxinc/nginx-unprivileged:stable-alpine` con `frontend/nginx.conf`.
- **`nginx.conf`** (puerto 8080, sin privilegios):
  - `location /api/` hacia `http://gateway:8080` (variable `MDD_URI_GATEWAY` con `envsubst` de la imagen), con el plazo y las cabeceras `X-Forwarded-*`;
  - `location /` con `try_files $uri /index.html`, para las rutas de la SPA;
  - `/assets/` con caché larga (los nombres llevan *hash*), e `index.html` sin caché;
  - las cabeceras de seguridad, que ZAP revisará al cerrar la ola:
    - `Content-Security-Policy`: `default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`. Emotion inserta estilos, por eso hace falta `'unsafe-inline'` en los estilos, nunca en los scripts;
    - `X-Content-Type-Options`, `Referrer-Policy: no-referrer` y `Permissions-Policy`;
  - `server_tokens off`;
  - una ruta `/salud` que responde 200, para el *healthcheck*.

**Por qué.** El mismo origen evita CORS y deja el token en una cabecera que solo va a `/api` (ADR-014). En el VPS, Caddy ya hace esta división (`deploy/Caddyfile`); en local la hace Nginx (D-52).

## 11. Perfil consolidado (D-52)

**Decisión.**
- **Servicio `frontend` en `compose.yaml`:**
  - `build` con `context: .` y `dockerfile: deploy/docker/frontend.Dockerfile`;
  - depende del API Gateway sano;
  - *healthcheck* `wget -q -O- http://127.0.0.1:8080/salud` (la imagen Alpine de Nginx trae `wget`);
  - límite de memoria de 64 MiB;
  - puerto `8081:8080`.

  El API Gateway sigue en `8080` para los scripts de punta a punta, que no cambian. El total de límites pasa de 2 944 a 3 008 MiB.
- **`deploy/medir_memoria.sh`** suma `frontend` a su orden de contenedores (no es JVM). Se ajusta `specs/002-compose-cuatro-procesos/contracts/medicion-r19.md`.
- **`deploy/probar_spa.sh`** (en `perfil-consolidado`, después de los eventos):
  1. `GET /` en `:8081` responde `index.html` con la CSP;
  2. `GET /inicio-que-no-existe` también responde `index.html` (la ruta de la SPA);
  3. `POST /api/usuarios/sesion` a través de la SPA (`:8081/api/...`) responde 200 con token;
  4. `GET /api/usuarios/sesion` con ese token, a través de la SPA, responde el usuario y sus roles;
  5. `GET /api/usuarios/sesion` sin token, a través de la SPA, responde el 401 uniforme.
- **Revisión manual del quickstart:** iniciar sesión en el navegador, ver usuario y roles, y cerrar sesión (SC-005).

**Por qué.** Prueba que la SPA y el proxy funcionan con el perfil real (SC-009) sin agregar navegadores al runner de `perfil-consolidado`: los flujos de la interfaz ya los cubre Playwright con la API simulada (§8).

**Alternativas.** Publicar la SPA en el puerto 8080 y mover el API Gateway: cambiaría los tres scripts de punta a punta y sus contratos.

## 12. Node y el pull request #73 de Dependabot

**Decisión** (D-53). Se queda **Node 24**, la LTS activa, en la CI (`NODE_VERSION`), en `frontend.Dockerfile` y en `.nvmrc`. El pull request #73 (Node 24 → 26) se cierra con un comentario: Node 26 todavía es «Current» y no LTS, y Vitest y React Router ya funcionan con Node 24. En `.github/dependabot.yml`, el ecosistema `docker` ignora los cambios de versión mayor de `node`: el paso a la próxima LTS se hace a mano, en la CI y en la imagen a la vez.

**Por qué.** La CI y la imagen deben usar la misma versión, y en producción conviene una LTS. Fusionar el #73 dejaría Node 26 en la imagen y Node 24 en la CI.

**Alternativas.** Fusionar el #73 y subir la CI a Node 26 (sale de LTS); dejar el #73 abierto (Dependabot lo seguiría actualizando).

## 13. Entorno local

**Decisión.** El equipo del autor tiene Node 20.20, y Vitest 5 y React Router 8 piden Node 22 o posterior. Los comandos de npm se corren con Node 24 sin `sudo`, con una de dos opciones:
- **el binario oficial de Node 24** en `~/.local/node-24` (agregado al `PATH` de la sesión);
- **el contenedor `node:24-alpine`** sobre una copia de `frontend/` y `contratos/` en el directorio temporal de la sesión, nunca montando el repositorio con `:z`. El `package-lock.json` resultante se copia de vuelta.

`npm run dev` (Vite en el puerto 5173, con proxy de `/api` al API Gateway en `localhost:8080`) queda para quien desarrolle pantallas.

## 14. ADR-019

La base del frontend fija cómo construyen sus pantallas todas las historias con interfaz:
- la estructura de `frontend/`;
- el cliente generado con openapi-typescript y openapi-fetch;
- el traductor único de errores;
- la sesión en memoria con rutas protegidas;
- el mismo origen con `/api` a través de Nginx o Caddy;
- la accesibilidad con Playwright y axe-core.

Es una decisión que afecta a varias features, así que se registra como **ADR-019** (constitución §6, paso 4), aprobada por el autor el 2026-10-15. D-53 (Node 24 y el #73) es una decisión de alcance, no de arquitectura.
