# Research: Generar el CSV para SIGA con el layout configurable

F19-H01 (#266). Cada sección cierra una decisión del plan. Todas van a la decisión **D-119**, que se registra en `docs/decisiones.md` antes de implementar.

Lo que ya decidieron D-115 (F20-H01) y D-117 (F20-H02) no se repite, y esta historia lo reutiliza:
- la publicación vigente que da Aprobación;
- el contenido congelado por docente, guardado al congelar, y la comprobación del hash;
- el `ReporteGeneral` y sus totales;
- la neutralización de fórmulas del CSV;
- el registro en la bitácora antes de responder.

## §1. De dónde salen las filas

**El problema.** Una fila por docente y actividad necesita las asignaturas y las actividades de **todos** los docentes de la versión. Hoy Generación las da de a un docente (`consultarCargaCongelada`), y el listado (`consultarCargasCongeladas`) solo trae las horas por componente.

- **Decision:** una ruta interna nueva en Generación, `GET /interno/generacion/periodos/{periodo}/versiones/{numero}/contenido` (`consultarContenidoCongelado`). Devuelve el hash y la carga congelada completa de cada docente, con el esquema `CargaCongeladaDelDocente` que ya existe, por nombre. Lee las mismas filas de `carga_congelada` que las otras dos rutas, sin tabla ni migración nueva.
- **Rationale:**
  - **Una llamada:** con 300 docentes (SC-008), el CSV no hace 300 llamadas entre procesos.
  - **Una sola foto:** las filas salen de una sola lectura de la versión, sin mezclar respuestas de momentos distintos.
  - **El mismo esquema:** Reporting ya sabe leer `CargaCongeladaDelDocente` (`ContenidoDesdeGeneracion.contenido`), así que se reutiliza su mapeo.
- **Alternatives considered:**
  - **Una llamada por docente:** N llamadas por CSV, contra SC-008.
  - **Ampliar el listado con las asignaturas y las actividades:** el reporte general (D-117) las descartaría todas, y la respuesta del listado sería unas diez veces más grande para todos sus usos.

## §2. El tipo de identificación

**El problema.** FR-004 pide el tipo de identificación (cédula o pasaporte), y la regla de los 10 dígitos aplica solo a la cédula (Edge Cases). El contenido congelado no lo guarda: solo tiene el número.

- **Decision:** el contenido congelado guarda el tipo desde esta historia.
  - **Docentes:** el esquema interno `DocenteActivo` (`/interno/docentes/activos`) suma `tipoIdentificacion` (`CEDULA` o `PASAPORTE`), obligatorio. Docentes ya lo tiene en su dominio (`Identificacion`).
  - **Generación:** `DocenteActivo` lo recibe. `CargaCongelada` lo guarda como `tipoIdentificacionAlCongelar`, opcional, igual que la dedicación. Va en la clave `tipoIdentificacion` del JSON de `carga_congelada.contenido`.
  - **La ruta interna:** `DatosDelDocenteCongelado` suma `tipoIdentificacion`, opcional.
  - **Reporting:** si falta, la columna sale «NA». Falta cuando el docente ya no estaba activo al congelar, o cuando el contenido se guardó antes de esta historia.
- **Rationale:**
  - **Principio VII:** el CSV sale del contenido congelado, como los reportes, y no de los datos de hoy.
  - **Sin migración:** el JSON admite la clave nueva, y el hash de la versión no depende de él (`ContenidoDelParcial.hashDeLaVersion`).
  - **El caso sin dato** ya existe con la dedicación (Edge Cases: «Un dato opcional vacío sale NA»).
  - **La demostración** recrea el distributivo, así que congela con el tipo.
- **Alternatives considered:**
  - **Deducirlo del número:** un pasaporte de 10 dígitos puede pasar el módulo 10 de la cédula. Sería una regla inventada.
  - **Que Reporting le pregunte a Docentes al generar:** una dependencia nueva de `salida` hacia `datos`, y un dato de hoy en lugar del congelado.

## §3. La comprobación de los totales (FR-005)

- **Decision:** el caso de uso lee la publicación vigente **una sola vez**. Con su versión pide a Generación:
  - el listado de docentes de la versión, del que arma el `ReporteGeneral` con `ReporteGeneral.de` (D-117);
  - el contenido completo (§1), del que arma las filas.

  Antes de entregar, `FilasParaSiga` compara la suma de sus filas, por docente y por componente, con cada `FilaDelDocente` del reporte general, y la suma de todas las filas con los totales institucionales. Si algo no cuadra o falta un docente, lanza `ContenidoInconsistente`, que ya existe como `REPORTING_CONTENIDO_INCONSISTENTE` (500). En el catálogo solo se amplían la causa y la descripción técnica.
- **Rationale:**
  - **Dos lecturas distintas:** el reporte general lee las horas guardadas de cada docente, y el CSV las suma de sus asignaturas y actividades. Comparar las dos tiene sentido. `CargaCongelada` ya exige esa igualdad al congelar, así que no debería fallar nunca (Edge Cases).
  - **Una sola publicación:** si se publicara una reforma entre dos lecturas, el CSV no puede mezclar dos versiones.
  - **El mismo error:** es la misma situación que un hash distinto (el contenido no es el publicado), con la misma acción para el usuario.
- **Alternatives considered:**
  - **Llamar a `ConsultarReportes.reporteGeneral`:** relee la publicación vigente, y podría tomar otra versión.
  - **Un código nuevo `REPORTING_TOTALES_NO_CUADRAN`:** sería otro código para la misma causa y la misma acción.

## §4. El formato del archivo (FR-009 a FR-012)

- **Decision:** un formato fijo, que el layout no cambia:

  | Regla | Valor |
  |---|---|
  | Codificación | UTF-8, **sin** BOM |
  | Separador | `,` |
  | Fin de línea | CRLF, también después de la última fila (RFC 4180) |
  | Comillas | Solo si el valor tiene `,`, `"`, CR o LF; las internas se duplican |
  | Encabezado | Primera fila, con los nombres del layout |
  | Texto | En mayúsculas con `Locale.ROOT`: conserva tildes y eñes («PEÑA ÁLVAREZ JOSÉ»). Vale también para los códigos («140-b» sale «140-B») y las traducciones |
  | No aplica, o dato vacío | `NA` |
  | Horas | `BigDecimal` con escala 2 y `toPlainString()`: punto decimal, sin miles («8.00», «2.50») |
  | Fecha de publicación | `aaaa-mm-dd`, en `America/Guayaquil` |
  | Fórmulas | Una celda que empieza con `=`, `+`, `-`, `@`, tabulación o CR lleva `'` al inicio, como en los reportes (RNF-SEG-04) |

- **Rationale:**
  - **RNF-COM-02 y las convenciones de SENESCYT (ADR-002):** a diferencia del CSV de los reportes (`;`, BOM y coma decimal, D-115), este archivo es para una carga masiva, no para abrirse en una hoja de cálculo.
  - **Sin BOM:** la RFC 4180 no lo prevé, y un importador puede leerlo como parte del primer encabezado.
  - **Comillas mínimas:** lo pide el escenario 5 de la US2.
  - **Sin momento de generación:** dos CSV de la misma versión y el mismo layout son idénticos byte a byte (FR-007).
- **Dónde vive el formato (principio I):** la tabla de puertos de la constitución pone la exportación a SIGA detrás de un puerto, con «CSV con mapeo configurable» como adaptador del prototipo y la API de SIGA como adaptador futuro. Por eso el dominio solo arma y comprueba las filas (`FilasParaSiga`) y da el valor tipado de cada columna (`ColumnaDelLayout.valor`). El puerto de salida `ExportadorSiga` (`exportar(filas, layout)`) escribe el archivo, y su adaptador `ExportadorSigaCsv` aplica todas las reglas de esta tabla. Una API de SIGA sería otro adaptador del mismo puerto, sin tocar el dominio ni el caso de uso.
- **La neutralización** se escribe una sola vez, en el dominio de Reporting. Se extrae de `EscritorCsv` a `CeldaCsv.neutralizada(valor)` y la usan `EscritorCsv` y `ExportadorSigaCsv`. Los nombres de las columnas nunca la necesitan: su patrón empieza con una letra (§6).
- **Alternatives considered:** reutilizar el formato del CSV de los reportes. Su `;`, su BOM y su coma decimal no cumplen la RFC 4180 de RNF-COM-02.

## §5. Las filas y su orden

- **Decision:**

  | Fila | Componente | Actividad | Carrera | Asignatura | Paralelo | Proyecto |
  |---|---|---|---|---|---|---|
  | Una por asignatura y paralelo | `CLASE` | `NA` | El código de la carrera de la asignatura | Su código | Su paralelo | `NA` |
  | Una por actividad | El de la actividad | El código del catálogo | El código de la carrera del parcial que la registró | `NA` | `NA` | El código del proyecto, o `NA` |

  Cada fila lleva también los datos de la versión (periodo, número y fecha de publicación) y los del docente (tipo e identificación, apellidos y nombres, y dedicación).

  **El orden:**
  1. por docente, con el comparador del reporte general (alfabético del español y luego la identificación);
  2. por componente, en el orden del enumerado (clase, otras de docencia, investigación, vinculación y gestión);
  3. por código (de la asignatura o de la actividad);
  4. por carrera, paralelo, proyecto y horas, para desempatar.

  Dos asignaciones idénticas de la misma actividad dan dos filas iguales, y su orden no cambia los bytes.
- **Rationale:** es la regla 1 de ADR-002 y el escenario 2 de la US1. Con un orden total, el archivo es determinista (FR-007).

## §6. El archivo de mapeo

- **Decision:** YAML, como la configuración del flujo (F12-H01). `layout-siga-por-defecto.yaml`, un recurso de Reporting, es a la vez la plantilla comentada y el layout por defecto. El orden de la lista es la posición de cada columna.

  ```yaml
  # Layout del CSV para SIGA (F19-H01, ADR-002). Cada elemento de «columnas» es una columna del
  # CSV, en este orden. «nombre» es su encabezado; «campo», de dónde sale (ver la lista);
  # «traduccion», opcional, cambia los códigos de un catálogo por los de SIGA.
  columnas:
    - nombre: PERIODO
      campo: periodo
    - nombre: TIPO_IDENTIFICACION
      campo: tipo_identificacion
    - nombre: NUMERO_IDENTIFICACION
      campo: identificacion
    # ...
    - nombre: COMPONENTE
      campo: componente
      # traduccion:
      #   CLASE: "01"
      #   OTRAS_DOCENCIA: "02"
  ```

- **Los campos disponibles** (lista cerrada, `CampoDelLayout`):

  | Campo | Valor | ¿Traducible? |
  |---|---|---|
  | `periodo` | El código del periodo | No |
  | `version` | El número de la versión publicada | No |
  | `fecha_publicacion` | `aaaa-mm-dd` | No |
  | `hash_version` | El hash de la versión publicada | No |
  | `tipo_identificacion` | `CEDULA` o `PASAPORTE` (§2) | Sí: `CEDULA`, `PASAPORTE` |
  | `identificacion` | **Obligatorio** | No |
  | `docente` | Apellidos y nombres | No |
  | `dedicacion` | `TC`, `MT` o `TP` | Sí: `TC`, `MT`, `TP` |
  | `componente` | **Obligatorio** | Sí: los cinco componentes |
  | `actividad` | El código del catálogo | No |
  | `carrera` | El código de la carrera | No |
  | `asignatura` | El código de la asignatura | No |
  | `paralelo` | El paralelo | No |
  | `proyecto` | El código del proyecto | No |
  | `horas` | **Obligatorio** | No |

  El layout por defecto tiene las columnas de FR-004, en ese orden: el periodo, el tipo y el número de identificación, los apellidos y nombres, la dedicación, el componente, la actividad, la carrera, la asignatura, el paralelo, las horas, la versión y la fecha de publicación.

- **La traducción, solo en los catálogos cerrados.** Solo `tipo_identificacion`, `dedicacion` y `componente` admiten traducción. Sus códigos los fija el sistema, así que «cubre todos los códigos» se comprueba al cargar, sin preguntar a otro servicio (FR-015). `actividad` y `carrera` salen con el código interno (Assumptions: «sin traducción, cada catálogo sale con el código interno»). Traducirlos exigiría leer el catálogo de Normativa y el de Académico al cargar el mapeo: dos dependencias nuevas, para un formato de SIGA que nadie conoce todavía (ADR-002). Queda como ampliación del mismo archivo.

- **Las reglas al cargar** (FR-015). Se reportan **todos** los problemas, no solo el primero, como en F12-H01 §3, hasta 50 y «Hay N problemas más.». Cada detalle dice la columna (`columnas[3]`, con su nombre si lo tiene) y la regla:

  | Regla | Problema |
  |---|---|
  | Se puede leer | YAML válido, UTF-8 estricto (se quita el BOM), hasta 256 KB y 4 niveles. Sin anclas, alias ni etiquetas. Si no, `REPORTING_ARCHIVO_ILEGIBLE` |
  | Las claves | En la raíz, solo `columnas`. En cada columna, solo `nombre`, `campo` y `traduccion` |
  | Cuántas | De 1 a 30 columnas |
  | El nombre | Obligatorio, `^[A-Za-z][A-Za-z0-9_]{0,63}$` (de 1 a 64, letra al inicio, letras ASCII, dígitos o `_`) y sin repetirse, sin distinguir mayúsculas |
  | El campo | Obligatorio y de la lista cerrada |
  | Las obligatorias | Al menos una columna con `identificacion`, una con `componente` y una con `horas` |
  | La traducción | Solo en un campo traducible. Cubre exactamente sus códigos, ni más ni menos. Cada valor cumple `^[A-Za-z0-9][A-Za-z0-9 ._-]{0,29}$` (de 1 a 30), no es `NA` y no se repite en la misma traducción |

  El valor no se repite para que la suma por componente siga siendo posible en el archivo traducido (SC-002).

- **Rationale:**
  - **Un solo archivo:** la plantilla y el layout por defecto no se pueden desincronizar, y una prueba carga la plantilla por el mismo camino que un archivo subido.
  - **El orden de la lista como posición:** no hay dos columnas en la misma posición ni huecos.
  - **Nombres ASCII:** son los encabezados de una carga masiva; ADR-002 propone mayúsculas y `_`.
- **Alternatives considered:**
  - **JSON:** sin comentarios, y la plantilla «comentada en español» de FR-013 no sería posible.
  - **Un campo `posicion` explícito:** permite huecos y repetidos que habría que validar, sin ninguna ventaja.

## §7. Leer el YAML sin riesgo

- **Decision:** `LectorDeLayoutYaml`, un adaptador de salida de Reporting, con las mismas reglas que `LectorYaml` de Validación OCS (F12-H01 §2):
  - SnakeYAML con `LoaderOptions` sin alias, sin claves repetidas, con un límite de profundidad y de tamaño;
  - primero recorre los eventos: un ancla, un alias o una etiqueta explícita rechazan el archivo;
  - después arma solo el árbol de nodos, sin construir objetos, y cada valor es el texto escrito.

  Devuelve un `BorradorDeLayout` tolerante; el dominio lo valida (`LayoutExportacion.desde(borrador)`).
- **Rationale:** el principio V impide usar la clase de otro servicio. Es la forma probada en el proyecto. El lector es pequeño, porque la estructura es una sola lista.
- **Alternatives considered:** mover `LectorYaml` a `plataforma`. Sería una refactorización de Validación OCS ajena a la historia (principio XI). Además, las claves y los mensajes de cada lector son de su dominio.

## §8. Qué se guarda y cuál rige

- **Decision:** la tabla `reporting.layout_exportacion` (migración `V2`). Una fila por carga válida, que es una versión del layout de exportación (término nuevo del glosario, D-119), con:
  - el texto tal cual, para conservar los comentarios y poder mostrarlo;
  - su SHA-256;
  - quién lo cargó y cuándo.

  Su `numero` es correlativo desde 1, y rige la última. El layout por defecto es el **número 0**: no se guarda, sale del recurso. La tabla no admite `update` ni `delete`, con el mismo disparador de `carga_congelada` (principio VII).
- **Al generar** se lee el layout vigente **una vez**, al empezar, y se usa ese objeto hasta el final. Un layout que se carga mientras tanto rige para el siguiente CSV (Edge Cases).
- **Dos cargas a la vez:** `numero` es `unique` y se calcula como `coalesce(max(numero), 0) + 1` dentro de la transacción. Si dos chocan, la segunda reintenta una vez y, si vuelve a chocar, responde el 500 común. Con una sola Secretaría, no debería pasar.
- **Rationale:** es el registro de F12-H01 §4. La tabla es el historial que pide FR-016, y el archivo es la fuente.
- **Alternatives considered:**
  - **Tablas de columnas y traducciones:** repetirían lo que dice el archivo.
  - **Un layout por periodo:** la spec lo descarta; el layout es de la institución.

## §9. Permisos

- **Decision:** las cuatro operaciones nuevas exigen `PUBLICACION:APROBACION` (aclaración del 2026-11-22): el CSV, el layout vigente con las versiones anteriores, la plantilla y la carga. Hoy solo lo tiene la Secretaría general académica (D-16, `V2__matriz_d16.sql`). Para elegir el periodo, la pantalla usa `listarPublicaciones` (`REPORTES:LECTURA`), que la Secretaría también tiene.
- **Rationale:** el mismo actor carga el mapeo y genera el CSV. La matriz no cambia, y el Administrador recibe 403 (FR-020).
- **Alternatives considered:** una ruta de publicaciones con `PUBLICACION:APROBACION`. Duplicaría `listarPublicaciones` para un rol que ya puede usarla.

## §10. La bitácora (FR-017, FR-021)

- **Decision:** dos acciones nuevas, por el mismo registro persistente de Reporting (ADR-018), y Usuarios no cambia:

  | Acción | Cuándo | Objeto | `nuevo` |
  |---|---|---|---|
  | `CSV_SIGA_GENERADO` | Con el archivo completo y comprobado, antes de entregarlo | `{periodo, version}` | `{layout}`: la versión del layout de exportación, por su número (0 si es el por defecto) |
  | `LAYOUT_SIGA_CARGADO` | En la misma transacción que la fila de `layout_exportacion` | `{layout}` | `{columnas}`: cuántas tiene, y `sha256` |

  Si el registro falla, no se entrega el CSV ni queda el layout. La entrada no lleva nombres, identificaciones ni horas (principio IX).
- **El puerto:** `RegistroDeDescargas` pasa a llamarse `RegistroEnBitacora`, con un método por cada acción: `registrar(DescargaDeReporte)`, `registrar(CsvGenerado)` y `registrar(LayoutCargado)`. El adaptador `RegistroDeDescargasEventos` pasa a ser `RegistroEnBitacoraEventos`.
- **Rationale:** es el mismo camino de D-115. Renombrar el puerto evita que una carga de layout pase por un registro «de descargas». El cambio es mecánico y queda en Reporting.
- **Alternatives considered:**
  - **Agregar un `TipoDeReporte.SIGA` a `DescargaDeReporte`:** el CSV de SIGA no es un reporte ni tiene formato elegible, y la carga del layout no es una descarga.
  - **Un puerto nuevo solo para el layout:** dos puertos con el mismo adaptador y el mismo destino.

## §11. La API

| Operación | Método y ruta | Respuesta |
|---|---|---|
| `generarCsvSiga` | `GET /api/reporting/periodos/{periodo}/csv-siga` | `text/csv; charset=UTF-8`, `siga-{periodo}-v{version}.csv`, `Cache-Control: no-store` |
| `consultarLayoutsSiga` | `GET /api/reporting/layout-siga` | `LayoutsSiga`: el vigente y los anteriores, del más reciente al más antiguo |
| `descargarPlantillaLayoutSiga` | `GET /api/reporting/layout-siga/plantilla.yaml` | `text/yaml`, el recurso por defecto |
| `cargarLayoutSiga` | `PUT /api/reporting/layout-siga` (`multipart/form-data`, `archivo`) | 200 con el `LayoutSiga` nuevo; 400 con los problemas |

Es un `GET`, como las descargas de los reportes. El registro en la bitácora es un efecto de auditoría, no un cambio del recurso. Los detalles están en [contracts/reporting.md](contracts/reporting.md).

## §12. La pantalla

- **Decision:** una pantalla nueva, «Exportación a SIGA» (`/exportacion-siga`), con su enlace en el menú para `PUBLICACION:APROBACION`. Tiene dos secciones:
  1. **«CSV para SIGA»:** el periodo (de `listarPublicaciones`), la versión publicada, con qué layout se generará y el botón «Generar CSV para SIGA».
  2. **«Layout de exportación»:**
     - el vigente con sus columnas;
     - «Descargar plantilla» y «Cargar archivo de mapeo», con los problemas si se rechaza;
     - las versiones anteriores.
- **Rationale:** «Reportes» es de `REPORTES:LECTURA`, que tienen cinco roles. Poner allí una acción solo de la Secretaría mezclaría dos permisos en una pantalla. Separarla deja la de reportes sin cambios.
- **Alternatives considered:** un botón en «Reportes», visible solo con `PUBLICACION:APROBACION`. La carga del layout no tiene cabida allí, y las pruebas de los otros cuatro roles cambiarían.

## §13. La demostración

Dos pasos nuevos en `deploy/probar_documento_firmable.sh`, después de R8, con la versión 1 publicada:
- **R9:** el CSV por defecto, comparado con el reporte general;
- **R10:** un layout nuevo, uno inválido y el CSV con el nuevo.

Además se amplían R1 (409), R4 (403) y R5 (idéntico durante la reforma). Detalle en [contracts/demostracion.md](contracts/demostracion.md).

## §14. El tamaño

300 docentes con unas 8 filas cada uno son unas 2 400 filas y menos de 400 KB. Todo se arma en memoria, como los reportes.

SC-008 se mide en `GenerarCsvSigaServicioTest`, con 300 docentes de prueba, los puertos de lectura simulados y `ExportadorSigaCsv` real, porque escribir el archivo es parte de lo que se mide. La evidencia anota el tiempo de punta a punta en el perfil consolidado.
