# Data Model: Enviar a revisión con cero inconsistencias abiertas

Sin tablas ni migraciones nuevas. El proceso de aprobación, sus transiciones y la bitácora son los de F12-H02 (`specs/041-avanzar-etapas/data-model.md`).

## Proceso de aprobación (Validación OCS, dominio)

`ProcesoAprobacion` suma dos operaciones y una acción.

| Operación | Exige | Resultado |
|---|---|---|
| `validarRevision(quien)` | El rol de EN_CONSOLIDACION en la copia de la configuración (`RolDeOtraEtapa`, 403); `estado == EN_CONSOLIDACION` (`EtapaNoAdmiteRevision`); todos los parciales `ENVIADO` (`ParcialesPendientes`) | Nada: solo comprueba, antes de llamar a Generación (research §2) |
| `enviarARevision(quien, condiciones, version, hash, ahora)` | Lo de `validarRevision`, y además las condiciones en el orden de research §8: `SinConsolidar`, `ConsolidadoDesactualizado`, `SinInforme` e `InconsistenciasAbiertas(duras, blandas)` | Una `Transicion` `PASO_DE_ETAPA` de EN_CONSOLIDACION a `siguienteActiva(EN_CONSOLIDACION)`, con el hash del distributivo |
| `acciones(quien)` | — | Suma `ENVIAR_A_REVISION` si `estado == EN_CONSOLIDACION` y `quien` tiene su rol, aunque falten condiciones: la pantalla las muestra (research §9) |

**Un intento rechazado** no cambia el estado ni agrega transiciones (FR-012).

**`AccionDelProceso`** suma `ENVIAR_A_REVISION`.

## Condiciones de revisión (Validación OCS, dominio)

`CondicionesDeRevision`: un valor que no cambia, con lo que Generación dice del consolidado en el momento del envío.

| Campo | Tipo | Regla |
|---|---|---|
| `consolidado` | booleano | Si la versión vigente se consolidó alguna vez |
| `desactualizado` | booleano | Si una propuesta o una actividad cambió después de consolidar. Es falso si no se consolidó |
| `informe` | texto, opcional | La huella SHA-256 del informe vigente; vacía si no hay |
| `duras` | entero ≥ 0 | Las duras abiertas del panel |
| `blandas` | entero ≥ 0 | Las blandas sin justificar del panel (MD-07) |

## Lo que publica Generación (`generacion.puertos.entrada`)

| Tipo | Contenido |
|---|---|
| `ParcialesDelDistributivo.condicionesDeRevision(periodo)` | Método nuevo. Lanza `DistributivoNoEncontrado` sin distributivo |
| `CondicionesDeRevision` (registro) | `version`, `consolidado`, `desactualizado`, `informe` (huella opcional), `duras` y `blandas`, todos tipos simples, sin clases del dominio de Generación |

El adaptador `DistributivoDesdeGeneracion` lo traduce al valor del dominio de Validación OCS.

## Transición y bitácora

| Dato | Valor |
|---|---|
| `Transicion.tipo` | `PASO_DE_ETAPA` (ya admitido por `transicion.tipo`) |
| `desde` → `hacia` | EN_CONSOLIDACION → siguiente activa (EN_REVISION_VRA por defecto) |
| `carrera` | vacía |
| `version`, `hash` | La versión vigente; `hashDelDistributivo` |
| Acción en la bitácora | `CONSOLIDADO_ENVIADO_A_REVISION` (nueva en `CambioDeFlujo.Accion`) |
| Detalle | `periodo`, `version`, `hash`, `informe` |
| Anterior / nuevo | `{estado: EN_CONSOLIDACION}` / `{estado: <hacia>}` |

## Estados

```text
BORRADOR_CARRERA ──último parcial (F12-H02)──► EN_CONSOLIDACION ──enviar a revisión (F10-H03)──► EN_REVISION_VRA
                                                     │                                              (o la siguiente activa)
                                                     └── 409 si falta una condición; sigue en EN_CONSOLIDACION
```
