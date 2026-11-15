# Data Model: Rechazar en firma con una observación

## Firma

### `firma.rechazo_en_firma` (V6, nueva)

| Columna | Tipo | Regla |
|---|---|---|
| `periodo` | `varchar(20)` | Parte de la clave |
| `version` | `integer` | Parte de la clave |
| `tipo` | `varchar(20)` | Parte de la clave |
| `orden` | `integer` | El paso en turno al rechazar; `>= 1` |
| `rechazado_por` | `varchar(64)` | El usuario del firmante en turno |
| `rechazado_en` | `timestamptz` | |
| `observacion` | `varchar(500)` | `length(btrim(observacion)) between 10 and 500` |

- Clave primaria `(periodo, version, tipo)`: un rechazo por documento.
- Clave foránea a `generacion_documento`, como `validacion_firma` (D-103: `documento_firmable` no admite `update`, y la verificación de la clave lo exigiría).
- `revoke update, delete, truncate` del rol del proceso (principio VII).

### Dominio

- **`RechazoEnFirma`** (objeto de valor del dominio, `dominio/`): orden, quién, cuándo y la observación. La observación se recorta y debe tener de 10 a 500 caracteres; si no, `ObservacionInvalida` (→ `FIRMA_OBSERVACION_INVALIDA`).
- **`FirmasDelDocumento`** suma `Optional<RechazoEnFirma> rechazo`. Con rechazo, `pasoEnTurno()` está vacío. El constructor de cuatro componentes se conserva, sin rechazo.
- **`DocumentoFirmable.rechazarEnFirma`** no hace falta como método aparte: la operación del glosario es el caso de uso `RechazarEnFirma` sobre `FirmasDelDocumento`.

### Puertos

- **Entrada:**
  - `RechazarEnFirma.rechazar(UUID documento, String observacion, Solicitante quien)` → `RechazoRegistrado`;
  - el evento **`RechazoEnFirma`** (API publicada): periodo, versión, orden, rol, rechazado por, rechazado en, observación.
- **Salida:**
  - `RepositorioDocumentos.registrarRechazo(RechazoEnFirma, FirmasDelDocumento vistas)` → `boolean`: bloquea la generación, comprueba firmas iguales y sin rechazo, inserta;
  - `registrarValidacion` comprueba también que no hay rechazo antes de aceptar;
  - `AvisoDeRechazoEnFirma.avisar(RechazoEnFirma)`.

### Bitácora

`CambioDeFirma.RECHAZO_EN_FIRMA`: periodo, versión, orden y la observación en el detalle.

## Aprobación (Validación OCS)

### `validacionocs.transicion` (V5)

`transicion_tipo_check` suma `RECHAZO_EN_FIRMA`.

### `validacionocs.rechazo_en_firma` (V5, nueva)

| Columna | Tipo | Regla |
|---|---|---|
| `transicion_id` | `uuid` | Clave primaria; referencia a `transicion (id)` |
| `orden` | `integer` | `>= 1` |
| `rol` | `varchar(40)` | El rol del paso |
| `rechazado_por` | `varchar(64)` | |
| `rechazado_en` | `timestamptz` | |
| `observacion` | `varchar(500)` | `length(btrim(observacion)) between 10 and 500` |

Solo de inserción, como `observacion_de_devolucion`.

### Dominio

- **`TipoDeTransicion.RECHAZO_EN_FIRMA`**: de CONGELADO_EN_FIRMA a la etapa activa anterior, por `sistema`.
- **`RechazoDelFirmante`** (objeto de valor): orden, rol, quién, cuándo y la `Observacion`.
- **`Transicion`** suma `Optional<RechazoDelFirmante> rechazo`: presente solo en `RECHAZO_EN_FIRMA`, y siempre en ella.
- **`ProcesoAprobacion`**:
  - `rechazarEnFirma(int version, RechazoDelFirmante rechazo, Instant ahora)` → `List<Transicion>`: vacía si no está en CONGELADO_EN_FIRMA o la versión no es la congelada;
  - `anteriorActiva(TipoDeEtapa)`;
  - `rechazoPendiente()` → `Optional<Transicion>`: la última transición, si es un rechazo en firma;
  - `validarAprobacion` y `validarRevision` (si congela al avanzar) lanzan `RequiereVersionNueva` con un rechazo pendiente;
  - `acciones()` no ofrece APROBAR_CONTENIDO con un rechazo pendiente.

### Puertos

- **Entrada:** `RegistrarRechazoEnFirma.registrar(String periodo, int version, RechazoDelFirmante rechazo)`.
- **Adaptador de entrada:** `CierreDeLaFirma.alRechazar(RechazoEnFirma)`.

### Bitácora

`CambioDeFlujo.Accion.DISTRIBUTIVO_RECHAZADO_EN_FIRMA`, con la observación, el paso y quién rechazó.

## Estados

```text
Documento:  en firma (alguien en turno) ──rechazarEnFirma──▶ rechazado (nadie en turno, cerrado)
Flujo:      CONGELADO_EN_FIRMA ──RECHAZO_EN_FIRMA (sistema)──▶ EN_REVISION_VRA (anterior activa)
            EN_REVISION_VRA con rechazo pendiente: aprobar → 409 hasta la versión nueva (F12-H04)
```
