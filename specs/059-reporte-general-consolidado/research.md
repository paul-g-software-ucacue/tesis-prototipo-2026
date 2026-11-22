# Research: Reporte general consolidado en PDF y CSV

F20-H02 (#269). Cada sección cierra una decisión del plan. Todas van a la decisión **D-117**, que se registra en `docs/decisiones.md` antes de implementar. Lo que ya decidió D-115 (F20-H01, spec 058) no se repite. Esta historia lo reutiliza:
- la publicación vigente que da Aprobación;
- el contenido congelado por docente, guardado al congelar;
- la comprobación del hash;
- el camino del PDF/A-2b;
- el formato del CSV;
- el registro de cada descarga antes de responder.

## §1. De dónde salen las horas de cada docente

**El problema.** La ruta interna de Generación que lista los docentes de una versión (`consultarCargasCongeladas`, `GET /interno/generacion/periodos/{periodo}/versiones/{numero}/cargas`) devuelve solo la identificación y el nombre. Para el reporte general hacen falta, por docente, su dedicación y sus horas por componente.

- **Decision:** ampliar esa misma ruta. Cada `DocenteCongelado` suma:
  - `dedicacion`, opcional como en `DatosDelDocenteCongelado`: falta si el docente no estaba activo al congelar;
  - `horas`, obligatoria, con el esquema `HorasPorComponente` que ya existe.

  Generación las lee de la columna `contenido` de `carga_congelada`, que ya las guarda (`contenido->'horas'` y `contenido->>'dedicacion'`). No cambia la tabla, no hay migración y no hay cálculo nuevo.
- **Rationale:**
  - **Una sola fuente.** El reporte general y el individual leen las mismas horas guardadas de cada docente. Por eso la fila de un docente en el general es igual a su reporte individual (SC-002), sin una regla que lo fuerce.
  - **Una sola llamada.** Con 300 docentes, el reporte general no hace 300 llamadas.
  - **Hoy solo la usa Reporting.** Reporting lee la ruta con su esquema (principio XIV) y se actualiza en el mismo cambio. El listado público de docentes (`listarDocentesDelPeriodo`) sigue dando solo la identificación y el nombre.
- **Alternatives considered:**
  - **Una ruta nueva (`…/cargas/resumen`):** duplica la consulta y la comprobación de la versión congelada para dar los mismos docentes con dos campos más.
  - **Una llamada por docente a la ruta del contenido individual:** son N llamadas entre procesos por reporte, contra SC-007.
  - **Devolver el contenido completo de cada docente:** con sus asignaturas y actividades, el reporte general las descartaría todas. La respuesta sería unas diez veces más grande, sin ningún uso.
  - **Que Generación calcule los totales institucionales:** sería una regla de presentación en el dominio central. Además, Reporting tendría que confiar en una suma que no puede comprobar con las filas que recibe.

## §2. El agregado `ReporteGeneral`

- **Decision:** un agregado de lectura nuevo en el dominio de Reporting, hermano de `ReporteIndividual`.
  - **Qué tiene:** la publicación, las filas por docente (`FilaDelDocente`: identificación, nombre, dedicación opcional y horas por componente), los totales institucionales, y quién lo generó y cuándo.
  - **`ReporteGeneral.de(publicacion, docentes, quien, momento)`:**
    - comprueba que el hash del contenido sea el de la publicación;
    - rechaza un docente repetido;
    - ordena las filas por nombre y luego por identificación;
    - calcula los totales como la suma exacta de las filas, componente por componente, sin redondear.

    El hash distinto y el docente repetido lanzan `ContenidoInconsistente`.
  - **`HorasPorComponente.mas(otras)`:** suma componente por componente. Es el único cambio al objeto de valor que ya existe, y sirve para acumular los totales.
  - **Una versión sin docentes:** es válida, con los totales en 0. No se espera en la práctica, pero el agregado no lo prohíbe: el reporte diría lo que tiene la versión publicada.
- **Rationale:**
  - FR-003 a FR-005 y SC-003 son reglas del reporte, y se prueban en el dominio sin dobles (principio VIII).
  - Un docente repetido en el contenido es un error de datos. Si el reporte lo imprimiera, sumaría sus horas dos veces en un documento oficial.
  - El total de cada fila (`HorasPorComponente.total()`) ya existe. El total general es la suma de los cinco totales institucionales, y la prueba comprueba que también es igual a la suma de los totales de las filas.
- **Alternatives considered:**
  - **Generalizar `ReporteIndividual`:** mezcla dos documentos con filas distintas.
  - **Calcular los totales en el escritor de CSV y en la plantilla del PDF por separado:** se pueden desalinear, contra FR-007.

## §3. El PDF/A

- **Decision:** el mismo adaptador, `GeneradorDeReportePdfa`, con un segundo método en el puerto: `GeneradorDePdf.generar(ReporteGeneral)`.
  - **El renderizado común.** Del método actual se extrae lo que es igual: el renderizado con PDF/A-2b, la fuente incrustada, el perfil sRGB y el pie de página. Cada reporte arma su propio HTML.
  - **La página:** A4 horizontal (`@page { size: A4 landscape }`), porque la tabla tiene nueve columnas: identificación, docente, dedicación, los cinco componentes y el total. En vertical, los nombres se partirían en tres o cuatro líneas.
  - **El encabezado del documento:** «Reporte general del distributivo», con el periodo, la versión publicada, su hash, la fecha de publicación, el número de docentes, y quién lo generó y cuándo.
  - **La tabla:**
    - el encabezado de las columnas va en `thead`, que se repite en cada página;
    - la fila de los totales institucionales es la **última fila del cuerpo**, con su clase y el fondo gris del total del individual (ajuste al implementar: no en negrita, porque solo la fuente regular está incrustada y una negrita sin su fuente no sería PDF/A). No va en `tfoot`, porque openhtmltopdf repetiría un `tfoot` en cada página y los totales deben aparecer una sola vez.
    - Ninguna fila se parte entre dos páginas (`page-break-inside: avoid`).
  - **El pie de cada página:** el periodo, la versión, el hash y «página X de Y», como el individual.
  - **Las pruebas.** `GeneradorDeReportePdfaTest` suma un caso de 300 docentes, que verifica:
    - la conformidad con veraPDF 2B, sin `AcroForm` ni anotaciones;
    - el encabezado de la tabla repetido en cada página;
    - el pie en cada página;
    - «Total institucional» una sola vez, en la última página.
- **Rationale:**
  - El mismo camino de D-101 y D-115, ya verificado en el repositorio.
  - El documento del reporte general es otro, pero el formato y la verificación son los mismos.
- **Alternatives considered:**
  - **A4 vertical con letra más chica:** con nueve columnas, los nombres largos ocupan varias líneas y el documento casi duplica sus páginas.
  - **Un adaptador nuevo:** repetiría la configuración de PDF/A que hoy está en un solo lugar.

## §4. El CSV

- **Decision:** el mismo formato de archivo que el individual (D-115 §5): UTF-8 con BOM, `;`, CRLF, comillas dobles en las celdas de texto, horas con dos decimales y coma decimal, fórmulas neutralizadas con un apóstrofo.
  - **Una fila por docente, con una columna por componente:** a diferencia del individual, que tiene una fila por asignación.
  - **Las columnas:** `Periodo`, `Versión`, `Hash de contenido`, `Publicada el`, `Tipo`, `Identificación`, `Docente`, `Dedicación`, `Clase`, `Otras actividades de docencia`, `Investigación`, `Vinculación`, `Gestión` y `Total`. Los datos de la versión se repiten en cada fila, como en el individual.
  - **Las filas:**
    - una por docente (`Tipo` = `DOCENTE`), en el orden del PDF;
    - al final, una de totales (`Tipo` = `TOTAL`), con `Docente` = `Total institucional` y la identificación y la dedicación vacías.
  - **La dedicación:** su nombre («Tiempo completo», «Medio tiempo», «Tiempo parcial»), o «sin dato» si falta, como el individual.
  - **Qué no lleva:** quién lo generó ni cuándo. Así dos CSV de la misma versión son idénticos byte a byte (FR-011).
  - **El código.** `EscritorCsv` suma `escribir(ReporteGeneral)`. Las piezas comunes (la celda de texto neutralizada, las horas y la línea) ya son métodos del mismo escritor y se reutilizan.
- **Rationale:**
  - **La unidad es el docente.** La unidad del reporte general es el docente, y el PDF muestra una fila por docente con sus cinco componentes. Con la misma forma, comparar el CSV con el PDF es directo (SC-004).
  - **Comparar con SIGA.** Una fila por docente también facilita comparar los totales con el CSV para SIGA de F19 (RF-52).
  - **El mismo formato** hace que los dos reportes se abran igual en una hoja de cálculo configurada en es-EC.
- **Alternatives considered:**
  - **El formato largo del individual (una fila por docente y componente):** cinco veces más filas, y para ver la carga de un docente hay que armar una tabla dinámica.
  - **Los totales en una cabecera aparte:** el archivo deja de ser una tabla regular.
  - **Sin fila de totales:** el escenario 2 del issue pide los mismos totales en los dos formatos.

## §5. Las rutas públicas

- **Decision:** tres operaciones nuevas en `contratos/reporting/openapi.yaml`, todas con `REPORTES:LECTURA` (`x-mdd-permiso`):
  - `GET /api/reporting/periodos/{periodo}/reporte-general` (`consultarReporteGeneral`): el reporte en JSON, para la pantalla. No deja entrada en la bitácora.
  - `GET …/reporte-general.pdf` (`descargarReporteGeneralPdf`) y `GET …/reporte-general.csv` (`descargarReporteGeneralCsv`): las descargas, con `Content-Disposition: attachment; filename="reporte-general-<periodo>.pdf"` (o `.csv`) y `Cache-Control: no-store`.

  **Las respuestas de error:** 400 si el periodo no cumple su patrón, 401, 403, 409 `REPORTING_SIN_PUBLICACION`, 500 `REPORTING_CONTENIDO_INCONSISTENTE` o el interno, y 502 o 504 si `distributivo` falla o no responde. No hay 404: el reporte general no pide un docente.
- **Rationale:**
  - El mismo molde de las rutas del individual: un recurso por formato, para que el navegador descargue con el nombre del archivo.
  - El reporte cuelga del periodo, porque es de todo el periodo.
- **Alternatives considered:** un parámetro `tipo=general` en las rutas del individual, que mezcla un recurso que pide un docente con otro que no.

## §6. La bitácora

- **Decision:** cada descarga publica `reporting.EntradaBitacora` con la acción `REPORTE_GENERAL_DESCARGADO`, por el mismo registro persistente de publicaciones.
  - **Objeto:** `{periodo, version}`.
  - **Valor nuevo:** `{formato}`.
  - **Quién y cuándo:** del token y del reloj.

  El mismo orden que el individual: primero se genera el archivo completo, después se registra en una transacción, y recién entonces se responde.
  - **`DescargaDeReporte`** suma el tipo de reporte (`TipoDeReporte`: `INDIVIDUAL` o `GENERAL`). La identificación se exige solo en el individual, y en el general debe faltar.
  - **`RegistroDeDescargasEventos`** elige la acción por el tipo, y no pone la identificación en el objeto del general.
  - **Usuarios no cambia:** el origen `reporting` ya está admitido y la acción cumple el patrón `^[A-Z][A-Z_]{2,39}$` del contrato.
- **Rationale:**
  - FR-013 y el principio VII.
  - Una acción propia permite distinguir en la bitácora quién bajó el distributivo completo, sin mirar el objeto.
- **Alternatives considered:** la misma acción del individual con la identificación vacía, que haría ambigua la consulta de la bitácora.

## §7. Los errores

- **Decision:** ningún código nuevo. El reporte general usa:
  - `REPORTING_SIN_PUBLICACION` (409), si el periodo no tiene versión publicada (FR-006);
  - `REPORTING_CONTENIDO_INCONSISTENTE` (500), si el hash no coincide, si Generación no tiene la versión, o si un docente viene repetido (§2);
  - `COMUN_PROCESO_CON_ERROR` (502) y `COMUN_PROCESO_SIN_RESPUESTA` (504);
  - `COMUN_SOLICITUD_INVALIDA` (400).

  En `contratos/comun/errores.md` se amplían la causa de `REPORTING_CONTENIDO_INCONSISTENTE` (el docente repetido) y las operaciones que usan los dos códigos de Reporting.
- **Rationale:** principio XIII y principio X. Los casos son los mismos del individual, y el usuario hace lo mismo en cada uno.

## §8. La pantalla «Reportes»

- **Decision:** en `frontend/src/pantallas/reportes/`.
  - **Arriba, el periodo,** como hoy.
  - **El tipo de reporte:** debajo del periodo, un grupo de opciones «Tipo de reporte» (`fieldset` con `legend`, dos `radio`): «General» y «Por docente». La opción propuesta es «General», porque no exige buscar a nadie y es el reporte que se presenta al OCS.
  - **«General»:**
    - los datos de la publicación, con el aviso de la reforma si la hay;
    - el número de docentes;
    - la tabla de los docentes con sus cinco componentes y su total, con su `caption` y `th scope`;
    - la fila de los totales institucionales al final, en `tfoot`, que en la pantalla no se repite;
    - «Descargar PDF» y «Descargar CSV», con el mismo aviso de la columna «Identificación» al bajar el CSV.

    El JSON del reporte general se pide solo al elegir «General».
  - **«Por docente»:** el buscador y el reporte individual de hoy, sin cambios.
  - **Las pruebas:**
    - Vitest (`Reportes.test.tsx`): la opción propuesta, la tabla y los totales, las dos descargas con su nombre, el aviso del CSV y el cambio a «Por docente»;
    - Playwright (`reportes.spec.ts`): el Rector baja los dos archivos del general, y axe-core con «General» y con «Por docente». Los casos del individual suman el clic en «Por docente».
- **Rationale:**
  - FR-015 a FR-017. Elegir entre dos reportes del mismo periodo evita una pantalla con dos tablas largas, una debajo de la otra.
  - Un grupo de opciones nativo es accesible con el teclado sin código propio.
- **Alternatives considered:**
  - **Las dos secciones, una debajo de la otra:** con 50 a 300 docentes, el buscador del individual queda muy abajo.
  - **Pestañas ARIA:** necesitan el manejo de las flechas y del foco; el grupo de opciones da lo mismo con HTML nativo.
  - **Una pantalla aparte en el menú:** duplica el selector de periodo y suma un enlace por rol.

## §9. El rendimiento

- **Decision:** sin caché.
  - **La pantalla:** el reporte se arma en cada pedido con dos llamadas internas, las publicaciones y los docentes de la versión.
  - **El PDF:** se genera en memoria, como el individual.
  - **La medición:** `GeneradorDeReportePdfaTest` genera 300 docentes, y la evidencia anota cuánto tarda. SC-007 pide menos de 10 s por descarga con 300 docentes. Como referencia, el PDF/A del consolidado de F13-H02, que es más largo (resumen por carrera más el detalle por docente), se genera en ese orden con los 47 docentes de la demo.
- **Rationale:** principio X. El reporte de una versión publicada no cambia, pero se pide pocas veces, y una caché sería estado nuevo en un servicio sin tablas.
- **Alternatives considered:** guardar el PDF generado por versión, que necesita almacenamiento de archivos en `salida`.

## §10. La demostración

- **Decision:** `deploy/probar_documento_firmable.sh` suma dos pasos, R7 y R8, después de R3, y amplía R1, R4, R5 y R6 con el reporte general ([contracts/demostracion.md](contracts/demostracion.md)). Las funciones `bajar`, `pdf_sin_campos` y `csv_igual_al_json` se generalizan para recibir la ruta del reporte.
- **Rationale:**
  - **Las dos comparaciones clave:** que la fila de cada docente sea igual a su reporte individual (SC-002) y que los totales sean la suma de las filas (SC-003). Se hacen de punta a punta, con la versión publicada de verdad.
  - **La versión no cambia en la reforma:** R5 comprueba que el reporte general sigue igual mientras se corrige la oferta.
