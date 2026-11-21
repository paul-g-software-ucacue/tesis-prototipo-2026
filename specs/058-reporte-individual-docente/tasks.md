---
description: "Tareas de F20-H01: reporte individual del docente en PDF y CSV"
---

# Tasks: Reporte individual del docente en PDF y CSV

**Input**: documentos de diseño en `specs/058-reporte-individual-docente/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto. Generación es dominio central (TDD estricto).

**Rutas**:
- `REP/` es `backend/servicios/reporting/src/`;
- `GEN/` es `backend/servicios/generacion/src/`;
- `OCS/` es `backend/servicios/validacionocs/src/`;
- `USU/` es `backend/servicios/usuarios/src/`;
- `SPA/` es `frontend/src/`;
- `.../` es el paquete `ec/mdd/reporting/`, `ec/mdd/generacion/`, `ec/mdd/validacionocs/` o `ec/mdd/usuarios/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de una tarea pendiente).
- **[Story]**: US1 (el reporte de un docente) o US2 (las descargas en PDF y CSV).

---

## Phase 1: Setup

- [X] T001 Escribir **D-115** en `docs/decisiones.md`, en la fila del índice y en su sección, con research §1 a §10:
  - Reporting lee la publicación vigente de Aprobación y el contenido de Generación por rutas internas;
  - Generación guarda el contenido congelado por docente al congelar, por orden de Aprobación, y lo calcula «tardío» si falta. El problema que resuelve: las horas de clase salen de la oferta actual y Académico permite corregirla después de publicar;
  - la comparación del hash;
  - el PDF/A-2b en un adaptador propio de Reporting, sin módulo compartido todavía;
  - el CSV con `;`, coma decimal, BOM y fórmulas neutralizadas con apóstrofo;
  - las rutas públicas y la publicación más reciente como periodo por defecto (el ajuste de FR-014);
  - la bitácora antes de responder, por el registro de publicaciones de `salida`;
  - los tres errores;
  - el caso aceptado: una corrección de la oferta en los milisegundos entre la congelación y el guardado.

  En «Documentos que hay que poner al día», anotar que el glosario (`sdd/modelo_dominio.md` §7) suma «Contenido congelado» (`CargaCongelada`, Generación).
- [X] T002 Crear la rama `058-reporte-individual-docente` desde `main` actualizado y confirmar `.specify/feature.json`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T013. Esta fase deja guardado el contenido congelado, las rutas internas y el proceso `salida` con su token y su registro de publicaciones.

### Contratos

- [X] T003 [P] El contrato nuevo `contratos/reporting/openapi.yaml` (OpenAPI 3.0.3), según `contracts/reporting.md`:
  - las cinco operaciones (`listarPublicaciones`, `listarDocentesDelPeriodo`, `consultarReporteIndividual`, `descargarReporteIndividualPdf` y `descargarReporteIndividualCsv`), todas con `x-mdd-permiso: REPORTES:LECTURA`;
  - los esquemas `Publicacion` («`version` integer ≥ 1», «`hash` 64 hex», «`publicadaEn` date-time», «`enReforma` boolean»), `DocentesDelPeriodo`, `ReporteIndividual` y `HorasPorComponente` («`number` con hasta dos decimales, ≥ 0»), con `additionalProperties: false`;
  - `periodo` como `CodigoDePeriodo` (`^[A-Za-z0-9._-]{1,20}$`) y `identificacion` como `NumeroDeIdentificacion` (`^[0-9A-Z]{1,13}$`);
  - los encabezados `Content-Disposition` y `Cache-Control: no-store` de las descargas;
  - las respuestas 400, 401, 403, 404, 409, 500, 502 y 504.

  Generar el cliente de la SPA (`SPA/api/generado`) con el script que ya usan los demás contratos.
- [X] T004 [P] Las rutas internas en `contratos/validacionocs/openapi.yaml` (`consultarPublicacionesVigentes`, `PublicacionVigente`) y en `contratos/generacion/openapi.yaml` (`consultarCargasCongeladas`, `consultarCargaCongelada`, `CargasCongeladas`, `CargaCongeladaDelDocente`), según `contracts/interno.md`, con su descripción, `additionalProperties: false` y el 404 `NoEncontrada`.
  - En `contratos/usuarios/openapi.yaml`, la descripción de `recibirEvento` nombra siete servicios, con `reporting`.
