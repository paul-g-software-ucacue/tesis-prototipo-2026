# Contrato: la SPA

F12-H03 (#58). Los textos van en español, la fecha en formato `es-EC` y todo cumple WCAG 2.1 AA (FR-011).

## La tarjeta del estado del flujo (`frontend/src/pantallas/comun/EstadoDelFlujo.tsx`)

La usan «Consolidado del periodo», «Oferta y propuesta de docentes» y «Resolución del OCS». Suma dos bloques, debajo de la etapa actual.

### «Plazo de la etapa», con `plazo`

| `estado` | Texto |
|---|---|
| `A_TIEMPO` | «En «<nombre de la etapa>» desde el <llegada>. Fecha límite: <fechaLimite>. Faltan <diasRestantes> días.» («Falta 1 día» en singular) |
| `VENCE_HOY` | «… Fecha límite: <fechaLimite>. Vence hoy.» |
| `VENCIDO` | «… Fecha límite: <fechaLimite>. Vencido hace <−diasRestantes> días.», con un ícono de advertencia con `aria-hidden` y el texto «Plazo vencido» en un `<strong>` |

- Sin `plazo` (PUBLICADO), el bloque no se muestra.
- El color acompaña al texto, nunca lo reemplaza, y cumple el contraste de 4.5:1 en los dos temas.

### «Etapas del flujo», con `etapas`

- Una `<ol>` con cada etapa activa: «<nombre> · <plazoDias> días»; PUBLICADO va sin días.
- La actual lleva `aria-current="step"` y se distingue también con texto («actual»).

## «Configuración del flujo» (`frontend/src/pantallas/flujo/Flujo.tsx`)

- **Después de cargar:** si la respuesta trae `advertencias`, un aviso con `role="status"`, el título «Advertencia de RN-23» y el `mensaje`. También muestra el último plazo, la fecha de RN-23 y los días de margen, si los hay.
  - El aviso dice que la configuración **quedó vigente**: no es un error.
- **En «Qué rige»:** el mismo aviso, si la configuración vigente lo tiene.

## Pruebas

- **Vitest** (`EstadoDelFlujo.test.tsx`):
  - los tres estados con su texto, el singular y el plural;
  - sin `plazo` en PUBLICADO;
  - la lista con `aria-current`.
- **Vitest** (`Flujo.test.tsx`):
  - el aviso después de la carga y en «Qué rige»;
  - `RN23_SIN_NORMATIVA`;
  - sin aviso cuando no hay advertencias.
- **Playwright** con axe-core:
  - `consolidado.spec.ts`: la tarjeta con un plazo vencido simulado en la respuesta;
  - `flujo.spec.ts`: la carga con la advertencia.
- Los tipos salen de `contratos/validacionocs/openapi.yaml` con `npm run generar:api`, y `npm run tipos` los comprueba.
