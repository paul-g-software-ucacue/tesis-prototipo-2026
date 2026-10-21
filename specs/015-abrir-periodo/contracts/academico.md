# Contrato: Académico · periodos (F02-H01)

Resumen de `contratos/academico/openapi.yaml`, que es nuevo y se escribe antes del código (principio IV). Los detalles y el porqué están en [research.md](../research.md) §5 y §8.

## Esquemas

| Esquema | Campos |
|---|---|
| `EstadoPeriodo` | enum `PLANIFICACION`, `EN_APROBACION`, `VIGENTE`, `CERRADO` |
| `PeriodoAcademico` | `codigo` (patrón `^[A-Za-z0-9._-]{1,20}$`), `nombre` (1 a 120), `inicio` y `fin` (`date`), `estado`, `version` (entero ≥ 1, opcional: sin versión no viene) |
| `PeriodoNuevo` | `codigo`, `nombre`, `inicio`, `fin` |
| `DatosDelPeriodo` | `nombre`, `inicio`, `fin` |
| `VersionDelPeriodo` | `version` (entero ≥ 1) |
| `EstadoDeDestino` | `estado` (`EstadoPeriodo`) |

## Operaciones

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarPeriodos` | `GET /api/academico/periodos` | `DISTRIBUTIVO:LECTURA` | 200 lista de `PeriodoAcademico`, por fecha de inicio descendente |
| `registrarPeriodo` | `POST /api/academico/periodos` | `DISTRIBUTIVO:ESCRITURA` | 201 · 400 · 409 `ACADEMICO_CODIGO_REPETIDO` |
| `consultarPeriodo` | `GET /api/academico/periodos/{codigo}` | `DISTRIBUTIVO:LECTURA` | 200 · 404 |
| `corregirPeriodo` | `PUT /api/academico/periodos/{codigo}` | `DISTRIBUTIVO:ESCRITURA` | 200 · 400 · 404 · 409 `ACADEMICO_PERIODO_CERRADO` |
| `abrirPeriodo` | `PUT /api/academico/periodos/{codigo}/normativa` | `DISTRIBUTIVO:ESCRITURA` | 200 · 400 (versión inexistente) · 404 · 409 `ACADEMICO_PERIODO_CERRADO` o `ACADEMICO_PERIODO_CON_VERSION` |
| `cambiarEstadoPeriodo` | `PUT /api/academico/periodos/{codigo}/estado` | `DISTRIBUTIVO:ESCRITURA` | 200 · 400 · 404 · 409 `ACADEMICO_PERIODO_CERRADO`, `ACADEMICO_TRANSICION_NO_PERMITIDA` o `ACADEMICO_PERIODO_SIN_VERSION` |

Todas responden también 401, 403, 500 y 503 de `contratos/comun/`. Las de escritura devuelven el `PeriodoAcademico` como queda.

## Errores nuevos (`contratos/comun/errores.md`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `ACADEMICO_CODIGO_REPETIDO` | 409 | Código repetido | Ya existe un periodo con ese código. Elige otro. |
| `ACADEMICO_PERIODO_CERRADO` | 409 | Periodo cerrado | El periodo está cerrado y ya no admite cambios. |
| `ACADEMICO_TRANSICION_NO_PERMITIDA` | 409 | Cambio de estado no permitido | El periodo solo pasa al estado siguiente, sin saltos ni retrocesos. |
| `ACADEMICO_PERIODO_SIN_VERSION` | 409 | Periodo sin versión | El periodo necesita su versión de normativa para salir de Planificación. Ábrelo antes. |
| `ACADEMICO_PERIODO_CON_VERSION` | 409 | Periodo con versión | El periodo ya tiene otra versión de normativa, y no se puede cambiar. |

## Lo que no cambia en Normativa

- `asociarPeriodo` y `consultarPeriodo` siguen iguales; la SPA deja de usar `asociarPeriodo` (D-69).
- Los puertos `AsociarVersionAPeriodo` y `ConsultarNormativa` no cambian: Académico los usa tal como están.