- [X] T005 [P] Los tres errores en `contratos/comun/errores.md`, con sus ocho columnas, según `contracts/reporting.md`: `REPORTING_SIN_PUBLICACION` (409), `REPORTING_DOCENTE_SIN_DISTRIBUTIVO` (404) y `REPORTING_CONTENIDO_INCONSISTENTE` (500, sistema).

### Generación: el contenido congelado (dominio central, TDD)

- [X] T006 `CargaCongeladaTest` en `GEN/test/java/.../dominio/`, y después `CargaCongelada`, `AsignaturaCongelada` y `ActividadCongelada` en `GEN/main/java/.../dominio/`, según `data-model.md`:
  - «`dedicacion` TC, MT o TP; vacío si ya no estaba activo»;
  - «`horasContratadas` de 1 a 40»;
  - el orden de las asignaturas (carrera, código y paralelo) y de las actividades (componente y nombre);
  - el invariante: la suma de las asignaturas es `horas.clase`, y la de las actividades de cada componente es ese componente.
- [X] T007 `V10__contenido_congelado.sql` en `GEN/main/resources/db/migration/generacion/`:
  - las tablas `contenido_congelado` («`hash` char(64)», «`tardio` boolean», clave `(periodo, version)») y `carga_congelada` («`contenido` jsonb», clave `(periodo, version, docente)»), con su referencia;
  - `GRANT SELECT, INSERT` al rol `distributivo`, sin `UPDATE` ni `DELETE`.

  Prueba en `GeneracionEsquemaIntegrationTest`: las tablas existen y el rol no puede actualizarlas ni borrarlas.
- [X] T008 `RepositorioContenidoCongelado` en `GEN/main/java/.../puertos/salida/` y `RepositorioDistributivos.version(periodo, numero)`. Sus adaptadores `RepositorioContenidoCongeladoJdbc` y `RepositorioDistributivosJdbc.version` van en `adaptadores/salida/`. `guardar` inserta la cabecera y las cargas con `ON CONFLICT DO NOTHING`.

  `ContenidoCongeladoIntegrationTest` en `GEN/integrationTest/`, en PostgreSQL real:
  - guardar dos veces deja una cabecera;
  - la versión 1 se lee con la 2 vigente;
  - el JSON vuelve igual.
- [X] T009 Extraer en `ContenidoCongeladoServicio` (`GEN/main/java/.../aplicacion/`) el cálculo de los docentes para **una versión dada**, sin cambiar su resultado para la vigente (la prueba de F13-H02 sigue verde). Sumar la dedicación y las horas contratadas, leídas de Docentes por el puerto que ya usa Generación (`DocenteActivo`).
- [X] T010 `GuardarContenidoCongeladoServicioTest` y `ConsultarContenidoCongeladoServicioTest` (Mockito en los puertos). Después, los puertos de entrada `GuardarContenidoCongelado` y `ConsultarContenidoCongelado` y sus servicios en `GEN/main/java/.../aplicacion/`. Casos:
  - guardar es idempotente;
  - si la versión no es la vigente o no está CONGELADA, no hace nada y lo anota;
  - consultar una versión sin contenido lo calcula, lo guarda con `tardio = true` y anota un `WARN` con el periodo y la versión;
  - una versión que no existe o no está congelada da vacío;
  - un docente ausente da vacío.
- [X] T011 `InternoGeneracionControlador` en `GEN/main/java/.../adaptadores/entrada/`, con las dos rutas de `contracts/interno.md`, los patrones del periodo, el número y la identificación, y el 404 `NoEncontrada`.
  - `InternoGeneracionContratoTest`: guardado, tardío, no congelada, docente ausente, 400 y 403 con un token de usuario.

### Validación OCS: la publicación vigente y la orden de guardar

- [X] T012 [P] En Validación OCS:
  - `ConsultarPublicacionesServicioTest`, y después `ConsultarPublicaciones`, `PublicacionVigente` y `ConsultarPublicacionesServicio`, con `ProcesoAprobacion.publicacion()` y `enReforma`;
  - `InternoValidacionocsControlador`, con `InternoValidacionocsContratoTest`: sin publicaciones, con una, durante una reforma y 403;
  - el oyente `ContenidoDeLaFirma` (`@ApplicationModuleListener` sobre `AvisoDeFirma.DistributivoEnFirma`) en `OCS/main/java/.../adaptadores/salida/`, que llama a `GuardarContenidoCongelado`, registrado en `ConfiguracionValidacionocs`. JavaDoc como en `PreparacionDelDocumento`;
  - una prueba de integración del proceso `distributivo`: al entrar en firma queda guardado el contenido de la versión.

