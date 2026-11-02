# Contrato: Generación

Cambios en `contratos/generacion/openapi.yaml`.

## `proponerDocente` y `asignarActividad`

Agregan a su 409:

| Código | Título | Detalle |
|---|---|---|
| `GENERACION_HORAS_EXCEDIDAS` | Horas laborales excedidas | `horas`: las que le quedan |
| `GENERACION_TOPE_DE_CLASE` | Tope de horas de clase | `horas`: las de clase que le quedan |

## `ActividadGuardada`

Quita `horasSuficientes`.

## `Candidato`

Agrega `motivoParaNoProponer`, opcional: `HORAS` o `TOPE_DE_CLASE`. Si no viene, se puede proponer.

## `Error` (`contratos/comun/errores.yaml`)

`detalles` deja de ser solo de errores de entrada.
