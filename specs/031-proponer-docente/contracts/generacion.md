# Contrato: cambios en `contratos/`

## `contratos/generacion/openapi.yaml`

### `crearDistributivo` · `POST /api/generacion/periodos/{periodo}/distributivo`

- **Permiso**: `DISTRIBUTIVO:ESCRITURA`.
- **201**: `Distributivo`: `{ periodo, version, carreras: string[] }`.
- **404**: `GENERACION_PERIODO_NO_ENCONTRADO`.
- **409**: `GENERACION_PERIODO_NO_LISTO` o `GENERACION_DISTRIBUTIVO_REPETIDO`.
- **400, 401, 403, 500, 502, 503, 504**: los comunes.

### `consultarDistributivo` · `GET /api/generacion/periodos/{periodo}/distributivo`

- **Permiso**: `[DISTRIBUTIVO:LECTURA, CARGAS:LECTURA]`.
- **200**: `Distributivo`. **404**: `GENERACION_DISTRIBUTIVO_NO_ENCONTRADO`.

### `proponerDocente` · `PUT /api/generacion/periodos/{periodo}/oferta/{asignatura}/{paralelo}/propuesta`

- **Permiso**: `OFERTA:ESCRITURA`.
- **Cuerpo**: `PropuestaNueva`: `{ docente, texto? }`. `docente`: `^[0-9A-Z]{1,13}$`. `texto`: de 10 a 500 caracteres, sin contar los espacios de los extremos.
- **200**: `Propuesta`, con `horasSuficientes`.
- **400**: `COMUN_SOLICITUD_INVALIDA`, también sin la justificación escrita, con el detalle del campo `texto`.
- **403**: otra carrera, o sin carrera.
- **404**: `GENERACION_PARALELO_NO_ENCONTRADO`.
- **409**: `GENERACION_PARALELO_RETIRADO`, `GENERACION_PERIODO_CERRADO`, `GENERACION_SIN_DISTRIBUTIVO`, `GENERACION_DOCENTE_NO_ACTIVO` o `GENERACION_PARCIAL_NO_EDITABLE`.
- **401, 500, 502, 503, 504**: los comunes.

### `quitarPropuesta` · `DELETE` en la misma ruta

- **Permiso**: `OFERTA:ESCRITURA`. **204**. **404**: `GENERACION_PROPUESTA_NO_ENCONTRADA`.

### `consultarParciales` · `GET /api/generacion/periodos/{periodo}/parciales`

- **Permiso**: los de la oferta, `[OFERTA:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, con `AlcanceDeCarrera`.
- **200**: `Parciales`: `{ creado (boolean), parciales: ParcialDeCarrera[] }`. Sin distributivo, `creado` es `false`.

### Esquemas

- **`ParcialDeCarrera`:** `{ carrera, estado, ofertadas, conPropuesta, completo, propuestas: Propuesta[] }`.
- **`Propuesta`:** `{ asignatura, paralelo, docente, nombreDocente, justificacion: TipoDeJustificacion, texto?, retirada (boolean), horasSuficientes (boolean) }`.
- **`TipoDeJustificacion`:** enum `AFINIDAD`, `DISPONIBILIDAD`, `EXCEPCION`.
- **`Sugerencia`** suma `bajoElMinimo: Candidato[]`, `distributivoCreado (boolean)` y `propuesta?: Propuesta`.

## Rutas internas (ADR-023)

| Contrato | Operación | Ruta | 200 | 404 |
|---|---|---|---|---|
| `contratos/cargas` | `consultarPeriodoListo`, nueva | `GET /interno/cargas/periodos/{periodo}/preparacion` | `{ abierto, cerrado, listo }` | El periodo no existe |
| `contratos/academico` | `consultarOfertaDelPeriodo`, nueva | `GET /interno/academico/periodos/{codigo}/oferta` | `ParaleloDeLaOferta[]` | El periodo no existe |
| `contratos/academico` | `consultarParaleloDeLaOferta`, ampliada | La misma | Con `periodoCerrado` | — |

## Catálogo (`contratos/comun/errores.md`), con sus 8 columnas (D-72) y en «tú» (D-74)

| Código | Estado | Título | Descripción | Clasificación |
|---|---|---|---|---|
| `GENERACION_PERIODO_NO_LISTO` | 409 | Periodo sin preparar | El periodo todavía no está listo para su distributivo. Revisa lo que falta en «Preparación del periodo». | Negocio |
| `GENERACION_DISTRIBUTIVO_REPETIDO` | 409 | Distributivo ya creado | Este periodo ya tiene su distributivo. | Negocio |
| `GENERACION_DISTRIBUTIVO_NO_ENCONTRADO` | 404 | Distributivo no encontrado | El periodo todavía no tiene distributivo. | Negocio |
| `GENERACION_SIN_DISTRIBUTIVO` | 409 | Sin distributivo | El periodo todavía no tiene distributivo. Pide al Vicerrector académico que lo cree. | Negocio |
| `GENERACION_PERIODO_CERRADO` | 409 | Periodo cerrado | El periodo está cerrado y ya no admite propuestas. | Negocio |
| `GENERACION_DOCENTE_NO_ACTIVO` | 409 | Docente no activo | Ese docente no está activo. Elige otro. | Negocio |
| `GENERACION_PARCIAL_NO_EDITABLE` | 409 | Parcial cerrado | Este parcial ya no admite cambios. | Negocio |
| `GENERACION_PROPUESTA_NO_ENCONTRADA` | 404 | Sin propuesta | Esta asignatura-paralelo no tiene una propuesta. | Negocio |