### Proceso `salida`, Identidad y Usuarios

- [X] T013 La infraestructura de `salida`:
  - `backend/apps/salida/build.gradle` suma `plataforma:eventos`;
  - `backend/apps/salida/src/main/resources/application.yaml` suma:
    - `spring.modulith.events.jdbc.schema: eventos_salida`;
    - `mdd.eventos.*`: proceso `salida`, el destino `usuarios`, el token de proceso y `MDD_CLAVE_PROCESO_SALIDA`;
    - `mdd.procesos.distributivo: ${MDD_URI_DISTRIBUTIVO:http://distributivo:8080}`;
  - `backend/apps/identidad/src/main/resources/application.yaml` suma `mdd.usuarios.procesos.salida.clave: ${MDD_CLAVE_PROCESO_SALIDA:}`;
  - `compose.yaml` y los secretos de `deploy/desplegar.sh` pasan la clave a `identidad` y a `salida`, y el despliegue falla si falta (D-55);
  - en `USU/main/java/.../`, `RegistrarEventoRecibidoServicio`, `dominio/EntradaBitacora` y `dominio/FiltroDeBitacora` admiten el origen `reporting`.

  Pruebas: `RegistrarEventoRecibidoServicioTest` y `RecibirEventoContratoTest` con `reporting`; `EmitirTokenDeProcesoServicioTest` con `salida`; `SalidaAplicacionTest` arranca con PostgreSQL real y migra `eventos_salida`.

**Checkpoint**: el contenido congelado se guarda al entrar en firma, las tres rutas internas responden y `salida` obtiene su token de proceso.

---

## Phase 3: User Story 1 - El Rector consulta el reporte individual de un docente (Priority: P1) 🎯 MVP

**Goal**: `rectorado.demo` elige un periodo publicado y un docente, y ve sus asignaturas, actividades y horas por componente con el total, de la versión publicada vigente.

**Independent Test**: spec US1. Con 2026-I publicado, «Reportes» muestra el reporte de `docente.demo` con la versión y el hash de la publicación, y sus totales coinciden con el consolidado. Pasos R1, R2 y R4 de `contracts/demostracion.md`.

### Dominio de Reporting

- [X] T014 [US1] `ReporteIndividualTest` en `REP/test/java/.../dominio/`. Después, en `REP/main/java/.../dominio/`: `ReporteIndividual`, `Publicacion`, `DatosDelDocente`, `FilaDeAsignatura`, `FilaDeActividad`, `HorasPorComponente`, `SinPublicacion`, `DocenteSinDistributivo` y `ContenidoInconsistente`. Casos:
  - `ReporteIndividual.de` exige que el hash de la publicación sea el del contenido; si no, `ContenidoInconsistente`;
  - los totales son la suma de sus filas;
  - los cinco componentes siempre están, también con 0, en el orden del glosario, más `total()`;
  - las horas tienen «hasta dos decimales» y son «≥ 0»;
  - el orden de las filas: asignaturas por carrera, código y paralelo; actividades por componente y nombre;
  - un docente sin asignaturas o sin actividades;
  - `dedicacion` vacía se muestra «sin dato».
- [X] T015 [US1] Los puertos de salida `PublicacionesDelDistributivo` y `ContenidoDeLaVersion` en `REP/main/java/.../puertos/salida/`. Sus adaptadores `PublicacionesDesdeAprobacion` y `ContenidoDesdeGeneracion` van en `adaptadores/salida/`, con `ConsultasEntreProcesos`, y `build.gradle` de Reporting suma `plataforma:eventos`. Un 404 es vacío; un error, `ProcesoConError`; un plazo vencido, `ProcesoSinRespuesta`.
  - `ClientesDeReportingIntegrationTest`, con un servidor HTTP de prueba: 200, 404, 500 (502 común) y una demora (504 común).

### Aplicación y entrada

