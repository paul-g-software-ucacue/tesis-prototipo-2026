# Data Model: Validar que el PDF subido es el mismo documento con una firma íntegra

## Firma (servicio 10, módulo `firma` del proceso `distributivo`)

### Migración V4 del esquema `firma`

```sql
create table validacion_firma (
    id             uuid        primary key,
    periodo        varchar(20) not null,
    version        integer     not null,
    tipo           varchar(20) not null,
    orden          integer     not null check (orden >= 1),        -- el paso en turno al subir
    subido_por     varchar(64) not null,
    subido_en      timestamptz not null,
    sha256_subido  char(64)    not null check (sha256_subido ~ '^[0-9a-f]{64}$'),
    tamano         integer     not null check (tamano > 0),
    aceptada       boolean     not null,
    comprobaciones jsonb       not null check (jsonb_typeof(comprobaciones) = 'array'),
    certificado    jsonb,                                            -- con una sola firma nueva
    foreign key (periodo, version, tipo) references generacion_documento (periodo, version, tipo)
);
create unique index validacion_firma_aceptada
    on validacion_firma (periodo, version, tipo, orden) where aceptada;
revoke update, delete, truncate on validacion_firma from current_user;
```

- **Una fila por subida** del firmante en turno, aceptada o rechazada (FR-015). Las del que no está en turno no dejan fila (FR-002).
- **La firma del paso** es la fila aceptada. El archivo de la firma es `documentos/<sha256_subido>.pdf`.
- **La clave foránea** apunta a `generacion_documento`, como `firmante_esperado` (research §1).
- **El rol del proceso** solo inserta y lee (principio VII).

### Valores del dominio (nuevos)

| Valor | Campos y reglas |
|---|---|
| `Comprobacion` (enum) | `MISMO_DOCUMENTO`, `UNA_FIRMA_NUEVA`, `INTEGRIDAD`, `CONFIANZA`, en ese orden (FR-008) |
| `Resultado` (enum) | `APROBADA`, `RECHAZADA`, `NO_EVALUADA` |
| `ResultadoDeComprobacion` | `comprobacion`, `resultado`, `motivo` (texto fijo del sistema; vacío si aprobada) |
| `CertificadoDeLaFirma` | `nombre` (CN), `emisor`, `serie` (hexadecimal), `validoDesde`, `validoHasta` |
| `ResultadoValidacion` | `id`, `documento`, `orden`, `subidoPor`, `subidoEn`, `sha256Subido`, `tamano`, las cuatro comprobaciones en orden, `certificado` opcional. `aceptada()` si las cuatro están aprobadas. `primerRechazo()` dice la comprobación del código del 422 |
| `FirmaRegistrada` | `orden`, `sha256`, `tamano`, `subidoPor`, `subidoEn`: la validación aceptada de un paso |

**Reglas del informe:**
- Exige las cuatro comprobaciones, una por cada valor del enum y en su orden.
- Una comprobación posterior a una rechazada de la que depende debe estar `NO_EVALUADA` (research §3).

### `FirmasDelDocumento` (de F14-H01, ampliado)

- **`firmas`:** la lista de `FirmaRegistrada`, por orden.
- **`pasoEnTurno()`:** usa los órdenes de `firmas`. El método con el conjunto explícito queda para las pruebas o se quita si nadie más lo usa.
- **`archivoVigente()`:** el archivo de la firma de mayor orden; sin firmas, el original.

### `ComprobacionDeDocumento` (dominio, nuevo)

`mismoDocumento(byte[] vigente, byte[] subido)` → `ResultadoDeComprobacion`: aprobada si el subido empieza con todos los bytes del vigente. Si es igual al vigente, también: la falta de firma nueva la rechaza la comprobación siguiente, con su propio código.

### Puertos

**Entrada:**
- `SubirFirma.subir(UUID documento, byte[] archivo, Solicitante quien)` → `ResultadoValidacion`. Si se rechaza, lanza `ArchivoFirmadoRechazado(informe)`.
- Lanza `SinTurnoDeFirma` (403), `ArchivoNoDisponible` (500), `ArchivoVacio` (400) y `ArchivoDemasiadoGrande` (413).

**Salida:**
- `ValidadorDeFirmas.validar(byte[] vigente, byte[] subido)` → `HallazgosDeLaFirma`: el resultado de una sola firma nueva, la integridad y la confianza, con el certificado si hay una sola firma nueva. El mismo documento lo decide el dominio antes.
- `RepositorioDocumentos`:
  - `firmas(UUID)` lee también las firmas registradas;
  - `registrarValidacion(ResultadoValidacion, Set<Integer> firmasEsperadas)` bloquea la generación, comprueba que las firmas no cambiaron y guarda (research §4). Devuelve si se registró como se pidió o se convirtió en rechazo.
- `AlmacenDeDocumentos.guardar(byte[], String sha256)`: ya existe (F13-H02), sin cambios. Nombra el archivo por su SHA-256, nunca sobrescribe, y si el nombre ya existe no escribe otra vez: es el mismo contenido.
- `RegistroDeCambios`: las acciones `FIRMA_ACEPTADA` y `FIRMA_RECHAZADA` (research §9).

### Excepciones nuevas

| Excepción | Código | HTTP |
|---|---|---|
| `ArchivoFirmadoRechazado` (con el informe) | `FIRMA_NO_ES_LA_VERSION_VIGENTE`, `FIRMA_SIN_UNA_FIRMA_NUEVA`, `FIRMA_NO_INTEGRA` o `FIRMA_SIN_CONFIANZA`, según `primerRechazo()` | 422 |
| `ArchivoVacio` | `FIRMA_ARCHIVO_VACIO` | 400 |
| `ArchivoDemasiadoGrande` | `FIRMA_ARCHIVO_DEMASIADO_GRANDE` | 413 |

### Configuración

- **`mdd.firma.raices`:** la lista de recursos PEM de la lista de confianza.
  - Por defecto: `classpath:firma/raices/ca-prueba.crt` y `classpath:firma/raices/bce-raiz.crt`.
  - En las pruebas: solo la CA de prueba.
  - Vacía o ilegible: el proceso no arranca (research §5).
  - Es la configuración de demostración. Producción la reemplaza, sin la CA de prueba (principio IX).
- **Los archivos** viven en `backend/servicios/firma/src/main/resources/firma/raices/`, con su excepción en `.gitleaks.toml` y en el hook de pre-commit (solo `.crt`).
- **El límite de 10 MB** lo comprueba Firma. El proceso ya corta a 11 MB con `COMUN_CONTENIDO_DEMASIADO_GRANDE`.

### Dependencias

`org.bouncycastle:bcpkix-jdk18on` pasa de las pruebas a `implementation`: la validación es código de producción (research §3). PDFBox ya está.
