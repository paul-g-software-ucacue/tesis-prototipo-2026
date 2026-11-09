# Contrato: Generación (`contratos/generacion/openapi.yaml` y su puerto requerido)

## Operaciones que se rechazan durante la revisión

| Operación | 409 nuevo | Cuándo |
|---|---|---|
| `consolidar` (`POST …/consolidado`) | `GENERACION_CONSOLIDADO_EN_REVISION` | El distributivo está en EN_REVISION_VRA o después |
| `justificarInconsistencia` (`PUT …/inconsistencias/justificacion`) | `GENERACION_CONSOLIDADO_EN_REVISION` | Igual |
| `adjuntarInformeSustentado` (`PUT …/consolidado/informe`) | `GENERACION_CONSOLIDADO_EN_REVISION` | Igual |

Las descripciones de los 409 de esas operaciones nombran el código nuevo. Las descripciones de las operaciones no nombran la bitácora (`BitacoraSinBorradoContratoTest`).

## Consultas

- `ConsolidadoDelPeriodo` suma `enRevision: boolean`, obligatorio: el distributivo está en EN_REVISION_VRA o después, y el consolidado ya no cambia.
- El panel de inconsistencias suma el mismo `enRevision`.
- `descargarInformeSustentado` no cambia.

## Catálogo

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `GENERACION_CONSOLIDADO_EN_REVISION` | 409 | Consolidado en revisión | El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia. |

Con sus 8 columnas en `contratos/comun/errores.md` y su entrada en `ErroresGeneracion`.

## El puerto requerido (sin ruta HTTP)

`ec.mdd.generacion.puertos.requeridos.EtapaDelConsolidado`, en una interfaz nombrada nueva (`requeridos`). Solo lo implementa Validación OCS, en el mismo proceso `distributivo` (research §1).

**Pruebas:**
- Contrato (Generación): el 409 en las tres operaciones, con su código, y `enRevision` en las dos consultas.
- Las pruebas de Spring de Generación que arman su configuración suman un doble del puerto.
