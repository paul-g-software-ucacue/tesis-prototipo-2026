# Contrato: cambios en `contratos/cargas/openapi.yaml` y `contratos/comun/errores.md`

## Operación nueva

### `consultarPreparacionDelPeriodo` · `GET /api/cargas/preparacion`

- **Permiso**: `x-mdd-permiso: [CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` (D-70).
- **Parámetro** `periodo` (query, opcional): `CodigoDePeriodo` (`^[A-Za-z0-9._-]{1,20}$`, la regla de Académico). Sin él, el periodo que empieza más tarde.
- **200**: `PreparacionDelPeriodo`.
- **400**:
  - `COMUN_SOLICITUD_INVALIDA` si el periodo no cumple su formato;
  - `CARGAS_PERIODO_NO_ENCONTRADO` si no está en los ciclos académicos.
- **401, 403, 500, 503**: los comunes.

## Esquemas nuevos

- **`PreparacionDelPeriodo`:**
  - `periodos`: `PeriodoParaElegir[]`, del que empieza más tarde al más antiguo; obligatorio, puede estar vacío;
  - `periodo`: `CodigoDePeriodo`, opcional (no va sin periodos);
  - `abierto` y `cerrado`: booleanos opcionales;
  - `cargas`: `CargaDelPeriodo[]`, obligatorio; 10 elementos, o 0 sin periodos;
  - `faltan`: `LoQueFalta[]`, obligatorio;
  - `listo`: booleano, obligatorio.
- **`PeriodoParaElegir`:** `{ codigo: CodigoDePeriodo, nombre: string }`.
- **`CargaDelPeriodo`:**
  - `numero`, `codigo`, `nombre`, `obligatoriedad` (`Obligatoriedad`) y `estado` (`EstadoDeLaCarga`), obligatorios;
  - `motivo`: string opcional, el código del catálogo del último rechazo.
- **`EstadoDeLaCarga`** (F01-H05) suma `CON_ERRORES`; `consultarEstadoDeLasCargas` no lo responde.
- **`LoQueFalta`:**
  - `tipo`: enum `CARGA`, `PERIODO_SIN_ABRIR`, `PERIODO_CERRADO` o `SIN_PERIODOS`; obligatorio;
  - `carga`: `CargaQueFalta`, solo en `CARGA`;
  - `estado`: `EstadoDeLaCarga`, solo en `CARGA`.

## Catálogo (`contratos/comun/errores.md`)

Sin códigos nuevos. La columna «Causa» de `CARGAS_PERIODO_NO_ENCONTRADO` suma: «… o pedir el tablero de preparación de un periodo con un código bien escrito que Académico no tiene (F02-H02)».
