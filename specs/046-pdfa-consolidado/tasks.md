---

description: "Tareas de F13-H02: generar el PDF/A del consolidado con su hash"
---

# Tasks: Generar el PDF/A del consolidado con su hash

**Input**: documentos de diseño en `specs/046-pdfa-consolidado/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las cuatro aclaraciones del autor del 2026-11-11, en la spec;
- D-101, que se escribe en T001;
- ADR-006, ADR-018, D-46, D-83, D-88, D-95 y D-100.

**Organization**:
- **Fundacional:** D-101, los contratos, los errores, las migraciones y las APIs publicadas que Firma consume.
- **US1:** se genera al congelar (MVP, con US2 y US3: las tres forman el documento).
- **US2:** el bloque de firmas.
- **US3:** el archivo no cambia.
- **US4:** si falla, se reintenta.
- **US5:** la pantalla lo muestra.
- **Cierre:** el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US5.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>`, e `integrationTest` donde aplica:
  - `FIR` = `backend/servicios/firma/src`;
  - `VAL` = `backend/servicios/validacionocs/src`;
  - `GEN` = `backend/servicios/generacion/src`;
  - `ROL` = `backend/servicios/roles/src`;
  - `USU` = `backend/servicios/usuarios/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §6 y §7): «Documento firmable», «Hash», «Versión congelada» y «Árbol de firmas». Anotar aquí el resultado.

  Escribir **D-101** en `docs/decisiones.md`, en la fila del índice y en su sección. Su contenido:
  - el documento firmable se genera al entrar en CONGELADO_EN_FIRMA: Validación OCS publica su evento interno `DistributivoEnFirma` en la transacción del paso y, después de confirmar, le entrega la cadena a Firma por `PrepararDocumentoFirmable` (MD-05: Firma no conoce el flujo; research §4);
  - un fallo se registra y no se relanza; el reintento es manual e idempotente;
  - la conformidad PDF/A se verifica en las pruebas y en el script, no en el proceso (research §3, D-46);
  - Firma tiene su propio almacén en el volumen de D-88, nombrado por el SHA-256 y sin sobrescribir (research §7);
  - los firmantes: el árbol de la copia de la configuración, los cargos vigentes para el Rector y el Vicerrector, y la designación del periodo para el Responsable (research §6).

  Relacionadas: RF-38, RF-58, RNF-POR-01, MD-05, ADR-006, ADR-018, D-46, D-83, D-88, D-100.

  **Resultado (2026-11-11):** «Documento firmable», «Hash» (el original identifica la versión congelada; el vigente, el último archivo firmado), «Árbol de firmas» y «Paso de firma» están en el glosario (§7) y se usan así. **MD-05** dice que Aprobación le entrega la cadena a Firma y Firma informa por eventos, para que Firma no conozca el flujo y no haya ciclo. El plan tenía a Firma escuchando y consultando a Validación OCS; se corrigió antes de implementar (research §4, data-model, contratos y T007, T009, T012, T013, T019).
- [X] T002 [P] En `backend/servicios/firma/build.gradle`, pasar OpenHTMLtoPDF y PDFBox a `implementation`; veraPDF (con JAXB 2.3) y Bouncy Castle siguen solo en pruebas. Copiar `DejaVuSans.ttf` y su licencia a `FIR/main/resources/fuentes/`. Confirmar que el `runtimeClasspath` de `apps:distributivo` no trae JAXB 2.3 (`./gradlew :apps:distributivo:dependencies`).

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T008.

- [X] T003 Escribir `contratos/firma/openapi.yaml` según `contracts/firma.md`:
  - `consultarDocumentoFirmable` y `generarDocumentoFirmable`, con su `x-mdd-permiso`;
  - `EstadoDelDocumento` con el estado, la versión, el motivo, el documento y las acciones.

  Generar los modelos en el `build.gradle` de Firma como en los demás servicios (`mdd.contrato`). Agregar el script de `generar:api` de la SPA para `firma.d.ts`. Compilar `:servicios:firma`.
- [X] T004 [P] Agregar a `contratos/roles/openapi.yaml` la ruta `/interno/roles/periodos/{periodo}/responsable` (`contracts/identidad.md`), y `firma` a la enumeración `servicio` de `contratos/usuarios/openapi.yaml`. Después de T003 y T004, correr `./gradlew :servicios:usuarios:test --rerun`.

  En la SPA, `NOMBRE_DEL_SERVICIO` (`SPA/pantallas/bitacora/nombres.ts`) es un `Record` de la enumeración: agregar `firma: 'Firma'`, porque si no, los tipos no compilan. Su prueba: el filtro por servicio de la bitácora ofrece «Firma».
- [X] T005 [P] Agregar a `contratos/comun/errores.md`, con sus 8 columnas:
  - `FIRMA_ETAPA_NO_ADMITE_GENERACION` (409);
  - `FIRMA_GENERACION_FALLIDA` (409, el motivo en detalles);
  - `ROLES_SIN_RESPONSABLE` (404).

  Crear las excepciones y `ErroresFirma`. `SinRolDeFirma` (sin el rol de la etapa que llegó en la solicitud) la traduce el controlador a 403 con el cuerpo común, como `RolDeOtraEtapa` en Validación OCS. Primero `ErroresFirmaTest` (como `ErroresGeneracionTest`); la de Roles, en `ErroresRoles`.
- [X] T006 [P] Migraciones, con pruebas de integración primero:
  - `FIR/main/resources/db/migration/firma/V2__documento_firmable.sql` según `data-model.md`, con los `check`, la clave única y los privilegios: solo `insert` y `select` sobre `documento_firmable` para el rol del proceso;
  - en Usuarios, la migración siguiente que agrega `firma` al `check` de `bitacora.servicio`.

  Ampliar `FirmaEsquemaIntegrationTest`: las tablas, los `check` y que el rol del proceso no puede actualizar ni borrar un documento.

  Las acciones que ofrece el filtro de la bitácora salen de las entradas guardadas (`SELECT DISTINCT servicio, accion`): `DOCUMENTO_FIRMABLE_GENERADO` aparece sola cuando llega la primera, sin registrarla en ninguna lista.

  **Resultado (2026-11-11):** además del `check`, Usuarios tenía la lista de servicios en tres lugares del código (`EntradaBitacora`, `FiltroDeBitacora` y `RegistrarEventoRecibidoServicio`), con sus cuatro pruebas parametrizadas: se agregó `firma` en todos (migración `V6__bitacora_firma.sql`). La migración de Firma la prueba `DocumentoFirmableEsquemaIntegrationTest`, con un rol del proceso que no es superusuario.
- [X] T007 [P] La cadena de firmas en Validación OCS (`contracts/validacionocs.md`), viendo fallar primero su prueba: `EtapasAlCrearse` da también los pasos del documento CONSOLIDADO de la misma carga, en su orden (por defecto: Elaborado/RESPONSABLE_CONSOLIDACION, Revisado/VICERRECTOR, Aprobado/RECTOR), la que regía al crear el distributivo. Y la API publicada de Firma, `PrepararDocumentoFirmable` con `SolicitudDeDocumento` y `PasoDeLaCadena`, en su API publicada (`puertos.entrada`, interfaz nombrada `api`) (esqueleto; la implementación es de T012).
- [X] T008 API publicada de Generación, `ContenidoCongelado.contenido(periodo)` (`contracts/generacion.md`). Ver fallar primero `ContenidoCongeladoServicioTest`:
  - exige la versión CONGELADA (`VersionNoCongelada`);
  - la versión y el hash son los de `hashDelDistributivo`;
  - las carreras y los totales son los del consolidado;
  - cada docente tiene sus asignaturas, actividades y horas por componente, como la carga horaria, ordenados por nombre;
  - sin distributivo, `DistributivoNoEncontrado`.

  Después, el servicio y su registro en `ConfiguracionGeneracion`.

  **Resultado (2026-11-11):** Generación no conoce los nombres de las carreras (la oferta trae solo el código; en pantalla los pone Académico). Se agregó la ruta interna `/interno/academico/carreras` → `[{codigo, nombre}]` (contrato, `InternoAcademicoContratoTest` e `InternoAcademicoControlador`) y el puerto `DatosParaElDocumento` en Generación, con su adaptador y su prueba. La prueba y la ruta de Académico se escribieron juntas, sin verla fallar antes.

**Checkpoint**: los contratos compilan, los errores están en el catálogo, las tablas existen y Firma tiene de dónde leer.

---

## Phase 3: User Story 1 - El documento firmable se genera al congelar (Priority: P1) 🎯 MVP

**Goal**: al entrar en CONGELADO_EN_FIRMA se genera solo un PDF/A-2b con el resumen, los totales, el detalle por docente y el pie con la versión y el hash.

**Independent Test**: `probar_documento_firmable.sh`, pasos 1, 2 y 4.

- [X] T009 [P] [US1] En Validación OCS, ver fallar primero en `FlujoDelDistributivoServicioTest`:
  - `DistributivoEnFirma(periodo, version, hash, momento)` se publica una vez al aprobar, al enviar a revisión sin la revisión, con el último parcial y con el avance genérico que llegan a CONGELADO_EN_FIRMA;
  - no se publica en un rechazo ni en un paso a otra etapa.

  - el evento lleva la cadena de la copia y el rol de la etapa CONGELADO_EN_FIRMA.

  Después: el registro interno en `VAL/main/.../aplicacion/`, el puerto de salida `AvisoDeFirma` con su adaptador sobre `ApplicationEventPublisher`, llamado dentro de la transacción del paso, y el oyente `PreparacionDelDocumento` (`@ApplicationModuleListener`) que llama a `PrepararDocumentoFirmable.preparar`, con su prueba.
- [X] T010 [P] [US1] En el dominio de Firma, ver fallar primero:
  - `GeneracionDeDocumentoTest`: los estados de `data-model.md`; GENERADA no cambia; FALLIDA vuelve a EN_CURSO; el motivo de hasta 200 caracteres;
  - `ContenidoDelDocumentoTest`: arma los firmantes desde el árbol (rótulo = etiqueta + « por», el cargo por rol y el nombre del campo `firma-<orden>-<rol>`); falla con `FirmanteSinNombre` si falta uno.

  Después, el dominio.
- [X] T011 [US1] El generador, `FIR/main/.../adaptadores/salida/GeneradorPdfa.java`, con el camino de `PdfaDePruebaTest`. Ver fallar primero `GeneradorPdfaTest`, sin Docker y con veraPDF:
  - un contenido pequeño y uno de 165 paralelos (datos sintéticos) dan PDF/A-2b conforme;
  - cada página tiene en el pie «<periodo> · versión N · hash <64 hex>», con el texto extraído con PDFBox;
  - están el resumen por carrera, los totales y cada docente con sus asignaturas y actividades;
  - un texto con `<`, `&` o comillas sale escapado;
  - el de 165 paralelos tarda menos de 30 s (SC-003).

  El XHTML en Java, con los textos escapados; las tablas con `page-break-inside: avoid` por fila.
- [X] T012 [US1] En la aplicación de Firma, ver fallar primero `DocumentoFirmableServicioTest` (Mockito):
  - **`preparar(solicitud)`:** registra la solicitud (cadena y rol de la etapa) y marca EN_CURSO; pide el contenido, el árbol y los firmantes fuera de la transacción; genera; y en la transacción final, con la generación bloqueada, guarda el archivo, registra el documento a nombre de `sistema` y una entrada `DOCUMENTO_FIRMABLE_GENERADO`, y marca GENERADA;
  - si la versión o el hash del contenido no son los de la solicitud, falla con ese motivo;
  - con la generación ya GENERADA, no hace nada.

  Después: los puertos de `data-model.md`, el servicio y `ConfiguracionFirma`. `ContenidoDesdeGeneracion` es el adaptador sobre la API publicada de Generación.
- [X] T013 [US1] En `backend/apps/distributivo/src/integrationTest/java/ec/mdd/DocumentoFirmableIntegrationTest.java`, como `BitacoraDesdeGeneracionIntegrationTest` (PostgreSQL real con Testcontainers, el contexto del proceso, los procesos remotos con dobles): aprobar deja, después de confirmar, un documento GENERADO de la versión congelada; la aprobación responde antes de que termine la generación. Verificar en `ModulosTest` que Validación OCS depende de Firma solo por su API publicada, que Firma depende de Generación solo por su interfaz nombrada, y que Firma no depende de Validación OCS (MD-05).

  **Resultado (2026-11-11):** un recorrido completo de aprobación necesita los procesos datos e identidad, así que la prueba publica `DistributivoEnFirma` en una transacción del proceso real (PostgreSQL con Testcontainers), con dobles solo para el contenido y los firmantes: el generador, el almacén, el repositorio y el registro de eventos son los reales. Comprueba el documento GENERADO, su archivo, su entrada pendiente para la bitácora, que la transacción no espera a la generación (los firmantes tardan 1 s) y que un paso revertido no genera nada. `ModulosTest` encontró que `ContenidoDeLaVersion` exponía `CargaHoraria`, del dominio de Generación: ahora lleva su propio registro `Horas`. Se escribió después del código.

**Checkpoint**: aprobar genera el documento.

---

## Phase 4: User Story 2 - El bloque de firmas (Priority: P1)

**Goal**: la última página tiene «Elaborado por», «Revisado por» y «Aprobado por», con el cargo, el nombre y un campo de firma vacío con nombre.

**Independent Test**: `GeneradorPdfaTest` y `probar_documento_firmable.sh`, paso 4.

- [X] T014 [US2] Ver fallar primero en `GeneradorPdfaTest`:
  - la última página tiene los tres rótulos en orden, cada uno con su cargo y su nombre;
  - el `AcroForm` tiene tres `PDSignatureField` vacíos, `firma-1-RESPONSABLE_CONSOLIDACION`, `firma-2-VICERRECTOR` y `firma-3-RECTOR`, con su widget dentro de la última página, el indicador *Print* y una apariencia normal;
  - veraPDF sigue dando PDF/A-2b conforme.

  Después, el bloque en la última página con posiciones fijas y los campos con PDFBox (research §2).

  **Resultado (2026-11-11):** las 10 pruebas de `GeneradorPdfaTest` pasan; veraPDF da PDF/A-2b conforme con los tres campos. El contenido de 165 paralelos se genera y verifica en 1,6 s. La prueba deja en `backend/servicios/firma/build/documento-firmable/` el PDF y las imágenes de la primera y la última página, con los campos marcados en rojo: así se vio que el margen por defecto del cuerpo desplazaba los recuadros 2,8 mm y que el borde sumaba a la altura; se corrigió (`body { margin: 0 }`, `box-sizing: border-box`).
- [X] T015 [P] [US2] Los firmantes del periodo:
  - en Roles, ver fallar primero la prueba del servicio y la de contrato de `/interno/roles/periodos/{periodo}/responsable`: 200 con el usuario y su nombre para mostrar (sin nombre, el usuario); 404 `ROLES_SIN_RESPONSABLE`; solo con token de proceso. Después, la ruta, el servicio y `NombreDeUsuario` en Usuarios (API publicada), verificado por el `ModulosTest` de `identidad`;
  - en Firma, ver fallar primero `FirmantesDesdeProcesosTest` (con el servidor de pruebas de los adaptadores entre procesos): el Rector y el Vicerrector salen de `cargos-vigentes` más `activos`, y el Responsable de la ruta nueva; si falta alguno, `FirmanteSinNombre` con el cargo. Después, el adaptador.

**Checkpoint**: el documento tiene sus firmantes y sus campos.

---

## Phase 5: User Story 3 - El archivo no cambia (Priority: P1)

**Goal**: el PDF fuera de la base, nombrado por su SHA-256, sin sobrescribir; el registro con el SHA-256 original y una entrada en la bitácora.

**Independent Test**: `probar_documento_firmable.sh`, pasos 3, 5 y 6.

- [X] T016 [P] [US3] `AlmacenDeDocumentosEnDisco`, viendo fallar primero su prueba con una carpeta temporal:
  - guarda `documentos/<sha256>.pdf` y devuelve ese nombre;
  - un segundo guardado del mismo contenido no escribe;
  - nunca abre un archivo existente para escribir (`CREATE_NEW`);
  - sin permiso de escritura, una excepción que la aplicación convierte en fallo.

  Montar la carpeta en `apps:distributivo` (`mdd.firma.archivos`, por defecto `/var/mdd/archivos/documentos`).
- [X] T017 [P] [US3] `RepositorioDocumentosJdbc` con integración (PostgreSQL real):
  - bloquear o crear la generación (`FOR UPDATE`);
  - registrar el documento y leerlo;
  - dos generaciones a la vez: la segunda espera, ve GENERADA y no registra; queda un documento (SC-005).
- [X] T018 [US3] La bitácora de Firma: `PublicadorDeCambios` con el evento `EntradaBitacora` del servicio `firma` (ADR-018), como el de Validación OCS. Ver fallar primero en `DocumentoFirmableServicioTest` que hay una sola entrada con el periodo, la versión, el hash de contenido y el SHA-256, y ninguna si ya estaba GENERADA.

**Checkpoint**: el documento queda registrado e inmutable.

---

## Phase 6: User Story 4 - Si falla, se reintenta (Priority: P2)

**Goal**: un fallo deja la etapa como está, con el motivo; el Responsable lo reintenta.

**Independent Test**: `DocumentoFirmableServicioTest`, `DocumentoContratoTest` y el Playwright con el estado FALLIDA.

- [X] T019 [US4] Ver fallar primero en `DocumentoFirmableServicioTest`:
  - un `FirmanteSinNombre`, un fallo de Datos o del almacén dejan FALLIDA con el motivo, sin documento ni entrada, y `preparar` termina sin relanzar;
  - **`generar(periodo, quien)`:** con el rol de la etapa de la solicitud, genera y registra a nombre de quien lo pidió; si falla, `GeneracionFallida` con el motivo; con el documento ya GENERADO, lo devuelve sin hacer nada; sin solicitud de Aprobación, `EtapaNoAdmiteGeneracion`; sin el rol, `SinRolDeFirma` (403);
  - **`estado(periodo, quien)`:** SIN_GENERAR (sin versión si no hay solicitud), EN_CURSO, FALLIDA con motivo o GENERADO con el documento; `GENERAR` solo para el rol y con FALLIDA.
- [X] T020 [US4] `DocumentoControlador` y su contrato. Ver fallar primero:
  - en `DocumentoContratoTest`: 200 en los cuatro estados según el esquema; 409 sin solicitud y de generación fallida con los detalles; 403 sin el rol; 400 con el periodo mal formado;
  - en `PermisosFirmaTest`: las dos operaciones con su permiso.

**Checkpoint**: el fallo se ve y se corrige.

---

## Phase 7: User Story 5 - La pantalla lo muestra (Priority: P3)

**Goal**: la tarjeta del flujo dice el estado del documento y ofrece «Generar el PDF» al Responsable.

**Independent Test**: Vitest y Playwright con axe-core, según `contracts/spa.md`.

- [X] T021 [US5] Escribir primero `SPA/pantallas/comun/DocumentoFirmable.test.tsx`:
  - GENERADO: «Documento firmable generado el <fecha>» y la huella abreviada con «Copiar la huella»;
  - EN_CURSO: «Generando el documento firmable…» y vuelve a consultar;
  - FALLIDA: «El PDF no se pudo generar» con el motivo;
  - con `GENERAR`, el botón llama una vez y se desactiva; un 409 muestra el mensaje y los detalles;
  - sin la acción, no hay botón.

  Después: `comun/documento.ts`, `DocumentoFirmable.tsx` (reusa el botón de copiar de la constancia) y su lugar en `EstadoDelFlujo.tsx`, solo en CONGELADO_EN_FIRMA.
- [X] T022 [US5] En `frontend/e2e/consolidado.spec.ts`, con la API simulada:
  - el Vicerrector aprueba y la tarjeta pasa de «Generando…» a «Documento firmable generado»;
  - el Responsable ve FALLIDA y «Generar el PDF», lo pulsa y queda GENERADO;
  - axe-core sin fallas en los tres estados.

**Checkpoint**: la pantalla refleja el documento.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T023 Crear `deploy/probar_documento_firmable.sh` con los 6 pasos de `contracts/demostracion.md`, a partir de `probar_aprobacion.sh`:
  - veraPDF en `verapdf/cli:v1.30.2` sobre el archivo copiado del volumen;
  - `MDD_CONSERVAR=1` con su guarda;
  - limpieza de `documentos/`;
  - sin claves ni tokens; shellcheck.

  Agregar su paso «Documento firmable» a `perfil-consolidado` después de «Aprobar y congelar». Cambiar `rm -f /var/mdd/archivos/*` por `rm -rf` en los scripts que lo usan.

  Desde esta historia, aprobar genera el documento, así que todo script que llega a CONGELADO_EN_FIRMA deja filas en `firma`. En `probar_aprobacion.sh` y en el nuevo:
  - la limpieza borra `firma.documento_firmable` y `firma.generacion_documento` de los periodos de prueba (con el superusuario: el rol del proceso no borra documentos) y vacía `documentos/`;
  - la revisión de «base sin datos» del paso 0 cuenta también esas tablas.

  Si no, el script siguiente encontraría ya GENERADA la versión 1 de 2026-I y recibiría el documento viejo.

  **Resultado (2026-11-11):** los 6 pasos pasan contra el perfil local (el documento, unos 1 a 3 s después de aprobar), y `probar_aprobacion.sh` sigue en verde corrido antes. Tres hallazgos al correrlo:
  - **El nombre del Responsable sale de la designación del periodo** (research §5), y los scripts anteriores no designaban a nadie: sin designación, la generación queda FALLIDA con «Falta el nombre del Responsable de consolidación.» (FR-008, como se diseñó). El script designa a `docente.demo`, como `probar_flujo.sh` (solo se designa a un DOCENTE), y borra la designación al terminar. En la demostración, el Vicerrector tiene que designar al Responsable del periodo antes de aprobar.
  - **La limpieza no podía borrar `firma.generacion_documento`**, aunque ya no quedaran documentos: la verificación de la clave foránea de `documento_firmable` toma `FOR KEY SHARE` como dueño de esa tabla, y V2 le quitó UPDATE. La limpieza (superusuario) la borra con `session_replication_role = replica`, después de los documentos. El proceso nunca borra generaciones.
  - **La imagen de veraPDF corre con el usuario 100**, que no lee la carpeta temporal (700, con respuestas que llevan tokens): se monta solo una subcarpeta con el PDF, de solo lectura.
- [X] T024 Correr todos los scripts del workflow, en su orden, sobre un perfil construido desde la rama, y anotar el tiempo de generación (SC-003). Recorrer `quickstart.md` §3 a mano: el PDF abierto, el pie, el detalle y el bloque de firmas; y el camino de fallo: sin Responsable designado (borrado en la base local, como dice el quickstart), el aviso «El PDF no se pudo generar»; designarlo otra vez, «Generar el PDF» y medir el tiempo (SC-006).

  **Resultado de los scripts (2026-11-12):** los 35 del workflow pasan, en su orden, sobre un perfil construido desde la rama y con la base vacía; `probar_documento_firmable.sh`, 6/6 en 44 s, con el documento generado 1 s después de aprobar (SC-003).

  **Recorrido a mano del autor (2026-11-14), en el perfil local:**
  - Camino normal: al aprobar, la tarjeta pasó a «Documento firmable generado» en el mismo minuto (03:43). El PDF, de 11 páginas, tiene el SHA-256 de la tarjeta (`8b4e6d5c59d6…`); el autor revisó el resumen, el detalle, el pie y las firmas, y los dio por buenos.
  - Camino de fallo (SC-006): sin designación, al aprobar la tarjeta dijo «El PDF no se pudo generar» con «Falta el nombre del Responsable de consolidación.», y la versión 1 quedó congelada igual. El Vicerrector designó otra vez a `docente.demo`. `consolidacion.demo` pulsó «Generar el PDF» y lo tuvo en **40 s**, medidos desde que abrió la pantalla. En la base quedó un solo documento, generado por `consolidacion.demo`, y en la bitácora `CONTENIDO_APROBADO` (03:50:42) y una sola `DOCUMENTO_FIRMABLE_GENERADO` (03:53:47, usuario `consolidacion.demo`).
- [X] T025 Correr completos `./gradlew build integrationTest`, `:apps:distributivo:test` y `:apps:identidad:test` (Spring Modulith), la cobertura por módulo (D-32), Spotless y SpotBugs, y en la SPA los tipos, ESLint, Prettier, Vitest y Playwright (SC-007).

  **Resultado (2026-11-12):**
  - `./gradlew build integrationTest jacocoTestCoverageVerification --rerun-tasks`: 3 002 pruebas unitarias y 290 de integración, con 2 fallas en módulos que esta rama no toca, que pasan solas: `ClienteDeEventosLocalTest` de `plataforma:eventos` (3 de 3) y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` de `apps:datos` (9/9), con todos los módulos corriendo a la vez. Firma 65 y 8; Validación OCS 349 y 24; Generación 811 y 53; `apps:distributivo` 6 de integración. Modulith (`apps:*:test`) en verde.
  - Cobertura de líneas (D-32, mínimo 60 %): Firma 86,5 %, Validación OCS 83,8 %, Generación 90,7 %, Roles 77,2 %, Usuarios 84,3 %, Académico 84,7 %. El XML de `jacocoTestReport` no lo regenera `build`: hay que pedirlo.
  - Spotless y SpotBugs en verde.
  - SPA: tipos, ESLint y Prettier en verde; Vitest 562/562 en 41 archivos (con el build de Gradle corriendo a la vez fallaron 3 por tiempo; solas pasan); Playwright 123/123.
- [X] T026 Revisar el JavaDoc (principio XII): el evento interno y su oyente, `PrepararDocumentoFirmable` (por qué Aprobación entrega la cadena, MD-05), `ContenidoCongelado`, `NombreDeUsuario`, los puertos y el dominio de Firma, el generador (por qué el bloque en la última página), el almacén (por qué `CREATE_NEW`) y el oyente (por qué no relanza).
- [X] T027 Crear `specs/046-pdfa-consolidado/evidencia.md`, como la de 045, con:
  - las pruebas, la cobertura, la arquitectura, la SPA, los scripts, shellcheck y Gitleaks;
  - la tabla del script y el resultado de veraPDF;
  - la comprobación a mano;
  - las desviaciones y los criterios de terminado.
- [X] T028 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración (`quickstart.md` §4) y abrir el PR de evidencia con `Closes #61`. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup (T001, T002)** antes que todo.
- **Fundacional (T003 a T008):** T003 antes de T020; T005 y T006 antes de T012; T007 y T008 antes de T012.
- **US1:** T009 y T010 en paralelo; T011 después de T010; T012 después de T009 a T011; T013 al final.
- **US2:** T014 después de T011; T015 en paralelo con T014.
- **US3:** T016 y T017 en paralelo; T018 después de T012.
- **US4:** T019 después de T012 y T015; T020 después de T019.
- **US5:** T021 puede empezar con la API simulada después de T003; T022 después de T021.
- **Cierre:** T023 y T024 dependen de US1 a US4; T025 a T028, al final.

### Parallel Opportunities

```text
T002, T004, T005, T006, T007 en paralelo con T003.
T009 y T010 (servicios distintos).
T015 (Roles y Usuarios) en paralelo con T014 (generador).
T016 y T017 (almacén y repositorio).
T021 (SPA) con la API simulada en cuanto exista T003.
```

## Implementation Strategy

- **MVP:** Setup, Fundacional, US1, US2 y US3 (T001 a T018): el documento completo se genera solo.
- **Después:** US4 (el fallo y el reintento) y US5 (la pantalla).
- **Luego:** el script, las regresiones y el cierre.
