# Contrato: SPA

## El estado del flujo (`EstadoDelFlujo.tsx`)

El aviso «Rechazado en firma» (F14-H04) conserva el paso, el rol, quién, cuándo y la observación. La última línea:

| Situación | Texto |
|---|---|
| Con `versionNueva` | «La versión 1 quedó anulada con sus firmas; el distributivo sigue con la versión 2.» |
| Sin `versionNueva` | «Para aprobar otra vez hace falta una versión nueva.» (como en F14-H04) |

«Aprobar el contenido» sale por `acciones`, sin cambios en la pantalla.

## «Consolidado del periodo» y el panel de inconsistencias

- Muestran la versión vigente, la nueva.
- Con la versión nueva EDITABLE en EN_REVISION_VRA, el aviso es el de revisión (#215).
- `AvisoDeVersionCongelada` vuelve al texto único de F13-H01, sin la rama del rechazo de F14-H04 (research §8).

## Accesibilidad

Sin controles nuevos. El aviso sigue siendo `role="status"` con su título.
