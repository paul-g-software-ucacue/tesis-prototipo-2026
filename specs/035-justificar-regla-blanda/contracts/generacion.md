# Contrato: Generación

Cambios en `contratos/generacion/openapi.yaml`.

## `proponerDocente`

**200** ya no es `Propuesta`: es `ResultadoDePropuesta`.

| Campo | Requerido | Descripción |
|---|---|---|
| `guardada` | Sí | Si quedó guardada |
| `propuesta` | No | `Propuesta`; viene si se guardó |
| `advertencia` | No | `Advertencia`; viene si no se guardó |

`Advertencia`: `regla`, `titulo` y `descripcion`, los tres requeridos.

**409** agrega `GENERACION_AFINIDAD_INSUFICIENTE`.

**400** sigue para un texto de 1 a 9 o de más de 500 caracteres.

## `Propuesta`

Agrega `inconsistencia`, opcional: `regla` y `estado`, que hoy solo puede ser `JUSTIFICADA`.

## `Candidato`

`motivoParaNoProponer` agrega `AFINIDAD`.
