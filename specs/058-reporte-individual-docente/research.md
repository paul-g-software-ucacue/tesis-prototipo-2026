# Research: Reporte individual del docente en PDF y CSV

F20-H01 (#268). Cada sección cierra una decisión del plan. Todas van a la decisión **D-115**, que se registra en `docs/decisiones.md` antes de implementar.

## §1. Qué lee Reporting y de quién

- **Decision:** Reporting (proceso `salida`) lee por dos rutas internas nuevas del proceso `distributivo`, con el token de proceso (ADR-023):
  - **Aprobación** (`GET /interno/validacionocs/publicaciones`): la publicación vigente de cada periodo, con la versión, su hash, quién publicó y cuándo. Es la última transición a PUBLICADO (`ProcesoAprobacion.publicacion()`, D-111), la misma regla de F18-H02 (spec 056, research §3): durante una reforma sigue siendo la anterior.
  - **Generación** (`GET /interno/generacion/periodos/{periodo}/versiones/{numero}/cargas` y `…/cargas/{identificacion}`): el contenido congelado de **esa** versión, por docente (§2).
- Reporting no guarda nada del distributivo. Cada reporte se arma al pedirlo, con las dos lecturas.
- **Rationale:**
  - Aprobación decide cuál versión es la publicada (MD-11), y Generación es dueña del contenido.
  - Publicación y consulta es conformista con los dos (mapa de contextos) y solo lee por llamada.
  - Es el mismo molde de `DatosDesdeProcesos` en Validación OCS y de `FirmantesDesdeProcesos` en Firma (`ConsultasEntreProcesos`).
- **Alternatives considered:**
  - **Reporting lee los esquemas `generacion` y `validacionocs`:** viola el principio V.
  - **Aprobación arma el reporte y lo expone:** mezcla la salida con el flujo y pone el PDF en el proceso `distributivo`, contra ADR-006.
  - **Una sola ruta en Generación que también diga cuál está publicada:** Generación no sabe qué versión se publicó; lo decide Aprobación.

## §2. El contenido congelado, guardado al congelar

**El problema.** Hoy el contenido de una versión se recalcula al pedirlo (`ContenidoCongeladoServicio`), y dos de sus datos no están en la versión:
- **Las horas de clase** salen de la oferta **actual** de Académico (`HorasDelPeriodo.horasDe`). Académico permite corregir las horas de un paralelo sin mirar la etapa del distributivo (`MantenerOfertaServicio.corregir`), y retirarlo mientras el periodo está en Planificación, que publicar no cambia (spec 055).
- **La dedicación** está solo en Docentes, y puede cambiar.

Así, el reporte de la versión publicada podría cambiar después de publicar, sin que cambie su hash: el hash solo cubre las propuestas y las actividades (`ContenidoDelParcial`). Eso rompe FR-010 y SC-001, y deja el reporte distinto del PDF/A firmado.

- **Decision:** Generación guarda el **contenido congelado por docente** de cada versión, una sola vez, cuando Aprobación la congela.
  - **Cuándo:** con el mismo aviso que prepara el documento firmable (`AvisoDeFirma.DistributivoEnFirma`, después de confirmar la congelación). Un oyente nuevo de Aprobación, `ContenidoDeLaFirma`, le pide a Generación `guardarContenidoCongelado(periodo, version)`, por orden de Aprobación (MD-04), con `@ApplicationModuleListener`. Si falla, el reenvío de publicaciones lo repite (ADR-018).
  - **Qué guarda:** para cada docente de la versión, su nombre, su dedicación y sus horas contratadas (de Docentes, en ese momento), sus asignaturas-paralelo con la carrera y las horas de clase, sus actividades con su componente, su unidad o proyecto y sus horas, y su carga horaria por componente. Lo calcula el código que ya existe (`ContenidoCongeladoServicio.docentes`, `CargaHoraria.calcular`), sin reglas nuevas.
  - **Junto al hash:** la cabecera guarda el hash de la versión (`hashDelDistributivo`) y cuándo se guardó.
  - **Idempotente:** la clave es (periodo, versión). Si ya está, no hace nada; nunca se sobrescribe.
  - **Si falta** (una versión congelada antes de esta historia, o el oyente todavía no corrió): la ruta interna lo calcula y lo guarda en ese momento, con la oferta y los docentes de ese momento, y anota un `WARN` con el periodo y la versión. En la demo, el primer reporte de 2026-I después del despliegue lo deja guardado.
- **Rationale:**
  - Principio VII: lo publicado no cambia. Lo que se firmó se calculó con la oferta del momento de congelar; el contenido guardado se calcula en el mismo momento.
  - F20-H02 (los totales institucionales) y F19 (el CSV para SIGA) necesitan lo mismo: ninguna de las dos debería recalcular con la oferta actual.
  - No cambia ninguna regla ni ningún cálculo; solo guarda el resultado.
- **Alternatives considered:**
  - **Recalcular con la oferta actual y aceptar el riesgo:** el reporte de la versión publicada podría no coincidir con el PDF firmado. Se descarta por el principio VII.
  - **Que Reporting guarde una copia al publicarse:** necesita recibir eventos en el proceso `salida` desde `distributivo`, y la copia sería de la oferta al publicar, no al congelar: podría no coincidir con lo firmado.
  - **Guardar las horas en cada propuesta al congelar:** cambia `Propuesta`, el repositorio y cada cálculo de `CargaHoraria` del dominio central. Más riesgo por la misma garantía.
  - **Leer el PDF/A firmado:** no sirve para el CSV ni para la pantalla.

## §3. La comprobación del hash

- **Decision:** Reporting compara el hash de la publicación (de Aprobación) con el del contenido congelado (de Generación). Si no coinciden, responde `REPORTING_CONTENIDO_INCONSISTENTE` (500) y no genera nada.
- **Rationale:** el pie del PDF dice «versión N, hash H». La comprobación garantiza que el contenido es el de esa versión, y no cuesta nada.
- **Alternatives considered:** confiar en Generación sin comparar; un error así quedaría impreso en un documento oficial.

## §4. El PDF/A

- **Decision:** un adaptador propio de Reporting, `GeneradorDeReportePdfa`, con el mismo camino de F13-H02 (D-101):
  - openhtmltopdf con PDFBox, `PdfAConformance.PDFA_2_B`, la fuente DejaVu Sans incrustada (una copia en `reporting/src/main/resources/fuentes`, con su licencia) y el perfil sRGB;
  - sin `AcroForm`, sin anotaciones ni JavaScript;
  - el pie de cada página con el periodo, la versión, su hash de contenido y «página X de Y», con las cajas de margen de `@page`;
  - los encabezados de las tablas se repiten en cada página (`thead` con `display: table-header-group`).
  - Las pruebas lo verifican con veraPDF (perfil 2B), y comprueban con PDFBox que no tiene `AcroForm` ni anotaciones. Igual que Firma, veraPDF y JAXB 2.3 van solo en el classpath de las pruebas.
- **Rationale:**
  - La aclaración del 2026-11-21: PDF/A-2b sin campos ni contenido activo y sin cifrado.
  - Es el camino ya probado en el repositorio, con las mismas dependencias.
- **Alternatives considered:**
  - **Extraer un módulo `plataforma:pdfa` compartido con Firma:** toca Firma, que es dominio central y ya está firmado y probado. Se anota como mejora posible cuando lo necesite una tercera historia.
  - **Que Firma genere el reporte:** Firma está en el proceso `distributivo` y su documento es el firmable; el reporte es de la salida.

## §5. El CSV

- **Decision:**
  - **Formato:** UTF-8 con BOM, separador `;`, fin de línea CRLF y comillas dobles en todas las celdas de texto (RFC 4180 con otro separador). Las horas con dos decimales y coma decimal (`12,50`), como el PDF.
  - **Por qué `;` y coma decimal:** es la configuración regional de Ecuador (es-EC). Una hoja de cálculo con esa configuración lo abre en columnas, sin asistente, y lee bien las tildes por el BOM.
  - **Columnas:** `Periodo`, `Versión`, `Hash de contenido`, `Publicada el`, `Identificación`, `Docente`, `Dedicación`, `Tipo`, `Carrera o unidad`, `Código`, `Asignatura o actividad`, `Paralelo`, `Componente`, `Horas`. Los datos de la versión y del docente se repiten en cada fila, así cada fila se entiende sola.
  - **Filas:** una por asignatura-paralelo (`Tipo` = `ASIGNATURA`, componente `Clase`), una por actividad (`Tipo` = `ACTIVIDAD`), y seis de totales (`Tipo` = `TOTAL`): una por componente, también las que suman 0, y una con `Componente` = `Total`.
  - **Orden:** el mismo del PDF: asignaturas por carrera, código y paralelo; actividades por componente y nombre; totales en el orden de los componentes.
  - **Fórmulas (FR-009):** a toda celda de texto que empiece con `=`, `+`, `-`, `@`, tabulación o retorno de carro se le antepone un apóstrofo (`'`), como recomienda OWASP. Las horas son números y nunca son negativas, así que no se tocan. El PDF muestra el texto original.
  - **La cédula y el cero inicial** (decisión del autor del 2026-11-21, después del recorrido): una hoja de cálculo lee `"0109990341"` como número y muestra `109990341`, aunque vaya entre comillas. El archivo no cambia: al descargar el CSV, un modal avisa que la columna «Identificación» se importa como texto. Se descartaron escribirla como fórmula (`="0109990341"`), que contradice RNF-SEG-04 y rompe la lectura por programas, y con un apóstrofo delante, que se ve en la celda. El documento para presentar es el PDF.
  - **Qué no lleva:** quién lo generó ni cuándo, que van solo en el PDF. Así dos CSV de la misma versión son idénticos byte a byte (FR-010).
- **Rationale:**
  - «Los dos contienen los mismos datos»: el CSV tiene los mismos datos que el PDF; el PDF solo suma quién lo generó y cuándo.
  - Un solo escritor, `EscritorCsv`, en el dominio de Reporting, sin dependencias, probado sin infraestructura.
- **Alternatives considered:**
  - **Coma y punto decimal:** una hoja de cálculo en es-EC lo abriría en una sola columna.
  - **Una cabecera con los datos de la versión y después la tabla:** ya no es una tabla regular, y no se puede filtrar ni concatenar con otros reportes (F20-H02).
  - **Una biblioteca de CSV:** el formato es simple y el escritor cabe en unas decenas de líneas; una dependencia más pasa por el SCA y el SBOM.

## §6. Las rutas públicas de Reporting

- **Decision:** un contrato nuevo, `contratos/reporting/openapi.yaml`. Todas las rutas exigen `REPORTES:LECTURA` (`x-mdd-permiso`), y el API Gateway ya enruta `/api/reporting/**` a `salida`:
  - `GET /api/reporting/publicaciones`: los periodos con versión publicada vigente, de la más reciente a la más antigua. La pantalla propone la primera.
  - `GET /api/reporting/periodos/{periodo}/docentes`: la publicación vigente y los docentes de esa versión (identificación y nombre), por nombre.
  - `GET /api/reporting/periodos/{periodo}/docentes/{identificacion}/reporte`: el reporte en JSON, para la pantalla. No deja entrada en la bitácora.
  - `GET …/reporte.pdf` y `GET …/reporte.csv`: las descargas, con `Content-Disposition: attachment; filename="reporte-individual-<periodo>-<identificacion>.pdf"` (o `.csv`). Cada una deja su entrada en la bitácora (§7).
- **Rationale:**
  - El Rector no tiene permiso para leer los periodos de Académico. La lista de Reporting le da solo los periodos que tienen algo que reportar.
  - Un recurso por formato, con su propia extensión, para que el navegador descargue el archivo con su nombre.
- **Ajuste de la spec:** FR-014 decía «por defecto, el vigente». La pantalla propone el periodo de la **publicación más reciente**, que no exige leer Académico. Se ajusta FR-014 y su caso límite.
- **Alternatives considered:** negociar el formato con `Accept`, que un enlace de descarga no puede enviar.

## §7. La bitácora de cada descarga

- **Decision:**
  - Cada descarga publica `reporting.EntradaBitacora` con la acción `REPORTE_INDIVIDUAL_DESCARGADO`, por el registro persistente de publicaciones del proceso `salida` (ADR-018, esquema `eventos_salida`, que ya existe).
    - **Objeto:** `{periodo, version, identificacion}`.
    - **Valor nuevo:** `{formato}`, `PDF` o `CSV`.
    - **Quién y cuándo:** del token y del reloj.
  - **El orden:** se genera el archivo completo en memoria y se publica el evento en una transacción. Recién después de confirmarla se responde con el archivo. Si la publicación falla, la respuesta es un error y no se entrega nada.
  - **Lo que hay que habilitar:**
    - `apps/salida` suma `plataforma:eventos`;
    - Identidad, la clave del proceso `salida` (`mdd.usuarios.procesos.salida.clave`, `MDD_CLAVE_PROCESO_SALIDA` en `compose.yaml` y en los secretos del despliegue);
    - Usuarios admite el origen `reporting` en sus tres listas (`RegistrarEventoRecibidoServicio`, `EntradaBitacora` y `FiltroDeBitacora`), y la descripción de `/interno/usuarios/eventos` dice siete servicios.
  - **Sin tabla local:** a diferencia de Académico, Reporting no guarda una copia, porque su esquema no tiene otra cosa (principio X).
- **Rationale:** aclaración del 2026-11-21 y principio VII. Es el mismo camino que usan los demás servicios: la bitácora de F17-H01 y su consulta de F17-H02 lo muestran sin cambios.
- **Alternatives considered:**
  - **Registrar después de responder:** una descarga podría quedar sin registro.
  - **Un `@Async` sin registro persistente:** se perdería si cae `identidad`.

## §8. Los errores

- **Decision:** tres códigos nuevos en `contratos/comun/errores.md`:
  - `REPORTING_SIN_PUBLICACION` (409): el periodo no tiene versión publicada (FR-005).
  - `REPORTING_DOCENTE_SIN_DISTRIBUTIVO` (404): el docente no está en la versión publicada (FR-005).
  - `REPORTING_CONTENIDO_INCONSISTENTE` (500, sistema): el hash no coincide (§3).
  - Si un proceso no responde, `COMUN_PROCESO_CON_ERROR` (502) o `COMUN_PROCESO_SIN_RESPUESTA` (504), los comunes de `ConsultasEntreProcesos`.
  - Una identificación o un periodo con un formato inválido responden `COMUN_SOLICITUD_INVALIDA` (principio XIV).
- **Rationale:** principio XIII. Los 409 y 404 dicen qué pasa y qué hacer; el 500 no dice nada técnico.
- **Alternatives considered:** reutilizar el 404 común para el docente; no diría que el docente existe pero no tiene carga en esa versión.

## §9. La pantalla «Reportes»

- **Decision:**
  - **Dónde:** `frontend/src/pantallas/reportes/`, en la ruta `/reportes` y en el menú como «Reportes», con `RutaConPermiso funcionalidad="REPORTES" tipo="LECTURA"`. `enlaces.test.ts` lo comprueba por rol.
  - **El periodo:** una lista con los periodos publicados; propone el más reciente.
  - **El docente:** un buscador sobre la lista de docentes de la versión, por nombre o identificación, filtrado en el navegador porque son decenas.
  - **El reporte:** el encabezado (periodo, versión, hash, publicación y datos del docente), la tabla de asignaturas, la de actividades y la de horas por componente con el total. Cada tabla tiene su `caption`, y una sección vacía dice «No tiene asignaturas» o «No tiene actividades».
  - **Las descargas:** «Descargar PDF» y «Descargar CSV», con su error en la pantalla.
  - **Sin publicación:** si no hay ningún periodo publicado, la pantalla dice «Todavía no hay un distributivo publicado» y no ofrece nada más.
  - **Pruebas:** Vitest y Playwright con axe-core.
- **Rationale:** FR-014 a FR-016 y SC-005. Sigue los moldes de `pantallas/bitacora` y de `pantallas/comun`.

## §10. La demostración

- **Decision:** `deploy/probar_documento_firmable.sh` ya recorre la publicación de la versión 1, la reforma y la publicación de la versión 2. Se le suman pasos de reportes en tres momentos:
  1. con la versión 1 publicada;
  2. durante la reforma, después de corregir las horas de un paralelo en la oferta;
  3. con la versión 2 publicada.
- Detalle en [contracts/demostracion.md](contracts/demostracion.md).
- **Rationale:** el segundo momento prueba §2: el reporte de la versión 1 no cambia aunque cambie la oferta. Cada historia deja sus pasos en el workflow (constitución §6).