- [X] T016 [US1] `ConsultarReportesServicioTest` (Mockito en los puertos). Después, `ConsultarReportes` en `REP/main/java/.../puertos/entrada/` y `ConsultarReportesServicio` en `aplicacion/`. Casos:
  - `publicaciones()` ordena de la más reciente a la más antigua;
  - `docentes(periodo)` sin publicación da `SinPublicacion`;
  - `reporte` de un docente ausente da `DocenteSinDistributivo`;
  - durante una reforma, el reporte sale de la versión publicada anterior;
  - el reporte se arma con quien lo pide y el reloj.
- [X] T017 [US1] `ReportesControlador` (las tres operaciones de consulta) y `ErroresReporting` en `REP/main/java/.../adaptadores/entrada/`, con `@PreAuthorize` de `REPORTES:LECTURA` y la validación de los patrones (principio XIV). `ConfiguracionReporting` en `infraestructura/` registra los beans.
  - `ReportingContratoTest`, para cada operación: el 200 contra el esquema, el 403 sin el permiso, el 400, el 404, el 409, el 500 sin trazas y el 502.
- [X] T018 [US1] `ArquitecturaTest` de Reporting: el dominio sin Spring ni HTTP; los clientes solo en adaptadores.

### SPA

- [X] T019 [P] [US1] La pantalla `SPA/pantallas/reportes/Reportes.tsx` con `Reportes.test.tsx`, según `contracts/spa.md`:
  - los estados de carga, sin publicaciones y error;
  - la lista de periodos, que propone el más reciente;
  - el aviso de reforma en curso;
  - el buscador de docentes por nombre o identificación;
  - el encabezado con el hash y «Copiar»;
  - las tres tablas con `caption` y `th scope`, y sus vacíos;
  - el foco al cargar el reporte.

  En `SPA/rutas.tsx`, la ruta `/reportes` con `RutaConPermiso funcionalidad="REPORTES" tipo="LECTURA"`. En `SPA/navegacion/`, el enlace «Reportes», y `enlaces.test.ts` comprueba los cinco roles que lo ven y los cinco que no.

### Demostración

- [X] T020 [US1] En `deploy/probar_documento_firmable.sh`:
  - la función `reporte()`, sin las descargas todavía;
  - los pasos R1 (dentro del paso 13, en EN_OCS), R2 y R4 (después de publicar la versión 1), según `contracts/demostracion.md`;
  - el comentario del encabezado puesto al día.

**Checkpoint**: el reporte se ve en la pantalla y por la API, con su versión y su hash; sin permiso, 403.

---

## Phase 4: User Story 2 - El Rector descarga el reporte en PDF y en CSV con los mismos datos (Priority: P1)

**Goal**: el mismo reporte se descarga en PDF/A-2b no editable y en CSV, con los mismos datos, y cada descarga queda en la bitácora.

**Independent Test**: spec US2. Descargar los dos archivos de `docente.demo`:
- el PDF pasa veraPDF 2B y no tiene formulario;
- el CSV tiene las mismas filas y totales;
- la bitácora tiene una entrada por descarga.

Pasos R3, R5 y R6 de `contracts/demostracion.md`.

### Dominio

- [X] T021 [P] [US2] `EscritorCsvTest` en `REP/test/java/.../dominio/`, y después `EscritorCsv` y `Formato` (`PDF`, `CSV`) en `dominio/`, según research §5. Casos:
  - el BOM, `;`, CRLF y las comillas dobles en el texto;
  - las columnas en el orden de research §5;
  - una fila por asignatura (`ASIGNATURA`, componente `Clase`) y por actividad (`ACTIVIDAD`);
  - seis filas `TOTAL`: los cinco componentes, también con 0, y `Total`;
  - las horas con dos decimales y coma (`12,50`);
  - una celda que empieza con `=`, `+`, `-`, `@`, tabulación o retorno de carro lleva un apóstrofo delante;
  - las tildes y la eñe;
  - las comillas dentro del texto;
  - la misma entrada da los mismos bytes;
  - no lleva quién lo generó ni cuándo.

### PDF/A

