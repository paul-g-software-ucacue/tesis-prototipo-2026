# Data Model: Reporte general consolidado en PDF y CSV

F20-H02 (#269). No hay tablas nuevas ni migraciones:
- **Reporting** sigue sin tablas.
- **Generación** lee lo que ya guarda en `carga_congelada.contenido` (V10, F20-H01).
- **La bitácora** usa el esquema `eventos_salida`, que ya existe.

## Reporting · dominio (`ec.mdd.reporting.dominio`)

### `ReporteGeneral` (nuevo, agregado de lectura)

El distributivo consolidado de la institución en una versión publicada (RF-46). No se guarda. El PDF, el CSV y la pantalla salen de él, con el mismo orden y los mismos datos (RF-48).

| Campo | Tipo | Regla |
|---|---|---|
| `publicacion` | `Publicacion` (existente) | Obligatoria |
| `docentes` | lista de `FilaDelDocente` | Ordenada por `nombre` en orden alfabético del español (`Collator` es-EC, `ReporteGeneral.porNombre()`) y luego por `identificacion`; sin identificaciones repetidas. **Ajuste al implementar:** `String.compareTo` ponía «ÁLVAREZ» después de «PERALTA» |
| `totales` | `HorasPorComponente` (existente) | La suma exacta de las horas de las filas, componente por componente |
| `generadoPor` | `String` | Obligatorio; solo para el PDF y la pantalla |
| `generadoEn` | `Instant` | Obligatorio; solo para el PDF y la pantalla |

**La fábrica `ReporteGeneral.de(publicacion, contenido, quien, momento)`**, con `contenido` de tipo `DocentesDeLaVersion`:
1. Si `contenido.hash()` no es `publicacion.hash()`, lanza `ContenidoInconsistente`.
2. Si dos docentes tienen la misma identificación, lanza `ContenidoInconsistente`.
3. Ordena las filas y calcula los totales con `HorasPorComponente.mas`, empezando de `NINGUNA`.

**Las consultas derivadas:**
- `cantidadDeDocentes()`, el tamaño de `docentes`;
- `totales().total()`, el total general.

**Invariantes que prueba `ReporteGeneralTest`:**
- el orden por nombre y, a igual nombre, por identificación;
- los totales iguales a la suma de las filas en cada componente;
- el total general igual a la suma de los totales de las filas;
- un componente en 0 en toda la institución;
- las horas con dos decimales, sin redondear;
- el hash distinto y el docente repetido, cada uno con `ContenidoInconsistente`;
- una versión sin docentes, con los totales en 0.

### `FilaDelDocente` (nuevo, objeto de valor)

| Campo | Tipo | Regla |
|---|---|---|
| `identificacion` | `String` | Obligatoria |
| `nombre` | `String` | Obligatorio |
| `dedicacion` | `Optional<Dedicacion>` (existente) | Vacía si el docente no estaba activo al congelar; «sin dato» en el reporte |
| `horas` | `HorasPorComponente` | Obligatorias; su `total()` es el total del docente |

### `DocentesDeLaVersion.Docente` (cambia)

Suma `dedicacion` (opcional) y `horas` (obligatorias), que llegan de Generación (research §1). El listado público de docentes (`listarDocentesDelPeriodo`) sigue mostrando solo la identificación y el nombre. Por eso `RespuestasDeReporting` mapea solo esos dos campos.

### `HorasPorComponente` (cambia)

Suma `mas(HorasPorComponente otras)`: una instancia nueva con la suma de cada componente. Mantiene la validación de `Horas.validas`: la suma de horas no negativas, con hasta dos decimales, también lo es.

### `DescargaDeReporte` (cambia)

| Campo | Tipo | Regla |
|---|---|---|
| `tipo` | `TipoDeReporte` (nuevo) | `INDIVIDUAL` o `GENERAL` |
| `usuario`, `momento`, `periodo`, `version`, `formato` | sin cambios | Obligatorios |
| `identificacion` | `String` | Obligatoria si el tipo es `INDIVIDUAL`; debe faltar (`null`) si es `GENERAL` |

Las dos fábricas, `DescargaDeReporte.individual(...)` y `DescargaDeReporte.general(...)`, evitan pasar un `null` a mano.

### `TipoDeReporte` (nuevo, enumerado)

| Valor | La acción de la bitácora | El nombre del archivo |
|---|---|---|
| `INDIVIDUAL` | `REPORTE_INDIVIDUAL_DESCARGADO` | `reporte-individual-<periodo>-<identificacion>.<ext>` |
| `GENERAL` | `REPORTE_GENERAL_DESCARGADO` | `reporte-general-<periodo>.<ext>` |

### `EscritorCsv` (cambia)

Suma `escribir(ReporteGeneral)`, que reutiliza las piezas que ya tiene: `texto` (con la neutralización de fórmulas), `horas` y `linea` ([contracts/reporting.md](contracts/reporting.md) §CSV).

## Reporting · puertos

| Puerto | Cambio |
|---|---|
| `ConsultarReportes` (entrada) | Suma `ReporteGeneral reporteGeneral(String periodo, String quien)`. Lanza `SinPublicacion` o `ContenidoInconsistente` |
| `DescargarReporte` (entrada) | Suma `ArchivoDelReporte descargarGeneral(String periodo, Formato formato, String quien)` |
| `GeneradorDePdf` (salida) | Suma `byte[] generar(ReporteGeneral reporte)` |
| `ContenidoDeLaVersion` (salida) | Sin cambios de firma: `docentes(periodo, version)` devuelve ahora docentes con dedicación y horas |
| `RegistroDeDescargas` (salida) | Sin cambios de firma; recibe la `DescargaDeReporte` con su tipo |

## Generación (`ec.mdd.generacion`)

### `DocenteDeLaVersion` (cambia, dominio)

| Campo | Tipo | Regla |
|---|---|---|
| `identificacion` | `String` | Sin cambios |
| `nombre` | `String` | Sin cambios |
| `dedicacion` | `Dedicacion` (la de `CargaCongelada`), opcional | Nuevo; de `contenido->>'dedicacion'` |
| `horas` | `CargaHoraria` (existente, la de `CargaCongelada`) | Nuevo; de `contenido->'horas'` |

**El repositorio.** `RepositorioContenidoCongeladoJdbc.docentes(periodo, numero)` lee esas dos claves del `jsonb`, en la misma consulta, con el mismo orden (por nombre y luego por identificación). **La tabla no cambia:** el contenido ya las guarda desde V10.

**El controlador.** `InternoGeneracionControlador` las expone en `DocenteCongelado` ([contracts/interno.md](contracts/interno.md)).

## La bitácora (`reporting.EntradaBitacora` → Usuarios)

| Campo | Individual (sin cambios) | General (nuevo) |
|---|---|---|
| `accion` | `REPORTE_INDIVIDUAL_DESCARGADO` | `REPORTE_GENERAL_DESCARGADO` |
| `objeto` | `{periodo, version, identificacion}` | `{periodo, version}` |
| `nuevo` | `{formato}` | `{formato}` |
| `usuario`, `momento`, `periodo` | del token y del reloj | igual |

La entrada no lleva nombres ni horas (principio IX).

## Estados

El reporte no tiene estados. Depende del estado del proceso de aprobación del periodo, que no cambia:

| Estado del periodo | Reporte general |
|---|---|
| Sin ninguna transición a PUBLICADO | 409 `REPORTING_SIN_PUBLICACION` |
| PUBLICADO | De la versión publicada |
| En reforma, después de una publicación | De la versión publicada anterior, con `enReforma: true` |
| PUBLICADO otra vez, después de la reforma | De la versión nueva |
