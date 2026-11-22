---
description: "Tareas de F20-H02: reporte general consolidado en PDF y CSV"
---

# Tasks: Reporte general consolidado en PDF y CSV

**Input**: documentos de diseño en `specs/059-reporte-general-consolidado/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto.

**Rutas**:
- `REP/` es `backend/servicios/reporting/src/`;
- `GEN/` es `backend/servicios/generacion/src/`;
- `SPA/` es `frontend/src/`;
- `.../` es el paquete `ec/mdd/reporting/` o `ec/mdd/generacion/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de una tarea pendiente).
- **[Story]**: US1 (el reporte general en pantalla) o US2 (las descargas en PDF y CSV).

---

## Phase 1: Setup

- [X] T001 Escribir **D-117** en `docs/decisiones.md`, en la fila del índice y en su sección, con research §1 a §10:
  - la ruta interna `consultarCargasCongeladas` suma la dedicación y las horas de cada docente, leídas de `carga_congelada.contenido`, sin migración. Por eso el general y el individual leen las mismas horas;
  - `ReporteGeneral` en el dominio de Reporting: el hash, el docente repetido, el orden y los totales exactos;
  - el PDF/A-2b en A4 horizontal, con «Total institucional» como última fila del cuerpo y no en `tfoot`;
  - el CSV ancho (una fila por docente, una columna por componente, la fila `TOTAL`), con el formato de archivo de D-115;
  - las tres rutas públicas;
  - la acción `REPORTE_GENERAL_DESCARGADO`, con el objeto `{periodo, version}`;
  - sin códigos de error nuevos;
  - el grupo «Tipo de reporte» en la pantalla, con «General» propuesto;
  - sin caché;
  - el riesgo de que los totales difieran de los del PDF/A del consolidado;
  - los datos de las pruebas de volumen: los 300 docentes salen de los 47 de `06_docentes.csv` de los datos de prueba, repetidos con una identificación derivada en el mismo rango ficticio (`010999xxxx`) y un sufijo en el nombre. Así se cumple el principio VIII, que pide usar solo los datos de `cargas/datos_prueba/`.

  Relacionar con RF-46, RF-48, RNF-SEG-04, D-101 y D-115.
- [X] T002 Crear la rama `059-reporte-general-consolidado` desde `main` actualizado y confirmar que `.specify/feature.json` apunta a `specs/059-reporte-general-consolidado`. Confirmar también que el issue #269 tiene cargadas sus «Horas estimadas» en el proyecto: el principio X pide la decisión y su estimación, y la definición de listo del issue también la exige. Si faltan, pedírselas al autor antes de seguir.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Propósito:** los contratos, las horas de cada docente en la ruta interna de Generación y los cambios que comparten las dos historias. Ninguna historia empieza antes.

### Contratos