- [X] T022 [P] [US2] `GeneradorDeReportePdfaTest`, y después el puerto `GeneradorDePdf` (`REP/main/java/.../puertos/salida/`) y su adaptador `GeneradorDeReportePdfa` (`adaptadores/salida/`), según research §4:
  - openhtmltopdf con PDFBox, `PDFA_2_B`, DejaVu Sans incrustada y el perfil sRGB;
  - sin `AcroForm`, sin anotaciones ni JavaScript;
  - el pie de cada página con el periodo, la versión, el hash y «página X de Y»;
  - `thead` repetido entre páginas;
  - el texto escapado para XHTML;
  - quién lo generó y cuándo.

  Pruebas:
  - veraPDF 2B conforme;
  - PDFBox no encuentra `AcroForm` ni anotaciones;
  - un docente con 40 filas tiene el pie en cada página;
  - un nombre muy largo no se sale de la página;
  - el texto con `=` se ve tal cual.

  `REP/main/resources/fuentes/` lleva `DejaVuSans.ttf` y `LICENSE-DejaVu.txt`, copiadas de Firma. El `build.gradle` de Reporting suma PDFBox y openhtmltopdf, y veraPDF con el bloque de JAXB 2.3 solo en las pruebas, copiado de Firma con su comentario. `ArquitecturaTest`: PDFBox y openhtmltopdf solo en los adaptadores.

### Bitácora y descargas

- [X] T023 [US2] El puerto `RegistroDeDescargas` y `Transaccion` en `REP/main/java/.../puertos/salida/`, con sus adaptadores `RegistroDeDescargasEventos` y `TransaccionSpring` en `adaptadores/salida/`. Publica `reporting.EntradaBitacora` según `data-model.md`:
  - «`accion` `REPORTE_INDIVIDUAL_DESCARGADO`»;
  - «`objeto` `{periodo, version, identificacion}`»;
  - «`nuevo` `{formato}`»;
  - sin `anterior`, sin el nombre ni las horas.

  `RegistroDeDescargasIntegrationTest`, en PostgreSQL real: el evento queda en `eventos_salida.event_publication` dentro de la transacción, y no queda si la transacción se revierte.
- [X] T024 [US2] `DescargarReporteServicioTest` (Mockito). Después, `DescargarReporte` y `ArchivoDelReporte` (los bytes, el tipo y el nombre `reporte-individual-<periodo>-<identificacion>.pdf|csv`) en `REP/main/java/.../puertos/entrada/`, y `DescargarReporteServicio` en `aplicacion/`. Casos:
  - genera el archivo completo y lo registra en una transacción antes de devolverlo;
  - si el registro falla, no devuelve nada;
  - si falla la generación, no registra nada;
  - los mismos errores de `ConsultarReportes`.
- [X] T025 [US2] Las dos operaciones de descarga en `ReportesControlador`, con `Content-Disposition` y `Cache-Control: no-store`. `ReportingContratoTest` suma:
  - los encabezados;
  - el tipo `application/pdf` y `text/csv; charset=UTF-8`;
  - el 403, el 404 y el 409;
  - que consultar el JSON no registra nada.
- [X] T026 [P] [US2] En `SPA/pantallas/reportes/Reportes.tsx`, los botones «Descargar PDF» y «Descargar CSV»:
  - piden el archivo con el token y lo guardan con el nombre de `Content-Disposition`;
  - dicen «Descargando…» y quedan deshabilitados mientras tanto;
  - muestran el error junto a ellos.

  `Reportes.test.tsx` suma los dos nombres de archivo y el error.
- [X] T027 [P] [US2] `frontend/e2e/reportes.spec.ts` con axe-core, según `contracts/spa.md`: `rectorado.demo` elige un docente y descarga los dos archivos; `coordinacion.demo` no ve el enlace y recibe el acceso no autorizado en `/reportes`; sin fallas WCAG 2.1 AA.

### Demostración

- [X] T028 [US2] En `deploy/probar_documento_firmable.sh`:
  - la función `reporte()` completa: el PDF con `pdfinfo` y `qpdf --check`, y el CSV contra el JSON con `jq`;
  - los pasos R3 (con la versión 1), R5 (durante la reforma, con `coordinacion.demo` corrigiendo las horas de un paralelo de `docente.demo`) y R6 (con la versión 2);
  - `pdfinfo` y `qpdf` en el paso de preparación del job `perfil-consolidado` y en el contenedor de herramientas del equipo de demostración.

**Checkpoint**: los dos archivos se descargan con los mismos datos. El PDF es PDF/A-2b sin campos. Cada descarga está en la bitácora. El reporte de la versión 1 no cambia al corregir la oferta.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T029 Correr el build completo (`./gradlew build integrationTest`), la SPA (`npm run tipos`, `lint`, `formato:verificar`, `test`) y Playwright. Comprobar:
  - la cobertura de Reporting y Generación (mínimo 60 %, D-32);
  - el SBOM y Grype de la imagen de `salida`, que ahora trae PDFBox;
  - Gitleaks sobre `main..HEAD`;
  - el script contra el perfil local (`quickstart.md`).
