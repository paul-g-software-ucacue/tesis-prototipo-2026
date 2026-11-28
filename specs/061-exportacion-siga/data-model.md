# Data Model: Validar antes de exportar y registrar la exportación

F19-H02 (#267). Lo nuevo y lo que cambia, por servicio. Los nombres del dominio salen del glosario (principio III):
- «Exportación a SIGA» es `ExportacionSiga`.
- «Suma de verificación» entra al glosario antes del código (T003).
- «Paso de la cadena» e «Inconsistencia» ya existen.

## Reporting · dominio (`ec.mdd.reporting.dominio`)

### `FirmasDeLaVersion` (nuevo, objeto de valor)

Lo que Firma dice de la cadena del documento de una versión (research §1).

| Campo | Tipo | Regla |
|---|---|---|
| `hashContenido` | `String` | `^[0-9a-f]{64}$` |
| `pasos` | `List<PasoFirmado>` | Copia inmutable, por orden. `PasoFirmado(int orden, boolean firmado)`, con orden desde 1 y sin repetir |
| `anulada` | `boolean` | Sus firmas se anularon (F12-H04) |
| `rechazada` | `boolean` | Se rechazó en firma (D-105) |

- `firmados()` y `total()`: los números para el mensaje.
- `completas(String hashPublicado)`: se cumplen las cuatro condiciones de research §1.

### `InconsistenciasAbiertas` (nuevo, objeto de valor)

Son las de la versión al congelarla (research §2).

| Campo | Tipo | Regla |
|---|---|---|
| `duras` | `int` | ≥ 0 |
| `blandas` | `int` | ≥ 0, las blandas sin justificar (MD-07) |

- `ninguna()`: `duras + blandas == 0`.

### `MotivoDeRechazo` (nuevo, enumerado)

`SIN_DOCUMENTO_FIRMADO`, `DOCUMENTO_DE_OTRA_VERSION`, `FIRMAS_INCOMPLETAS`, `FIRMAS_ANULADAS_O_RECHAZADAS`, `INCONSISTENCIAS_ABIERTAS` e `INCONSISTENCIAS_SIN_REGISTRO`.

### `CondicionesDeExportacion` (nuevo, objeto de valor)

- **Cómo se arma:** `CondicionesDeExportacion.de(Publicacion, Optional<FirmasDeLaVersion>, Optional<InconsistenciasAbiertas>)`.
- **Las firmas:**
  - sin firmas, `SIN_DOCUMENTO_FIRMADO`;
  - con un hash distinto del de la publicación, `DOCUMENTO_DE_OTRA_VERSION`;
  - anuladas o rechazadas, `FIRMAS_ANULADAS_O_RECHAZADAS`;
  - sin pasos, o con algún paso sin firmar, `FIRMAS_INCOMPLETAS`.
- **Las inconsistencias:**
  - sin el registro, `INCONSISTENCIAS_SIN_REGISTRO`;
  - con alguna abierta, `INCONSISTENCIAS_ABIERTAS`.
- **`motivos()`:** todos los motivos, en ese orden, sin cortar en el primero.
- **`exigir()`:** si hay motivos, lanza `ExportacionRechazada`.
- **Los números** (firmados de cuántos, duras y blandas) se conservan para el mensaje y la bitácora.

### `ExportacionRechazada` (nuevo, error de dominio)

- **Lleva:** `periodo`, `version` y `CondicionesDeExportacion`.
- **La traduce** `ErroresReporting` a `REPORTING_VERSION_NO_EXPORTABLE` (409).
- **El mensaje** sale de los motivos (research §8).

### `SumaDeVerificacion` (nuevo, objeto de valor)

| Campo | Tipo | Regla |
|---|---|---|
| `valor` | `String` | `^[0-9a-f]{64}$` |

- `de(byte[] contenido)`: el SHA-256 de los bytes, en hexadecimal en minúsculas. Usa `MessageDigest` del JDK.

### `ExportacionSiga` (nuevo, agregado)

Una exportación entregada (RF-53, glosario). No cambia después de crearse.

| Campo | Tipo | Regla |
|---|---|---|
| `id` | `UUID` | — |
| `usuario` | `String` | No vacío, hasta 64 |
| `momento` | `Instant` | — |
| `periodo` | `String` | `^[A-Za-z0-9._-]{1,20}$` |
| `version` | `int` | ≥ 1 |
| `hashVersion` | `String` | `^[0-9a-f]{64}$`, el de la publicación |
| `layout` | `int` | ≥ 0; 0 es el layout por defecto |
| `nombre` | `String` | El de `ArchivoSiga.nombreDe` |
| `filas` | `int` | ≥ 0, las filas del archivo sin el encabezado |
| `suma` | `SumaDeVerificacion` | — |

- **Cómo se crea:** `ExportacionSiga.de(Publicacion, LayoutExportacion, ArchivoSiga, FilasParaSiga, String quien, Instant momento)`. La suma sale de `archivo.contenido()`.

### `CsvExportado` (nuevo, objeto de valor)

- **Qué es:** lo que devuelve el caso de uso, `(ArchivoSiga archivo, ExportacionSiga exportacion)`. El controlador escribe los bytes, el nombre y el encabezado `X-Suma-Verificacion`.

### `CsvGenerado` (se quita)

La entrada `CSV_SIGA_GENERADO` se arma desde `ExportacionSiga` (research §5).

### `RechazoDeExportacion` (nuevo, objeto de valor de la bitácora)

| Campo | Tipo | Regla |
|---|---|---|
| `usuario` | `String` | — |
| `momento` | `Instant` | — |
| `periodo` | `String` | — |
| `version` | `int` | — |
| `condiciones` | `CondicionesDeExportacion` | Con al menos un motivo |

## Reporting · puertos

| Puerto | Tipo | Contrato |
|---|---|---|
| `GenerarCsvSiga` | entrada (cambia) | `CsvExportado generar(periodo, quien)`. Lanza `SinPublicacion`, `ExportacionRechazada` o `ContenidoInconsistente`. El registro y la entrada quedan guardados antes de devolver |
| `ConsultarExportacionesSiga` | entrada (nuevo) | `List<ExportacionSiga> delPeriodo(periodo)`, de la más reciente a la más antigua |
| `FirmasDeLasVersiones` | salida (nuevo) | `Optional<FirmasDeLaVersion> firmas(periodo, version)`. Vacío si Firma responde 404; lanza el error de proceso si no responde |
| `ContenidoDeLaVersion` | salida (cambia) | `cargas(periodo, version)` devuelve `CargasDeLaVersion` con `Optional<InconsistenciasAbiertas>` |
| `ExportacionesSiga` | salida (nuevo) | `guardar(ExportacionSiga)` y `delPeriodo(periodo)` |
| `RegistroEnBitacora` | salida (cambia) | `registrar(ExportacionSiga)` reemplaza a `registrar(CsvGenerado)`; suma `registrar(RechazoDeExportacion)` |

## Reporting · base (`reporting.exportacion_siga`, V3)

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `uuid` | Clave primaria |
| `periodo` | `varchar(20)` | `check (periodo ~ '^[A-Za-z0-9._-]{1,20}$')` |
| `version` | `integer` | `check (version >= 1)` |
| `hash_version` | `char(64)` | `check (hash_version ~ '^[0-9a-f]{64}$')` |
| `layout` | `integer` | `check (layout >= 0)` |
| `nombre` | `varchar(80)` | No vacío |
| `filas` | `integer` | `check (filas >= 0)` |
| `sha256` | `char(64)` | `check (sha256 ~ '^[0-9a-f]{64}$')` |
| `exportada_por` | `varchar(64)` | No vacío |
| `exportada_en` | `timestamptz` | — |

- **El índice:** `exportacion_siga_del_periodo` sobre `(periodo, exportada_en desc)`.
- **Sin cambios:** el disparador `exportacion_siga_sin_cambios`, `before update or delete`, lanza `insufficient_privilege`, como `layout_exportacion_sin_cambios` (principio VII).

## Firma (`ec.mdd.firma`)

- **Puerto de entrada nuevo** `ConsultarFirmasDeLaVersion`: `Optional<FirmasDelDocumento> firmas(periodo, version)`, del documento CONSOLIDADO de esa versión. Lo implementa `DocumentoFirmableServicio`, con `RepositorioDocumentos.documento(periodo, version)` y `firmas(id)`, que ya existen.
- **El adaptador de entrada nuevo** `InternoFirmaControlador` traduce `FirmasDelDocumento` a `FirmasDeLaVersion` del contrato:
  - los pasos, de `cadenaConEstado()`: `FIRMADO` como tal; `EN_TURNO` y `PENDIENTE` como `PENDIENTE`; `RECHAZADO` como tal;
  - `anulada`, de `anulacion().isPresent()`;
  - `rechazada`, de `rechazo().isPresent()`.
- **No cambian** el esquema ni el dominio.

## Generación (`ec.mdd.generacion`)

- **`ContenidoCongelado` (amplía):** `Optional<InconsistenciasAlCongelar>`, con `duras` y `blandas` (≥ 0).
- **`GuardarContenidoCongeladoServicio`:** recibe `ConsultarPanel`. Antes de la transacción, pide el panel del periodo. Si `panel.numero()` es la versión que se guarda, toma `abiertas(DURA)` y `abiertas(BLANDA)`; si no, queda vacío.
- **Migración V11** (`V11__inconsistencias_al_congelar.sql`):
  - `alter table contenido_congelado add column duras_abiertas integer check (duras_abiertas >= 0), add column blandas_abiertas integer check (blandas_abiertas >= 0)`;
  - `check ((duras_abiertas is null) = (blandas_abiertas is null))`.
  - El disparador contra `update` sigue igual: las columnas se llenan en el `insert`.
- **`consultarContenidoCongelado`:** suma `inconsistenciasAbiertas` si están.

## Las transiciones

No hay estados nuevos. Una `ExportacionSiga` existe o no existe.

**Cada pedido del CSV termina de una de cuatro formas:**

| Resultado | Qué queda |
|---|---|
| Rechazado por `SinPublicacion` | Nada |
| Rechazado por `CondicionesDeExportacion` | Una entrada `EXPORTACION_SIGA_RECHAZADA`, si se pudo guardar |
| Falla técnica (contenido, totales, proceso) | Nada |
| Entregado | Una `ExportacionSiga` y su entrada `CSV_SIGA_GENERADO`, guardadas juntas |
