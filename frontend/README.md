# Frontend del MDD

SPA en español con React 19, TypeScript 5.9 y Vite 8, React Router 8, TanStack Query, React Hook Form con Zod y MUI 9 (constitución §3). Su base la fija ADR-019 (`docs/decisiones.md`) y la detalla `specs/007-spa-base/`.

## Requisitos

Node 24 LTS (`.nvmrc`, D-53).

## Comandos

| Comando                                                        | Qué hace                                                                                                              |
| -------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `npm ci`                                                       | Instala las dependencias con las versiones exactas de `package-lock.json`.                                            |
| `npm run dev`                                                  | Vite en `http://localhost:5173`, con `/api` hacia el API Gateway en `localhost:8080` (`docker compose up -d --wait`). |
| `npm run generar:api`                                          | Genera los tipos desde `../contratos/` en `src/api/generado/` (lo hacen solos `dev`, `build`, `tipos` y `test`).      |
| `npm run lint`, `npm run formato:verificar`, `npm run tipos`   | ESLint, Prettier y TypeScript, como el check `1 · lint-frontend`.                                                     |
| `npm run test -- --run --coverage`                             | Vitest con Testing Library; exige el 60 % de líneas.                                                                  |
| `npx playwright install chromium firefox` y `npm run test:e2e` | Playwright: accesibilidad con axe-core (Chromium) y flujos con la API simulada (Chromium y Firefox).                  |
| `npm run verificar:ruptura`                                    | Comprueba que un cambio de contrato rompe la compilación.                                                             |
| `npm run build` y `npm run preview`                            | La SPA construida, en `http://localhost:4173`.                                                                        |

En el perfil consolidado, la SPA está en `http://localhost:8081` (Nginx, D-52).

## Agregar una pantalla

1. Si usa un servicio nuevo, agregar su contrato a `generar:api` (`openapi-typescript ../contratos/<servicio>/openapi.yaml --output src/api/generado/<servicio>.d.ts`) y su cliente en `src/api/`.
2. La pantalla va en `src/pantallas/`, con su `useTitulo`, un solo `h1` y los textos en español.
3. La ruta va como hija del marco en `src/rutas.tsx`, para que quede protegida (D-50), y su entrada en `ENTRADAS` de `src/navegacion/Marco.tsx`.
4. Las llamadas usan `llamar(api.…)` y los errores, `traducirError` con `AvisoDeError`; ninguna pantalla lee el cuerpo de un error por su cuenta (ADR-019).
5. El token nunca se guarda en el navegador: ESLint lo prohíbe (D-51).
6. Pruebas: Vitest con `instalarApiSimulada` y `montarSpa`, y un análisis de axe-core en `e2e/` si es una pantalla principal (WCAG 2.1 AA).