- [X] T003 [P] Ampliar `contratos/reporting/openapi.yaml` según `contracts/reporting.md`:
  - **Las operaciones:** `consultarReporteGeneral` (tag `Reportes`), y `descargarReporteGeneralPdf` y `descargarReporteGeneralCsv` (tag `Descargas`). Cada una con `x-mdd-permiso: [REPORTES:LECTURA]`, el parámetro `Periodo` y las respuestas 400, 401, 403, 409 `SinPublicacion`, 500 `ErrorDelReporte`, 502 y 504. Sin 404.
  - **Los esquemas nuevos:**
    - `ReporteGeneral`, con `required: [publicacion, docentes, totales, generadoPor, generadoEn]`;
    - `FilaDelDocente`, con `additionalProperties: false`, `required: [identificacion, nombre, horas]` y `dedicacion` opcional («falta si el docente no estaba activo al congelar»).
  - **La descripción de las descargas:** `REPORTE_GENERAL_DESCARGADO` y `reporte-general-<periodo>.pdf|csv`, en el encabezado `ContentDisposition`.
  - **El comentario de cabecera** del archivo nombra F20-H02 (#269, D-117).
- [X] T004 [P] En `contratos/generacion/openapi.yaml`, ampliar `DocenteCongelado` según `contracts/interno.md`:
  - `required: [identificacion, nombre, horas]`;
  - `dedicacion` opcional («Ausente si el docente ya no estaba activo al congelar»), con `$ref: Dedicacion`;
  - `horas` con `$ref: HorasPorComponente`.

  Actualizar la descripción de `consultarCargasCongeladas`: «el hash y los docentes, cada uno con su dedicación y sus horas (F20-H02, D-117)».
- [X] T005 [P] En `contratos/comun/errores.md`, sin códigos nuevos:
  - `REPORTING_SIN_PUBLICACION`: suma las tres operaciones nuevas;
  - `REPORTING_CONTENIDO_INCONSISTENTE`: suma las tres operaciones y la causa «un docente repetido en el contenido de la versión».

### Generación: las horas en el listado

- [X] T006 Primero, `DocenteDeLaVersionTest` (o los casos en la prueba que ya cubra el registro) en `GEN/test/java/.../dominio/`. Después, ampliar `DocenteDeLaVersion` en `GEN/main/java/.../dominio/` con `dedicacion` («`Dedicacion` (la de `CargaCongelada`), opcional») y `horas` («`CargaHoraria` (existente, la de `CargaCongelada`)», obligatoria). Ajustar los usos que lo construyen.
- [X] T007 Primero, ampliar `ContenidoCongeladoIntegrationTest` (`GEN/integrationTest/`) y verlo fallar en PostgreSQL real. Los casos:
  - los docentes de una versión guardada traen la dedicación y las horas de su carga, iguales a las de `carga(periodo, numero, docente)`;
  - un docente sin dedicación la trae vacía.

  Después, en `RepositorioContenidoCongeladoJdbc.docentes(periodo, numero)` (`GEN/main/java/.../adaptadores/salida/`), leer `contenido->>'dedicacion'` y `contenido->'horas'` en la misma consulta, con el mismo orden: por nombre y luego por identificación. La tabla no cambia. Generación es dominio central: TDD estricto (principio VIII).
- [X] T008 Primero, ampliar `InternoGeneracionContratoTest` y verlo fallar. Los casos: la respuesta con `dedicacion` y `horas`, y un docente sin `dedicacion`. Después, en `InternoGeneracionControlador` (`GEN/main/java/.../adaptadores/entrada/`), mapear los dos campos en `DocenteCongelado`.

### Reporting: lo que comparten las dos historias

- [X] T009 [P] Primero, `HorasPorComponenteTest` en `REP/test/java/.../dominio/`: `mas` suma componente por componente, `NINGUNA.mas(x)` es `x`, y el resultado conserva dos decimales sin redondear (`0.25 + 0.50 = 0.75`). Después, `HorasPorComponente.mas(HorasPorComponente otras)` en `REP/main/java/.../dominio/`.
- [X] T010 Ampliar `DocentesDeLaVersion.Docente` (`REP/main/java/.../dominio/`) con `dedicacion` (opcional, `Dedicacion`) y `horas` (`HorasPorComponente`, obligatorias).
  - **`ContenidoDesdeGeneracion`** (`adaptadores/salida/`) lee los dos campos nuevos con su esquema (principio XIV). Una respuesta sin `horas` es `ProcesoConError` (502).
  - **`RespuestasDeReporting`** sigue mapeando solo la identificación y el nombre en `listarDocentesDelPeriodo`.
  - **Las pruebas:** ampliar `ClientesDeReportingTest` con los campos nuevos y el 502 por la falta de `horas`. Ajustar los usos en `ConsultarReportesServicioTest`.

**Checkpoint:** la ruta interna devuelve las horas de cada docente y Reporting las lee. El reporte individual sigue igual (sus pruebas en verde).

---

## Phase 3: User Story 1 - El Rector consulta el reporte general del periodo (Priority: P1) 🎯 MVP

**Goal:** el Rector ve, para la versión publicada vigente, a cada docente con sus horas por componente y su total, y los totales institucionales.

**Independent Test:** con 2026-I publicado, `rectorado.demo` abre «Reportes», deja «General» y ve a todos los docentes de la versión con sus cinco componentes, «N docentes» y «Total institucional». Cada fila es igual al reporte individual del docente (paso R7).

### Dominio de Reporting

- [X] T011 [US1] Primero, `ReporteGeneralTest` en `REP/test/java/.../dominio/`. Después, `ReporteGeneral` y `FilaDelDocente` en `REP/main/java/.../dominio/`, según `data-model.md`. Casos:
  - las filas ordenadas por nombre y, a igual nombre, por identificación;
  - los totales iguales a la suma exacta de las filas en cada componente («La suma exacta de las horas de las filas, componente por componente»);
  - el total general igual a la suma de los totales de las filas;
  - un componente en 0 en toda la institución, que sigue presente;
  - las horas con dos decimales, sin redondear (por ejemplo, tres docentes con `0.33` dan `0.99`);
  - el hash distinto, que lanza `ContenidoInconsistente`;
  - un docente repetido («sin identificaciones repetidas»), que lanza `ContenidoInconsistente`;
  - una versión sin docentes: totales en 0 y `cantidadDeDocentes() == 0`;
  - `FilaDelDocente` sin dedicación: `dedicacion()` vacía.

  **Los datos de la prueba** (principio VIII):
  - se copia `06_docentes.csv` de los datos de prueba a `REP/test/resources/reporting/datos-de-prueba/`, como hacen Cargas y Docentes;
  - una clase de apoyo de las pruebas, `DocentesDePrueba` (`REP/test/java/.../dominio/`), lee de ese archivo la identificación, los apellidos y nombres y la dedicación;
  - `DocentesDePrueba.repetidos(n)` repite los 47 docentes hasta `n`, con una identificación derivada en el rango ficticio `010999xxxx` y un sufijo en el nombre (D-117). La usan T016 y T018;
  - los casos de esta tarea usan docentes de ese archivo (por ejemplo, `0109990010`, «PERALTA MALDONADO JUAN ANDRES», TC), y las horas de cada caso se eligen para probar la regla.

  Los nombres siguen el glosario (principio III), y el JavaDoc explica por qué los totales se suman en el dominio.

### Aplicación y entrada

- [X] T012 [US1] Primero, ampliar `ConsultarReportesServicioTest` (Mockito en los puertos). Después, `ConsultarReportes.reporteGeneral(String periodo, String quien)` en `REP/main/java/.../puertos/entrada/` y su implementación en `ConsultarReportesServicio` (`aplicacion/`), que reutiliza `publicacion(periodo)` y `docentesDe(publicacion)`. Casos:
  - el reporte de la publicación vigente, con el reloj y quién lo pide;
  - durante la reforma, sale de la versión publicada anterior (`enReforma: true`);
  - un periodo sin publicación lanza `SinPublicacion`;
  - Generación sin contenido para la versión lanza `ContenidoInconsistente`;
  - un hash distinto lanza `ContenidoInconsistente`.
- [X] T013 [US1] Implementar `consultarReporteGeneral` en `ReportesControlador` (`REP/main/java/.../adaptadores/entrada/`), con `@PreAuthorize("hasAuthority('PERMISO_REPORTES:LECTURA')")`. Sumar el mapeo de `ReporteGeneral` y `FilaDelDocente` a `RespuestasDeReporting`: la dedicación se omite si falta, y cada `HorasPorComponente` lleva su `total`.

  Ampliar `ReportingContratoTest`: la respuesta contra el contrato, 403 sin el permiso, 409, 500 sin datos técnicos, 502 y 400 con un periodo inválido. `ArquitecturaTest` sigue en verde.

### SPA

- [X] T014 [P] [US1] En `SPA/pantallas/reportes/`, según `contracts/spa.md`:
  - **El tipo generado:** regenerar `SPA/api/generado/reporting.d.ts`.
  - **`consultas.ts`:** sumar el tipo `ReporteGeneral` y `useReporteGeneral(periodo, habilitado)`. El JSON se pide solo con «General» elegido.
  - **`Reportes.tsx`:**
    - el grupo «Tipo de reporte» (`fieldset`, `legend`, dos `radio`: «General», propuesto, y «Por docente»);
    - la opción se mantiene al cambiar de periodo;
    - al pasar a «Por docente», el foco queda en el buscador;
    - con «General»: el encabezado (periodo, versión, hash con «Copiar», fecha de publicación, «N docentes», el aviso de la reforma) y la tabla «Docentes del distributivo publicado», con `caption`, `th scope="col"`, el nombre de cada docente en `th scope="row"`, la fila «Total institucional» en `tfoot`, las horas con `horas()` y la dedicación con `NOMBRE_DE_LA_DEDICACION` o «sin dato»;
    - mientras carga, «Cargando el reporte general…» con `aria-live="polite"`.
  - **`Reportes.test.tsx`:** sumar los casos de «General»: propuesto sin buscar a nadie, la tabla en orden con su fila de totales, «sin dato», el cambio a «Por docente» con el foco, y que no se pida el JSON del general con «Por docente». Los casos del individual suman el clic en «Por docente».

### Demostración

- [X] T015 [US1] En `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - sumar `ruta_del_general`;
  - ampliar **R1**: el general de 2026-I responde 409 `REPORTING_SIN_PUBLICACION` y la bitácora no tiene `REPORTE_GENERAL_DESCARGADO`;
  - el paso **R7**, después de R3, con `verificar R7 …`: la versión 1 y el hash de la publicación; los mismos docentes del listado, en el mismo orden y sin repetidos (SC-001); cada fila igual a las horas y al total del reporte individual del docente (SC-002); los totales iguales a la suma de las filas, y el total general a la suma de los cinco (SC-003); pedir el JSON no deja entrada en la bitácora;
  - ampliar **R4**: `coordinacion.demo` y `docente.demo` reciben 403 en el general y en su PDF, y un periodo inválido da 400 `COMUN_SOLICITUD_INVALIDA` (SC-008);
  - ampliar **R6**: con la versión 2 publicada, el general sale de ella, con su hash, y sus totales son la suma de sus filas.

  Actualizar el comentario de cabecera del script con F20-H02.

**Checkpoint:** el Rector ve el reporte general en la pantalla y por la API; R1, R4, R6 y R7 en verde.

---

## Phase 4: User Story 2 - El Rector descarga el reporte general en PDF y en CSV con los mismos datos (Priority: P1)

**Goal:** el reporte general se descarga en PDF/A-2b y en CSV, con los mismos docentes, horas y totales. Cada descarga queda en la bitácora antes de entregarse.

**Independent Test:** con el general en pantalla, `rectorado.demo` baja los dos archivos. El PDF pasa veraPDF sin campos. El CSV tiene una fila `DOCENTE` por docente y la fila `TOTAL`, con los mismos valores del PDF y del JSON. La bitácora tiene una entrada `REPORTE_GENERAL_DESCARGADO` por descarga (paso R8).

### Dominio

- [X] T016 [P] [US2] Primero, ampliar `EscritorCsvTest` en `REP/test/java/.../dominio/`. Después, `EscritorCsv.escribir(ReporteGeneral)` en `dominio/`, según `contracts/reporting.md` §CSV. Reutiliza `texto`, `horas` y `linea`, y extrae lo común sin cambiar los bytes del individual. Casos:
  - el BOM, `;`, CRLF y las 14 columnas en el orden del contrato: `Periodo`, `Versión`, `Hash de contenido`, `Publicada el`, `Tipo`, `Identificación`, `Docente`, `Dedicación`, `Clase`, `Otras actividades de docencia`, `Investigación`, `Vinculación`, `Gestión`, `Total`;
  - una fila `DOCENTE` por docente, en el orden del reporte, y una sola fila `TOTAL` al final, con `Docente` = `Total institucional` y la identificación y la dedicación vacías;
  - las horas con dos decimales y coma decimal, y los totales iguales a los del agregado;
  - «sin dato» si falta la dedicación;
  - cada uno de los seis caracteres de fórmula (`=`, `+`, `-`, `@`, tabulación, retorno de carro) al inicio de un nombre, con el apóstrofo delante;
  - las comillas internas duplicadas y las tildes y eñes en UTF-8;
  - los mismos bytes para la misma versión pedida por otra persona y en otro momento (FR-011);
  - los bytes del CSV individual no cambian (los casos de F20-H01 siguen en verde);
  - **el volumen (SC-007):** el CSV de los mismos 300 docentes de T018 tiene 301 filas de datos, y su tiempo de escritura se escribe en la salida de la prueba para la evidencia.
- [X] T017 [P] [US2] Primero, `DescargaDeReporteTest` en `REP/test/java/.../dominio/`. Después, `TipoDeReporte` (`INDIVIDUAL`, `GENERAL`) y el campo `tipo` en `DescargaDeReporte`, con las fábricas `individual(...)` y `general(...)`. Casos:
  - la identificación es «Obligatoria si el tipo es `INDIVIDUAL`; debe faltar (`null`) si es `GENERAL`»: el constructor rechaza las dos combinaciones inválidas;
  - cada tipo da su acción (`REPORTE_INDIVIDUAL_DESCARGADO`, `REPORTE_GENERAL_DESCARGADO`) y su nombre de archivo (`reporte-individual-<periodo>-<identificacion>.<ext>`, `reporte-general-<periodo>.<ext>`).

  Ajustar `DescargarReporteServicio` para que use `DescargaDeReporte.individual(...)`, sin cambiar su comportamiento.

### PDF/A

- [X] T018 [P] [US2] Primero, ampliar `GeneradorDeReportePdfaTest` (`REP/test/java/.../adaptadores/salida/`) con un reporte general de **300 docentes**.
  - **Los datos:** los 300 docentes de `DocentesDePrueba.repetidos(300)`, creada en T011 (D-117, principio VIII).

  Después, `GeneradorDePdf.generar(ReporteGeneral)` en `REP/main/java/.../puertos/salida/` y su implementación en `GeneradorDeReportePdfa`, según research §3:
  - **El renderizado común:** extraer lo que comparten los dos reportes (PDF/A-2b, DejaVu Sans incrustada, sRGB y el pie con «Periodo P · Versión N · Hash H · Página X de Y»). El caso del individual sigue en verde.
  - **La página:** `@page { size: A4 landscape }`.
  - **El encabezado del documento:** «Reporte general del distributivo», el periodo, la versión, el hash, «Publicada el …», «N docentes» y «Generado por … el …».
  - **La tabla:**
    - las columnas Identificación, Docente, Dedicación, Clase, Otras actividades de docencia, Investigación, Vinculación, Gestión y Total;
    - el encabezado en `thead` (`display: table-header-group`);
    - «Total institucional» como **última fila del cuerpo**, en negrita, no en `tfoot`;
    - `page-break-inside: avoid` en las filas;
    - los nombres largos se parten sin salirse de la página.
  - **La prueba verifica:**
    - veraPDF 2B conforme;
    - sin `AcroForm`, anotaciones ni cifrado;
    - el encabezado de la tabla y el pie en cada página;
    - «Total institucional» una sola vez, en la última página;
    - el tiempo de generación, que se escribe en la salida de la prueba para la evidencia (SC-007, por debajo de 10 s).

### Bitácora y descargas

- [X] T019 [US2] En `RegistroDeDescargasEventos` (`REP/main/java/.../adaptadores/salida/`), elegir la acción por `TipoDeReporte`. Para `GENERAL`, el objeto es `{periodo, version}`, sin identificación, y el valor nuevo es `{formato}`.
  - **Las pruebas unitarias:** ampliar o crear `RegistroDeDescargasEventosTest` con los dos tipos.
  - **La prueba de integración:** ampliar `RegistroDeDescargasIntegrationTest` (`backend/apps/salida/src/integrationTest/java/ec/mdd/`) con una descarga del general: el evento queda en `eventos_salida.event_publication` dentro de la transacción, y no queda nada si se revierte.
- [X] T020 [US2] Primero, ampliar `DescargarReporteServicioTest` (Mockito). Después, `DescargarReporte.descargarGeneral(String periodo, Formato formato, String quien)` en `REP/main/java/.../puertos/entrada/` y su implementación en `DescargarReporteServicio`, con el mismo orden que el individual. Casos:
  - genera el archivo completo, registra `DescargaDeReporte.general(...)` en la transacción y recién entonces devuelve `ArchivoDelReporte` con `reporte-general-<periodo>.pdf|csv`;
  - si la generación falla, no hay registro;
  - si el registro falla, no se entrega el archivo;
  - `SinPublicacion` y `ContenidoInconsistente` se propagan sin registro.
- [X] T021 [US2] Implementar `descargarReporteGeneralPdf` y `descargarReporteGeneralCsv` en `DescargasControlador` (`REP/main/java/.../adaptadores/entrada/`), con `@PreAuthorize`, `Content-Disposition: attachment` y `Cache-Control: no-store`, reutilizando `respuesta(...)`.

  Ampliar `DescargasContratoTest`:
  - los encabezados y el tipo de contenido de las dos descargas;
  - 403, 409, 500 sin datos técnicos, 502 y 400.

### SPA

- [X] T022 [P] [US2] En `SPA/pantallas/reportes/`, según `contracts/spa.md` §Las descargas:
  - **`Descargas`:** generalizar el componente para que reciba la ruta del reporte en lugar de la identificación.
  - **`descargarReporte`** (`consultas.ts`): admitir las dos rutas.
  - **La pantalla:** mostrar «Descargar PDF» y «Descargar CSV» con «General» elegido, con el nombre del `Content-Disposition`, el modal `AVISO_DEL_CSV` al bajar el CSV y el error del catálogo con `role="alert"`.

  `Reportes.test.tsx` suma: las dos descargas del general con el nombre del servidor, el modal del CSV y el error de una descarga.
- [X] T023 [P] [US2] En `frontend/e2e/reportes.spec.ts`:
  - `rectorado.demo` ve el reporte general y descarga los dos archivos, con sus nombres;
  - los casos del individual suman el clic en «Por docente»;
  - axe-core (WCAG 2.1 AA) con «General» y con «Por docente», sin fallas (SC-009).

### Demostración

- [X] T024 [US2] En `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - **Las funciones:** generalizar `bajar` (recibe la ruta) y `csv_igual_al_json` (recibe el tipo, y para el general compara cada fila `DOCENTE` y la fila `TOTAL` con el JSON, además del BOM, CRLF, la versión y el hash).
  - **El paso R8**, después de R7, con `verificar R8 …`:
    - `rectorado.demo` y `secretaria.demo` bajan el PDF y el CSV del general;
    - el PDF es PDF/A sin formulario (`pdf_sin_campos`), con `reporte-general-2026-I.pdf` y `Cache-Control: no-store`;
    - el CSV es igual al JSON (SC-004), y el de un usuario es idéntico byte a byte al del otro;
    - hay 4 entradas `REPORTE_GENERAL_DESCARGADO`, 2 con `CSV`, con el objeto sin identificación.
  - **R5 ampliado:** con la hora del paralelo bajada, el CSV del general es idéntico al de R8.

**Checkpoint:** las dos descargas funcionan, quedan en la bitácora y coinciden con el JSON; R5 y R8 en verde.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T025 Correr el build completo (`./gradlew build integrationTest`), la SPA (`npm run tipos`, `lint`, `formato:verificar`, `test`) y Playwright. Comprobar:
  - la cobertura de Reporting y Generación (mínimo 60 %, D-32);
  - Spotless, SpotBugs, ArchUnit y Spring Modulith en verde;
  - Gitleaks sobre `main..HEAD`;
  - el script completo contra el perfil local (R1 a R8).
- [X] T026 [P] Revisar el JavaDoc (principio XII) en `ReporteGeneral`, `EscritorCsv`, `GeneradorDeReportePdfa`, `DescargaDeReporte` y `DocenteDeLaVersion`:
  - por qué los totales se suman en el dominio y no en Generación;
  - por qué el general y el individual leen las mismas horas guardadas;
  - por qué «Total institucional» no va en `tfoot` en el PDF;
  - por qué el CSV del general es ancho y el del individual, largo;
  - por qué la identificación falta en la descarga del general.
- [X] T027 [P] Medir SC-007 en dos partes y anotar los resultados en la evidencia:
  - **con 300 docentes:** los tiempos de generación del PDF (T018) y del CSV (T016);
  - **de punta a punta con la demo:** en el perfil local, el tiempo de cada descarga del general (`curl -w '%{time_total}'`), que suma las dos lecturas internas, la generación, el registro en la bitácora y la red.

  SC-007 se da por cumplido si el PDF y el CSV de 300 docentes tardan menos de 10 s y, además, cada descarga de la demo restada de su propia generación deja un costo fijo (lecturas, registro y red) que, sumado a la generación de 300, sigue por debajo de 10 s. Si no se cumple, se anota como desviación con su causa.
- [X] T028 El recorrido del autor, `quickstart.md` §1 a §4, con el teclado y un lector de pantalla (SC-007, SC-009). Incluye:
  - abrir el CSV en una hoja de cálculo con la configuración de Ecuador;
  - verificar el PDF con veraPDF;
  - comparar los totales institucionales con los del PDF/A del consolidado firmado. Si difieren, se anota como hallazgo (Riesgos del plan).
- [X] T029 Crear `specs/059-reporte-general-consolidado/evidencia.md`, como la de 058: las pruebas, el script, el recorrido, las mediciones, las desviaciones y los criterios de terminado. La sección del equipo de demostración queda para después de fusionar.
- [ ] T030 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - **Verificar la demo en solo lectura:** «Sano», y si 2026-I está publicado, un reporte general descargado.
  - Abrir el PR de evidencia con `Closes #269`. Con eso F20 (#259) queda completa.

---

## Dependencies & Execution Order

- **Setup:** T001 y T002 antes de todo.
- **Fundacional:**
  - T003, T004 y T005 en paralelo;
  - T006 después de T004;
  - T007 después de T006;
  - T008 después de T007;
  - T009 en cualquier momento después de T002 (dominio puro);
  - T010 después de T004, T008 y T009.
- **US1:**
  - T011 después de T009 y T010;
  - T012 después de T011;
  - T013 después de T003, T005 y T012;
  - T014 después de T003;
  - T015 después de T013 y T014.
- **US2:**
  - T016, T017 y T018 después de T011, en paralelo;
  - T019 después de T017;
  - T020 después de T012, T016, T018 y T019;
  - T021 después de T013 y T020;
  - T022 después de T014;
  - T023 después de T021 y T022;
  - T024 después de T015 y T021.
- **Cierre:**
  - T025 después de US1 y US2;
  - T026 y T027 en paralelo con T025;
  - T028 después de T025;
  - T029 después de T025, T027 y T028;
  - T030 al final.

US2 usa el agregado (T011) y la consulta (T012) de US1, pero se prueba por separado: las descargas tienen su propio servicio, su controlador y sus pasos (R5 y R8).

### Parallel Opportunities

```text
T003, T004 y T005 (contratos), y T009 (HorasPorComponente.mas) en paralelo con la cadena T006 → T008 de Generación.
T014 (SPA) en paralelo con el backend de US1, una vez hecho T003.
T016 (CSV), T017 (DescargaDeReporte) y T018 (PDF/A) en paralelo, una vez hecho T011.
T022 y T023 (SPA) en paralelo con T020 y T021.
T026 y T027 en paralelo con T025.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. La fundación: T001 a T010. La ruta interna devuelve las horas, y el reporte individual sigue en verde.
2. US1: T011 a T015. Con eso, el Rector ya ve el reporte general en la pantalla.
3. Validar con `ReportingContratoTest` y los pasos R1, R4, R6 y R7.

### Incremental Delivery

1. La fundación.
2. US1, el reporte general en pantalla.
3. US2, las descargas y la bitácora, con R5 y R8.
4. El cierre: el build, las mediciones, el recorrido, la evidencia y la fusión.

Una sola rama (`059-reporte-general-consolidado`) y un solo PR, como en las historias anteriores. El PR principal lleva `Refs #269`, y el de evidencia, `Closes #269`.
