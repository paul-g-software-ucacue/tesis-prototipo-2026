# Contrato: enviar a revisión en la SPA

**`EstadoDelFlujo`** (`frontend/src/pantallas/comun/EstadoDelFlujo.tsx`):
- Con `ENVIAR_A_REVISION` en `acciones`, muestra el botón «Enviar a revisión».
- Una prop opcional nueva, `faltan: string[]`. Si tiene algo, el botón queda deshabilitado y debajo se lista lo que falta, con `aria-describedby` hacia esa lista.
- Antes de enviar, una confirmación: «El distributivo pasa a revisión del Vicerrectorado. Ya no se podrá consolidar ni justificar en esta etapa.» Con la acción «Enviar».
- Los errores se muestran con `AvisoDeError`; el detalle de `INCONSISTENCIAS_ABIERTAS` aparece con el mensaje.

**`Consolidado.tsx`** arma `faltan` con los datos que ya consulta (research §9):

| Condición | Texto |
|---|---|
| Sin `consolidacion` | «Consolida el periodo.» |
| `desactualizado` | «Consolida otra vez: algo cambió después de consolidar.» |
| Sin `informe` | «Adjunta el informe sustentado.» |
| `abiertas.duras + abiertas.blandas > 0` | «Quedan N inconsistencias abiertas.», con enlace al panel |

Al enviar, invalida el flujo y el consolidado.

**Pruebas:**
- Vitest de `EstadoDelFlujo`: el botón solo con la acción; deshabilitado y con la lista si falta algo; la confirmación; el 409 con su detalle.
- Vitest de `Consolidado`: cada condición produce su texto; con todo cumplido, el botón habilitado.
- Playwright `e2e/consolidado.spec.ts`: el Responsable ve lo que falta, y el envío exitoso con la API simulada; axe-core sin fallas.
