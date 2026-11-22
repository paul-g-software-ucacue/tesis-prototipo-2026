# Data Model: Generar el CSV para SIGA con el layout configurable

F19-H01 (#266). Las decisiones están en [research.md](research.md).

**Lo que cambia en la base:**
- **Reporting:** su primera tabla, `reporting.layout_exportacion` (V2).
- **Generación:** sin migración. El JSON de `carga_congelada.contenido` (V10) suma la clave opcional `tipoIdentificacion`.
- **La bitácora:** usa el esquema `eventos_salida`, que ya existe.

## Reporting · dominio (`ec.mdd.reporting.dominio`)

### `LayoutExportacion` (nuevo, agregado)

El mapeo de columnas y catálogos del CSV para SIGA (glosario: «Layout de exportación»). Es inmutable: cada carga es uno nuevo.

| Campo | Tipo | Regla |
|---|---|---|
| `numero` | `int` | 0 para el layout por defecto; desde 1, el de cada carga, correlativo |
| `columnas` | lista de `ColumnaDelLayout` | De 1 a 30, en el orden del archivo; nombres sin repetirse (sin distinguir mayúsculas); al menos una de `IDENTIFICACION`, una de `COMPONENTE` y una de `HORAS` |
| `texto` | `String` | El archivo tal cual, sin el BOM |
| `sha256` | `String` | Del texto, en hexadecimal |
| `cargadoPor` | `String` | Vacío en el layout por defecto |
| `cargadoEn` | `Instant` | Vacío en el layout por defecto |

**La validación `LayoutExportacion.validar(BorradorDeLayout)`** devuelve `ValidacionDelLayout`: las columnas válidas o la lista de `ProblemaDelLayout`. Nunca lanza al primer problema (research §6). Con las columnas válidas se arma:
- `LayoutExportacion.porDefecto(texto, columnas)`;
- `LayoutExportacion.cargado(numero, texto, columnas, quien, momento)`.

**Las consultas derivadas:**
- `esPorDefecto()`;
- `encabezado()`: los nombres, en orden.

### `ColumnaDelLayout` (nuevo, objeto de valor)

| Campo | Tipo | Regla |
|---|---|---|
| `nombre` | `String` | `^[A-Za-z][A-Za-z0-9_]{0,63}$` |
| `campo` | `CampoDelLayout` | Obligatorio |
| `traduccion` | `Map<String, String>` ordenado | Vacío o, si `campo.traducible()`, con exactamente las claves de `campo.codigos()`. Cada valor cumple `^[A-Za-z0-9][A-Za-z0-9 ._-]{0,29}$`, no es `NA` (sin distinguir mayúsculas) y no se repite |

`valor(FilaSiga)` da el valor de la celda, tipado y sin formato de texto: `Optional<String>` (el código o el texto, traducido si hay traducción; vacío si no aplica), `BigDecimal` (las horas) o `LocalDate` (la fecha de publicación). Las convenciones de texto (mayúsculas, `NA`, decimales, fecha, comillas y fórmulas) las aplica el adaptador `ExportadorSigaCsv` (principio I).

### `CampoDelLayout` (nuevo, enumerado)

Los 15 campos de research §6, cada uno con:
- `clave()`: el nombre en el archivo (`tipo_identificacion`);
- `traducible()`;
- `codigos()`: el conjunto de códigos de su catálogo, solo si es traducible. `TIPO_IDENTIFICACION` → `CEDULA`, `PASAPORTE`; `DEDICACION` → `TC`, `MT`, `TP`; `COMPONENTE` → los cinco de `Componente`.
- `obligatorio()`: `IDENTIFICACION`, `COMPONENTE` y `HORAS`.

### `BorradorDeLayout`, `ProblemaDelLayout` y `LayoutInvalido` (nuevos)

- **`BorradorDeLayout`:** lo que el lector entendió del archivo, sin validar. Tiene la lista de columnas como textos (`nombre`, `campo` y `traduccion`, cada uno opcional) y los problemas de estructura del lector (claves desconocidas, tipos de nodo).
- **`ProblemaDelLayout(String columna, String regla)`:**
  - `columna`: `columnas[3]` o `columnas[3] (HORAS)`, o `archivo` para un problema de todo el archivo;
  - `regla`: qué falla y qué hacer, en español («El campo «horas_semanales» no existe. Usa uno de la lista de la plantilla.»).
- **`LayoutInvalido`:** el error de aplicación con los problemas, hasta 50 y «Hay N problemas más.».
- **`ArchivoIlegible`:** el YAML no se puede leer. Lleva la línea si se conoce, nunca el mensaje del lector.
- **`ArchivoDemasiadoGrande`** (adaptador de entrada): el archivo pasa de 256 KB. `SigaControlador` lo comprueba antes de leerlo, y `ErroresReporting` lo traduce a 413 `REPORTING_ARCHIVO_DEMASIADO_GRANDE` (principio XIII).

### `FilaSiga` (nuevo, objeto de valor)

Una fila del CSV antes del layout: los datos de un docente y una de sus asignaturas o actividades.

| Campo | Tipo | Regla |
|---|---|---|
| `publicacion` | `Publicacion` (existente) | Obligatoria |
| `docente` | `DatosDelDocente` (amplía) | Obligatorio |
| `componente` | `Componente` | Obligatorio; `CLASE` en una asignatura |
| `actividad` | `String` opcional | Solo en una actividad |
| `carrera` | `String` | El código de la carrera |
| `asignatura`, `paralelo` | `String` opcional | Solo en una asignatura |
| `proyecto` | `String` opcional | Solo en una actividad con proyecto |
| `horas` | `BigDecimal` | `Horas.validas` (no negativas, hasta 2 decimales) |

Las fábricas son `FilaSiga.de(publicacion, docente, FilaDeAsignatura)` y `FilaSiga.de(publicacion, docente, FilaDeActividad)`.

### `FilasParaSiga` (nuevo, agregado de lectura)

Las filas del CSV para SIGA de una versión publicada, comprobadas contra el reporte general. No se guarda ni conoce el formato del archivo: lo escribe el adaptador del puerto `ExportadorSiga` (principio I).

| Campo | Tipo | Regla |
|---|---|---|
| `publicacion` | `Publicacion` | Obligatoria |
| `filas` | lista de `FilaSiga` | En el orden de research §5 |

**La fábrica `FilasParaSiga.de(publicacion, CargasDeLaVersion, ReporteGeneral)`:**
1. Si el hash de las cargas no es el de la publicación, o el de alguna carga no es el de las cargas, lanza `ContenidoInconsistente`.
2. Si hay un docente repetido, lanza `ContenidoInconsistente`.
3. Arma las filas y las ordena.
4. Comprueba los totales (research §3). Si un docente del reporte general falta en las filas, o al revés, o si una suma por docente y componente no es igual, o si un total institucional no lo es, lanza `ContenidoInconsistente`. Compara con `compareTo`, sin redondear (Edge Cases).

**El archivo** lo escribe `ExportadorSigaCsv` (adaptador de salida), con el layout y el formato de research §4. No depende del momento ni de quién lo pide, así que dos archivos de las mismas filas y el mismo layout son idénticos byte a byte.

### `CeldaCsv` (nuevo, utilidad del dominio)

`neutralizada(valor)`: antepone `'` si el valor empieza con `=`, `+`, `-`, `@`, tabulación o CR. Se extrae de `EscritorCsv`, que la sigue usando sin cambiar su resultado (`EscritorCsvTest` no cambia). También la usa `ExportadorSigaCsv`.

### `CargasDeLaVersion` (nuevo, objeto de valor)

`(String hash, List<ContenidoDelDocente> cargas)`: lo que devuelve la ruta interna nueva (research §1).

### `DatosDelDocente` (amplía)

Suma `tipoIdentificacionAlCongelar`, de tipo `TipoDeIdentificacion` (`CEDULA` o `PASAPORTE`, nuevo enumerado), opcional, con la consulta `tipoIdentificacion()` como `Optional`. El reporte individual no lo muestra.

### `CsvGenerado` y `LayoutCargado` (nuevos, objetos de valor de la bitácora)

- **`CsvGenerado(usuario, momento, periodo, version, layout)`:** el CSV entregado, con la versión del layout de exportación (su número).
- **`LayoutCargado(usuario, momento, layout, columnas, sha256)`:** un layout nuevo.

Ninguno lleva nombres, identificaciones ni horas.

## Reporting · puertos

| Puerto | Cambio |
|---|---|
| `GenerarCsvSiga` (entrada, nuevo) | `ArchivoSiga generar(String periodo, String quien)`. `ArchivoSiga(byte[] contenido, String nombre)`, con `siga-{periodo}-v{version}.csv`; lo arma el adaptador de `ExportadorSiga` |
| `GestionarLayoutSiga` (entrada, nuevo) | `LayoutsSiga consultar()` (el vigente y los anteriores); `String plantilla()`; `LayoutExportacion cargar(byte[] archivo, String quien)` |
| `ContenidoDeLaVersion` (salida, amplía) | `Optional<CargasDeLaVersion> cargas(String periodo, int version)` |
| `LayoutsGuardados` (salida, nuevo) | `Optional<LayoutGuardado> vigente()`; `List<LayoutGuardado> todos()`, del más reciente al más antiguo; `int guardar(String texto, String sha256, String quien, Instant momento)`; `String porDefecto()`, el texto del recurso |
| `ExportadorSiga` (salida, nuevo) | `ArchivoSiga exportar(FilasParaSiga filas, LayoutExportacion layout)`. Es el puerto «Exportación a SIGA» de la tabla del principio I: el adaptador del prototipo es `ExportadorSigaCsv` (el CSV con el layout); una API de SIGA sería otro adaptador del mismo puerto |
| `LectorDeLayout` (salida, nuevo) | `BorradorDeLayout leer(byte[] archivo)`; lanza `ArchivoIlegible` |
| `RegistroEnBitacora` (salida, renombra `RegistroDeDescargas`) | Suma `registrar(CsvGenerado)` y `registrar(LayoutCargado)` |

`LayoutGuardado(numero, texto, sha256, cargadoPor, cargadoEn)` es la fila tal cual. El servicio la vuelve a validar al leerla: un texto que antes valía siempre vale, porque las reglas no dependen de datos externos.

## Reporting · base (`reporting.layout_exportacion`, V2)

| Columna | Tipo | Regla |
|---|---|---|
| `numero` | `integer` | `primary key`, `check (numero >= 1)` |
| `texto` | `text` | `not null`, `check (octet_length(texto) <= 262144)` |
| `sha256` | `char(64)` | `not null`, `check (sha256 ~ '^[0-9a-f]{64}$')` |
| `cargado_por` | `varchar(100)` | `not null` |
| `cargado_en` | `timestamptz` | `not null` |

Un disparador rechaza `update` y `delete`, como en `carga_congelada` (principio VII).

## Generación (`ec.mdd.generacion`)

| Tipo | Cambio |
|---|---|
| `TipoDeIdentificacion` (dominio, nuevo) | `CEDULA`, `PASAPORTE` |
| `DocenteActivo` (dominio) | Suma `tipoIdentificacion`, obligatorio |
| `CargaCongelada` (dominio) | Suma `tipoIdentificacionAlCongelar`, opcional; `ContenidoCongelado.de` lo toma del docente activo |
| `RepositorioContenidoCongelado` (puerto) | Suma `cargas(periodo, numero)`: todas las `CargaCongelada` de la versión, por nombre |
| `RepositorioContenidoCongeladoJdbc` | Escribe y lee la clave `tipoIdentificacion`; si falta, el tipo queda vacío |
| El cliente de Docentes | Lee `tipoIdentificacion` de `DocenteActivo` |
| `InternoGeneracionControlador` | `consultarContenidoCongelado`, con la misma comprobación de versión congelada y el mismo cálculo tardío de las otras dos rutas |

## Docentes (`ec.mdd.docentes`)

La consulta de los activos y el mapeo del controlador interno suman `tipoIdentificacion`, que ya está en `Identificacion`.

## Las transiciones

No hay estados. Cargar un layout válido agrega una fila, y la última rige. Un archivo rechazado no deja nada: ni fila ni entrada en la bitácora.

## Ajustes al implementar

- **`FilaDeAsignatura` y `FilaDeActividad`** (Reporting, existentes) suman los códigos que el CSV necesita y los reportes no muestran: `codigoDeCarrera` en las dos y `codigoDeProyecto` en la actividad, opcionales, con `codigoCarrera()` y `codigoProyecto()`. Los reportes los siguen construyendo con su constructor de siempre, sin códigos; el cliente de Generación los llena.
- **`DatosDelDocente`** conserva su constructor de cuatro datos para los reportes; el tipo de identificación va en uno de cinco.
- **`ValorDeColumna`** (nuevo, interfaz sellada): `Texto`, `Entero`, `Horas`, `Fecha` y `NO_APLICA`. Es lo que devuelve `ColumnaDelLayout.valor`; el adaptador lo escribe.
- **`ArchivoSiga`** (nuevo, dominio): los bytes y el nombre `siga-<periodo>-v<version>.csv`. Lo devuelven el puerto de salida y el de entrada.
- **`LayoutExportacion.huella(texto)`**: el SHA-256 del texto, público, para el servicio y la bitácora.
- **`LayoutVigente`** (aplicación, paquete): arma el layout vigente o el por defecto a partir del texto, para los dos casos de uso.
- **La plantilla se valida en `PlantillaDelLayoutTest`** (adaptadores), porque lee el recurso con `LectorDeLayoutYaml`; el dominio no depende del lector.
- **`ArchivoDemasiadoGrande`** queda en el adaptador de entrada, como dice la sección de errores.
