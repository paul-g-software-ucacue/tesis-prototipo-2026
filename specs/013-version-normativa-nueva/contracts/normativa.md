# Contrato: Normativa (cambios de F04-H02)

Se cambia el contrato antes que el código (principio IV, ADR-015). Las operaciones de F04-H01 siguen igual, con los campos nuevos de abajo.

## Operaciones nuevas en `contratos/normativa/openapi.yaml`

| Operación | Ruta | `x-mdd-permiso` | Cuerpo | Respuestas |
|---|---|---|---|---|
| `crearVersion` | `POST /api/normativa/versiones` | `PARAMETROS:ESCRITURA` | `VersionNueva`: `copiarDe` (número), `codigo`, `origen`, `vigenteDesde` | 201 `VersionNormativa` · 400 · 404 · 409 `NORMATIVA_CODIGO_REPETIDO` |
| `editarVersion` | `PUT /api/normativa/versiones/{numero}` | `PARAMETROS:ESCRITURA` | `VersionEditada`: `codigo`, `origen`, `vigenteDesde`, `tablaCargaHoraria` (las filas con sus horas) y `reglas` (identificador y parámetros) | 200 `VersionNormativa` · 400 · 404 · 409 `NORMATIVA_VERSION_EN_USO` o `NORMATIVA_CODIGO_REPETIDO` |
| `asociarPeriodo` | `PUT /api/normativa/periodos/{periodo}` | `PARAMETROS:ESCRITURA` | `{ "version": n }` | 200 `PeriodoNormativo` · 400 · 404 · 409 `NORMATIVA_PERIODO_CON_VERSION` |
| `consultarPeriodo` | `GET /api/normativa/periodos/{periodo}` | `PARAMETROS:LECTURA` | — | 200 `PeriodoNormativo` · 404 |

- `periodo`: `^[A-Za-z0-9._-]{1,20}$`.
- `PeriodoNormativo`: `periodo`, `version` y `asociadaEn`.
- En `editarVersion`, la fila se identifica por `aplicaA` y `perfil`, y no cambian ni la severidad ni la referencia. La observación sí se puede cambiar.
- Todas responden además 401 `NoAutenticado`, 403 `AccesoNoAutorizado`, 500 y 503.

## Campos nuevos

| Esquema | Campo |
|---|---|
| `VersionResumen`, `VersionNormativa` | `copiadaDe` (opcional) y `periodos` (`[string]`, vacía si no rige ninguno) |

## `contratos/comun/errores.md`

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `NORMATIVA_VERSION_EN_USO` | 409 | Versión en uso | La versión rige al menos un periodo y ya no se puede cambiar. Crea una versión nueva a partir de ella. |
| `NORMATIVA_CODIGO_REPETIDO` | 409 | Código repetido | Ya existe una versión de normativa con ese código. Elige otro. |
| `NORMATIVA_PERIODO_CON_VERSION` | 409 | Periodo con versión | El periodo ya tiene una versión de normativa. |