- [X] T030 [P] Revisar el JavaDoc (principio XII):
  - por qué el contenido se guarda al congelar y qué significa «tardío»;
  - por qué se compara el hash;
  - por qué `;`, coma decimal y BOM;
  - por qué se registra la descarga antes de responder;
  - por qué el PDF no se cifra.
- [X] T031 Medir SC-005 (cada descarga en menos de 5 s con los datos de prueba) y repetir `deploy/medir_memoria.sh` con `salida` cargando PDFBox. Anotar los dos resultados en la evidencia.
- [ ] T032 El recorrido del autor, `quickstart.md` §1 a §4, con el teclado y un lector de pantalla (SC-005, SC-007). Incluye abrir el CSV en una hoja de cálculo con la configuración de Ecuador y verificar el PDF con veraPDF.
- [X] T033 Crear `specs/058-reporte-individual-docente/evidencia.md`, como la de 057: las pruebas, el script, el recorrido, las mediciones, las desviaciones y los criterios de terminado. La sección del equipo de demostración queda para después de fusionar.
- [ ] T034 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - **Verificar la demo en solo lectura:** «Sano», V10 de Generación aplicada, `MDD_CLAVE_PROCESO_SALIDA` presente y un reporte de 2026-I descargado (el contenido de la versión publicada queda «tardío», lo que es esperado).
  - Abrir el PR de evidencia con `Closes #268`.

---

## Dependencies & Execution Order

- **Setup:** T001 y T002 antes de todo.
- **Fundacional:**
  - T003, T004 y T005 en paralelo;
  - T006 después de T001;
  - T007 en paralelo con T006;
  - T008 después de T006 y T007;
  - T009 después de T006;
  - T010 después de T008 y T009;
  - T011 después de T004 y T010;
  - T012 después de T004 y T010;
  - T013 después de T004.
- **US1:**
  - T014 sin dependencias de la fase 2 (dominio puro);
  - T015 después de T011, T012 y T013;
  - T016 después de T014 y T015;
  - T017 después de T003, T005 y T016;
  - T018 después de T017;
  - T019 después de T003;
  - T020 después de T017 y T019.
- **US2:**
  - T021 después de T014;
  - T022 después de T014;
  - T023 después de T013;
  - T024 después de T016, T021, T022 y T023;
  - T025 después de T017 y T024;
  - T026 después de T019;
  - T027 después de T025 y T026;
  - T028 después de T020 y T025.
- **Cierre:**
  - T029 después de US1 y US2;
  - T030 y T031 en paralelo con T029;
  - T032 después de T029;
  - T033 después de T029, T031 y T032;
  - T034 al final.

US2 usa el agregado de US1 (T014) y su servicio de consulta (T016), pero se prueba por separado: las descargas tienen su propio servicio, su controlador y sus pasos.

### Parallel Opportunities

```text
T003, T004 y T005 (contratos), y T006 con T007.
T012 (Validación OCS) y T013 (salida, Identidad y Usuarios) en paralelo con T011.
T014 (dominio de Reporting) en cualquier momento, sin la fase 2.
T019 (SPA) en paralelo con el backend de US1, una vez hecho T003.
T021 (CSV) y T022 (PDF/A) en paralelo, una vez hecho T014; T023 en paralelo con los dos.
T026 y T027 (SPA) en paralelo con T024 y T025.
T030 y T031 en paralelo con T029.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. La fundación: T001 a T013. El contenido congelado ya se guarda al congelar y las rutas internas responden.
2. US1: T014 a T020. Con eso, el Rector ya ve el reporte de cualquier docente de la versión publicada.
3. Validar con `ReportingContratoTest` y los pasos R1, R2 y R4.

### Incremental Delivery

1. La fundación, que también deja listo el contenido que necesitan F20-H02 y F19.
2. US1, el reporte en pantalla.
3. US2, las descargas y la bitácora, con el paso R5 que prueba que el contenido publicado no cambia.
4. El cierre: el build, las mediciones, el recorrido, la evidencia y la fusión.

Una sola rama (`058-reporte-individual-docente`) y un solo PR, como en las historias anteriores. El PR principal lleva `Refs #268`, y el de evidencia, `Closes #268`.
