# Data Model: Reporte individual del docente en PDF y CSV

F20-H01 (#268). Los nombres siguen el glosario (`sdd/modelo_dominio.md` §7, principio III).

## Generación: el contenido congelado (nuevo)

Se guarda una vez por versión, cuando Aprobación la congela (research §2). Nunca cambia.

### Tabla `generacion.contenido_congelado` (migración V10)

| Columna | Tipo | Regla |
|---|---|---|
| `version_id` | uuid | Clave primaria; referencia a `version_distributivo`, con borrado en cascada |
| `periodo` | varchar(20) | Junto con `numero`, único |
| `numero` | integer, ≥ 1 | El número de la versión |
| `hash` | char(64) | El hash de la versión (`hashDelDistributivo`), en hexadecimal |
| `guardado_en` | timestamptz | Cuándo se guardó |
| `tardio` | boolean | `true` si se calculó al pedirlo y no al congelar (research §2) |

### Tabla `generacion.carga_congelada` (migración V10)

| Columna | Tipo | Regla |
|---|---|---|
| `version_id` | uuid | Referencia a `contenido_congelado`, con borrado en cascada |
| `docente` | varchar(13) | La identificación del docente; clave `(version_id, docente)` |
| `nombre` | varchar(200) | Sus apellidos y nombres, como en la asignación |
| `contenido` | jsonb | `CargaCongelada` sin la identificación ni el nombre (abajo) |

- **Cuelga de la versión, no de (periodo, número):** la limpieza de los scripts de demostración borra y recrea el distributivo de un periodo; con la cascada, el contenido se borra con su versión y nunca queda uno de otra versión con el mismo número. Ajuste respecto del plan, hecho al implementar.
- **Solo de inserción:** un disparador rechaza todo `UPDATE` (principio VII), como el de la bitácora.

### `CargaCongelada` (objeto de valor, nuevo)

| Campo | Tipo | Regla |
|---|---|---|
| `identificacion` | String | No vacío |
| `nombre` | String | No vacío |
| `dedicacion` | `Dedicacion` o vacío | TC, MT o TP, de Docentes al congelar; vacío si ya no estaba activo |
| `horasContratadas` | int o vacío | De 1 a 40 |
| `asignaturas` | lista de `AsignaturaCongelada` | Por carrera, código y paralelo |
| `actividades` | lista de `ActividadCongelada` | Por componente y nombre |
| `horas` | `HorasPorComponente` | Su carga horaria: los cinco componentes |

- `AsignaturaCongelada`: `codigo`, `nombre`, `paralelo`, `carrera` (el código), `nombreCarrera` y `horas` (las de clase, de la oferta al congelar).
- `ActividadCongelada`: `codigo`, `nombre`, `componente`, `carrera` (la del parcial que la registró), `proyecto` y `nombreProyecto` (opcionales) y `horas`.
- **Invariante:** la suma de las horas de las asignaturas es `horas.clase`, y la de las actividades de cada componente es `horas` de ese componente. Lo calcula `CargaHoraria.calcular`, la misma regla de RF-27; `CargaCongelada` solo lo comprueba.

### Puertos de Generación

| Puerto | Tipo | Qué hace |
|---|---|---|
| `GuardarContenidoCongelado.guardar(periodo, version)` | entrada (nuevo) | Por orden de Aprobación: calcula y guarda si no está. Si la versión vigente no es esa o no está CONGELADA, no hace nada y lo anota. Idempotente. |
| `ConsultarContenidoCongelado.cargas(periodo, version)` | entrada (nuevo) | El hash y la lista de docentes (identificación y nombre). Si falta, lo calcula y lo guarda como `tardio`. |
| `ConsultarContenidoCongelado.carga(periodo, version, docente)` | entrada (nuevo) | El hash y la `CargaCongelada` del docente; vacío si no está en la versión. |
| `RepositorioContenidoCongelado` | salida (nuevo) | `guardar` (insertar si no existe), `cabecera`, `docentes` y `carga` |
| `RepositorioDistributivos.version(periodo, numero)` | salida (nuevo método) | Una versión por su número, para calcular una que no es la vigente (el caso tardío durante una reforma) |

## Validación OCS: la publicación vigente (sin cambios en el modelo)

- `ProcesoAprobacion.publicacion()` ya da la última transición a PUBLICADO: la versión, el hash, quién y cuándo (D-111).
- **Puerto de entrada nuevo:** `ConsultarPublicaciones.vigentes()` devuelve una `PublicacionVigente` por periodo con distributivo publicado:
  - `periodo`, `version`, `hash`, `publicadaPor` y `publicadaEn`;
  - `enReforma`, `true` si hay una reforma en curso (spec 056).
- **Oyente nuevo:** `ContenidoDeLaFirma`, en los adaptadores de salida, recibe `AvisoDeFirma.DistributivoEnFirma` y llama a `GuardarContenidoCongelado`, como `PreparacionDelDocumento` llama a Firma.

## Reporting: el reporte (nuevo)

Reporting no guarda nada: su esquema sigue sin tablas.

### `ReporteIndividual` (agregado de lectura, del dominio de Reporting)

| Campo | Tipo | Regla |
|---|---|---|
| `publicacion` | `Publicacion` | `periodo`, `version`, `hash`, `publicadaEn`, `publicadaPor`, `enReforma` |
| `docente` | `DatosDelDocente` | `identificacion`, `nombre`, `dedicacion` (o «sin dato») y `horasContratadas` |
| `asignaturas` | lista de `FilaDeAsignatura` | `carrera` (nombre), `codigo`, `nombre`, `paralelo`, `horas` |
| `actividades` | lista de `FilaDeActividad` | `componente`, `codigo`, `nombre`, `unidad` (la carrera o el proyecto), `horas` |
| `horas` | `HorasPorComponente` | Los cinco componentes, en el orden del glosario, y `total()` |
| `generadoPor`, `generadoEn` | String, Instant | Solo para el PDF y la pantalla, no para el CSV (FR-010) |

- **Invariantes** (las comprueba `ReporteIndividual.de`, sin infraestructura):
  - el hash de la publicación es el del contenido congelado; si no, `ContenidoInconsistente` (research §3);
  - los totales por componente son la suma de sus filas;
  - las horas tienen como máximo dos decimales y no son negativas.
- **El orden** de las filas lo fija el agregado, así el PDF, el CSV y la pantalla lo comparten.

### `EscritorCsv` (dominio de Reporting)

Recibe un `ReporteIndividual` y devuelve los bytes del CSV (research §5). Su comportamiento:
- neutraliza las fórmulas;
- escribe la coma decimal con dos decimales;
- pone el BOM, el separador `;` y el fin de línea CRLF.

### Puertos de Reporting

| Puerto | Tipo | Qué hace |
|---|---|---|
| `ConsultarReportes.publicaciones()` | entrada | Los periodos con publicación vigente, de la más reciente a la más antigua |
| `ConsultarReportes.docentes(periodo)` | entrada | La publicación y sus docentes; `SinPublicacion` si no hay |
| `ConsultarReportes.reporte(periodo, identificacion, quien)` | entrada | El `ReporteIndividual`; `SinPublicacion` o `DocenteSinDistributivo` |
| `DescargarReporte.descargar(periodo, identificacion, formato, quien)` | entrada | Genera el archivo, registra la descarga y devuelve los bytes, el tipo y el nombre |
| `PublicacionesDelDistributivo` | salida | Lee Aprobación (`/interno/validacionocs/publicaciones`) |
| `ContenidoDeLaVersion` | salida | Lee Generación (`/interno/generacion/…/cargas`) |
| `GeneradorDePdf` | salida | El PDF/A (`GeneradorDeReportePdfa`) |
| `RegistroDeDescargas` | salida | Publica `reporting.EntradaBitacora` en la transacción (`RegistroDeDescargasEventos`) |
| `Transaccion` | salida | La transacción del registro persistente de publicaciones |

### Errores del dominio y de la aplicación

| Excepción | Código | Estado |
|---|---|---|
| `SinPublicacion` | `REPORTING_SIN_PUBLICACION` | 409 |
| `DocenteSinDistributivo` | `REPORTING_DOCENTE_SIN_DISTRIBUTIVO` | 404 |
| `ContenidoInconsistente` | `REPORTING_CONTENIDO_INCONSISTENTE` | 500 |

Los traduce `ErroresReporting`, en los adaptadores de entrada.

## La entrada de la bitácora

| Campo | Valor |
|---|---|
| tipo del evento | `reporting.EntradaBitacora` |
| `servicio` | `reporting` |
| `accion` | `REPORTE_INDIVIDUAL_DESCARGADO` |
| `usuario`, `momento` | Del token y del reloj |
| `periodo` | El del reporte |
| `objeto` | `{ "periodo", "version", "identificacion" }` |
| `nuevo` | `{ "formato": "PDF" }` o `{ "formato": "CSV" }` |
| `anterior` | ausente |

- No lleva el nombre del docente ni sus horas.
- La identificación es el identificador del objeto, como en las entradas de Generación, y se puede buscar con la consulta de F17-H02.
