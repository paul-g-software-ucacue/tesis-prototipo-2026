# Data Model: Crear una versión nueva y anular las firmas tras un rechazo en firma

## Distributivo (Generación)

### `generacion.version_distributivo` (V8)

| Columna | Tipo | Regla |
|---|---|---|
| `motivo` | `varchar(20)` | `RECHAZO_EN_FIRMA`; nulo solo en la versión 1: `check ((numero = 1) = (motivo is null))` |
| `creada_por` | `varchar(64)` | Nulo en las versiones que ya existían |
| `creada_en` | `timestamptz` | Nulo en las versiones que ya existían |

### `generacion.informe_sustentado` (V8)

- El índice único de `clave` pasa a `(version_id, clave)`: la versión nueva comparte el archivo del informe con la anulada (research §2).

### Copia a la versión nueva

| Tabla | Cómo |
|---|---|
| `parcial_carrera` | Mismo estado por carrera |
| `propuesta` | Iguales, con su momento y su autor |
| `actividad_no_lectiva` | Ids nuevos |
| `justificacion_inconsistencia` | Ids nuevos; mismo texto, autor y momento |
| `consolidacion` | Ids nuevos; mismo autor y momento, así que no queda desactualizado |
| `informe_sustentado` | Ids nuevos; la misma `clave` y la misma huella |

### Dominio

- **`MotivoDeVersion`** (nuevo, en `puertos/entrada`, la interfaz publicada): `RECHAZO_EN_FIRMA`. No va en el dominio: Aprobación lo usa al ordenar (principio V).
- **`EstadoDeVersion`**: ya tiene `ANULADA`.

### Puertos

- **`ParcialesDelDistributivo.crearVersionNueva(String periodo, int anulada, MotivoDeVersion motivo, Instant momento)`** → `Optional<VersionNuevaEnGeneracion>` (número y hash).
  - Vacío si la vigente no es la del aviso congelada ni la siguiente ya creada por ese motivo.
- **`RepositorioDistributivos.crearVersionNueva(UUID anulada, int numero, MotivoDeVersion, String quien, Instant momento)`**: anula y copia en la transacción en curso.

## Aprobación (Validación OCS)

### `validacionocs.transicion` (V6)

- `transicion_tipo_check` suma `VERSION_NUEVA`.

### Dominio

- **`TipoDeTransicion.VERSION_NUEVA`**: de la etapa a la que volvió a la misma, por `sistema`, con la versión nueva y su hash.
- **`ProcesoAprobacion`**:
  - `registrarVersionNueva(int anulada, int nueva, String hash, Instant ahora)` → `List<Transicion>`: solo si la última transición es el `RECHAZO_EN_FIRMA` de `anulada`; si no, vacía;
  - `rechazoVigente()` → `Optional<Transicion>`: el último `RECHAZO_EN_FIRMA` sin una congelación después;
  - `rechazoPendiente()` no cambia: con `VERSION_NUEVA` al final, queda vacío.

### Puertos

- **Salida `Distributivo.crearVersionNueva(String periodo, int anulada, Instant momento)`** → `Optional<VersionNuevaEnAprobacion>` (número y hash).
- **Salida `AvisoDeFirma.avisar(FirmasPorAnular)`**: periodo, versión, motivo, momento.
- **Adaptador `AnulacionDeLasFirmas`** (oyente): llama a `AnularFirmas` de Firma.

### Bitácora

- `CambioDeFlujo.Accion.DISTRIBUTIVO_VERSION_NUEVA`: la versión anulada, la nueva y el motivo.

## Firma

### `firma.anulacion_de_firmas` (V7, nueva)

| Columna | Tipo | Regla |
|---|---|---|
| `periodo`, `version`, `tipo` | | Clave primaria; clave foránea a `generacion_documento` |
| `motivo` | `varchar(20)` | `RECHAZO_EN_FIRMA` |
| `firmas_anuladas` | `integer` | `>= 0`: las aceptadas que tenía |
| `anulada_por` | `varchar(64)` | `sistema` |
| `anulada_en` | `timestamptz` | |

- `revoke update, delete, truncate` del rol del proceso (principio VII).

### Dominio

- **`AnulacionDeFirmas`** (objeto de valor): motivo, cuántas, quién y cuándo.
- **`FirmasDelDocumento`** suma `Optional<AnulacionDeFirmas> anulacion`. Con ella, `pasoEnTurno()` está vacío. Se conservan los constructores de antes.

### Puertos

- **Entrada (API publicada):**
  - `AnularFirmas.anular(String periodo, int version, MotivoDeAnulacion motivo, Instant momento)`, con `MotivoDeAnulacion` (`RECHAZO_EN_FIRMA`) en `puertos/entrada`;
  - el evento **`FirmasAnuladas`**: periodo, versión, cuántas, motivo, momento.
- **Salida:**
  - `RepositorioDocumentos.registrarAnulacion(...)` → `boolean`: bloquea la generación e inserta si no hay;
  - `registrarValidacion` y `registrarRechazo` comprueban también que no hay anulación;
  - `AvisoDeFirmasAnuladas.avisar(FirmasAnuladas)`.
- **`EstadoDelDocumento`** suma `Optional<Anulacion>`.

### Bitácora

- `CambioDeFirma.FIRMAS_ANULADAS`: periodo, versión, cuántas y el motivo.

## Estados

```text
Versión:   v1 CONGELADA ──rechazo en firma──▶ v1 ANULADA + v2 EDITABLE (motivo RECHAZO_EN_FIRMA)
Flujo:     CONGELADO_EN_FIRMA ──RECHAZO_EN_FIRMA──▶ EN_REVISION_VRA ──VERSION_NUEVA (v2)──▶ EN_REVISION_VRA
           EN_REVISION_VRA (v2) ──aprobar──▶ CONGELADO_EN_FIRMA (v2) → documento v2, F1 en turno
Firma:     documento v1 rechazado ──anularFirmas──▶ documento v1 anulado (firmas conservadas)
```
