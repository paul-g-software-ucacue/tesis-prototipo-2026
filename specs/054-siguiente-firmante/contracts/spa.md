# Contrato: la cadena de firmas en la SPA

F16-H02 (#68). En `frontend/src/pantallas/comun/DocumentoFirmable.tsx`, la tarjeta del documento firmable dentro de «Consolidado del periodo».

## Qué se ve

Con el documento GENERADO y `cadena` con pasos, debajo de «Documento firmable generado» y su huella:

- Un subtítulo «Cadena de firmas» y una lista con ese nombre accesible.
- Un elemento por paso, en su orden: «F{orden} · {rótulo}», el firmante y el estado en texto:

| Estado | Texto |
|---|---|
| `FIRMADO` | «Firmado el {dd/mm/aa hh:mm}», en `America/Guayaquil` |
| `EN_TURNO` | «En turno» |
| `PENDIENTE` | «Pendiente» |
| `RECHAZADO` | «Rechazado» |

- Sin `firmante`, el elemento dice solo el orden, el rótulo y el estado.
- El paso en turno se resalta, por ejemplo en negrita y con un ícono, pero el estado se dice siempre con texto (WCAG 1.4.1).
- Con `cadena` vacía no se muestra nada nuevo. La tarjeta sigue como hoy en EN_CURSO, FALLIDA, SIN_GENERAR o con anulación.
- El documento de otra versión (F12-H04), que la tarjeta ya trata como SIN_GENERAR, tampoco muestra la cadena.

## Cuándo se actualiza

La consulta del documento ya se repite al abrir la pantalla (FR-014). No hace falta consultarla cada cierto tiempo.

## La notificación

La campana de F16-H01 no cambia. La notificación de firma pendiente lleva a `/firmas-pendientes?periodo=…`. «Pendientes de firma» ignora el parámetro: lista todos los documentos en turno del usuario.

## Pruebas

- **Vitest (`DocumentoFirmable.test.tsx`):**
  - los cuatro estados, con su texto;
  - el paso en turno resaltado;
  - un paso sin firmante;
  - la cadena vacía, sin la lista;
  - el documento de otra versión, sin la lista.
- **`enlaces.test.ts`:** `/firmas-pendientes`, el enlace de la notificación, lo pueden abrir VICERRECTOR y RECTOR (`DISTRIBUTIVO:FIRMA`).
- **Playwright (`e2e/firmas-pendientes.spec.ts` o `consolidado.spec.ts`):** el Vicerrector ve la cadena con F2 en turno, y axe-core (WCAG 2.1 AA) no encuentra fallas (SC-006).
