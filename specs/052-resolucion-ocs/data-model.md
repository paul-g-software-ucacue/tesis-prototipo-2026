# Data Model: Registrar la resolución del OCS

## Aprobación (Validación OCS)

### `validacionocs.resolucion_ocs` (V7, nueva)

| Columna | Tipo | Regla |
|---|---|---|
| `transicion_id` | `uuid` | Clave primaria; referencia a `transicion (id)`, `on delete cascade` |
| `periodo` | `varchar(20)` | Con `version`, único |
| `version` | `integer` | `>= 1` |
| `numero` | `varchar(60)` | `length(btrim(numero)) between 1 and 60` |
| `fecha` | `date` | |
| `sha256` | `char(64)` | `~ '^[0-9a-f]{64}$'` |
| `nombre` | `varchar(150)` | El nombre del archivo recibido, sin ruta |
| `tamano` | `integer` | `between 1 and 10485760` |

Solo de inserción, como `rechazo_en_firma` (V5).

### Dominio

- **`ResolucionOcs`** (objeto de valor): número (recortado, de 1 a 60; si no, `NumeroDeResolucionInvalido`), fecha, `sha256`, nombre y tamaño.
- **`Transicion`** suma `Optional<ResolucionOcs> resolucion`: presente solo en el `PASO_DE_ETAPA` de EN_OCS a APROBADO, y siempre en él. Se conservan los constructores de antes.
- **`ProcesoAprobacion`**:
  - `registrarResolucion(Solicitante quien, ResolucionOcs resolucion, Instant ahora)` → `List<Transicion>`. Exige:
    - la etapa EN_OCS (si no, `EtapaNoAdmiteResolucion`);
    - el rol de EN_OCS (si no, `RolDeOtraEtapa`, el 403 común);
    - la fecha entre el día de la entrada en EN_OCS y el de `ahora`, en `America/Guayaquil` (si no, `ResolucionFueraDeRango`);
  - `resolucion()` → `Optional<Transicion>`: la última transición con resolución;
  - `avanzar` desde EN_OCS lanza `RequiereResolucion`;
  - `acciones()` ofrece `REGISTRAR_RESOLUCION` al rol de EN_OCS en EN_OCS.
- **`AccionDelProceso.REGISTRAR_RESOLUCION`**.
- **`ArchivoDeResolucion`**: comprueba el PDF (inicio, fin y tamaño) y calcula su SHA-256; `ResolucionNoEsPdf`, `ResolucionDemasiadoGrande`.

### Puertos

- **Entrada:** `RegistrarResolucion.registrar(String periodo, String numero, LocalDate fecha, String nombre, byte[] archivo, Solicitante quien)` → el estado del flujo; `DescargarResolucion.descargar(String periodo)` → nombre y bytes.
- **Salida:** `AlmacenDeResoluciones.guardar(String sha256, byte[] contenido)` y `leer(String sha256)`.

### Bitácora

`CambioDeFlujo.Accion.RESOLUCION_REGISTRADA`: por quien registra, la etapa antes y después, y en el detalle la versión, el hash, el número, la fecha y la huella del PDF.

## Estados

```text
EN_OCS ──registrarResolucion (SECRETARIA_GENERAL, número, fecha, PDF)──▶ APROBADO
EN_OCS ──avance genérico──▶ 409 VALIDACIONOCS_REQUIERE_RESOLUCION
```
