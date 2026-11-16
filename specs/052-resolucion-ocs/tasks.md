---
description: "Tareas de F15-H01: registrar la resolución del OCS"
---

# Tasks: Registrar la resolución del OCS

**Input**: documentos de diseño en `specs/052-resolucion-ocs/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero un esqueleto, y la implementación va en un commit aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-15, en la spec;
- D-107, escrita con el plan;
- D-16, D-19, D-88, D-95, D-104 y ADR-007.

**Organization**:
- **Fundacional:** el contrato, el catálogo, la migración y el dominio.
- **US1:** la Secretaría registra la resolución y el distributivo queda aprobado (MVP).
- **US2:** ver la resolución y descargar su PDF.
- **US3:** registrar desde «Consolidado del periodo».
- **Cierre:** el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. `OCS` = `backend/servicios/validacionocs/src`, con `main`, `test` e `integrationTest` en `java/ec/mdd/validacionocs`; `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Escribir **D-107** en `docs/decisiones.md`, en la fila del índice y en su sección (research §1 a §10). Revisar en el glosario `ResolucionOcs`, `registrarResolucion()`, `ResolucionRegistrada`, «Resolución del OCS» y SECRETARIA_GENERAL, y anotar aquí el resultado.

  **Resultado (2026-11-15):** escrita con el plan. El glosario tiene `ResolucionOcs` («Entidad de `ProcesoAprobacion`… Número, fecha y PDF. El MDD no valida sus firmas»), la operación `registrarResolucion()`, el evento `ResolucionRegistrada` («Secretaría registra la resolución → Pasa a APROBADO»), «Resolución del OCS» y el rol `SECRETARIA_GENERAL`. Se usan esos nombres. El evento no se publica todavía (research §8); la bitácora usa `RESOLUCION_REGISTRADA`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T006.

- [X] T002 [P] El contrato `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`:
  - `registrarResolucion`: `POST /api/validacionocs/periodos/{periodo}/resolucion`, `multipart/form-data` con «`numero` `maxLength: 60`», «`fecha` `format: date`» y «`archivo` `format: binary`», `x-mdd-permiso: RESOLUCION_OCS:ESCRITURA`, 200 `EstadoDelFlujo` y sus 400, 413, 403, 404 y 409;
  - `descargarResolucion`: `GET …/resolucion/archivo`, `application/pdf`, con `x-mdd-permiso: RESOLUCION_OCS:LECTURA`, y su 404 y 500;
  - `ResolucionDelOcs` y `EstadoDelFlujo.resolucion`; `REGISTRAR_RESOLUCION` en `AccionDelProceso`; el 409 `VALIDACIONOCS_REQUIERE_RESOLUCION` del avance.

  Regenerar los tipos de la SPA. Pasan `verificar:ruptura` y `PermisosValidacionocsTest` (con las dos operaciones nuevas: puede hacer falta un esqueleto del controlador).

  **Resultado (2026-11-15):** el contrato con las dos operaciones, `ResolucionDelOcs`, `EstadoDelFlujo.resolucion`, la acción y el 409 del avance. Los tipos de la SPA, regenerados; `verificar:ruptura` pasa. `FlujoControlador` tiene un esqueleto de las dos operaciones, con sus `@PreAuthorize`, que lanza hasta T010 y T012. `PermisosValidacionocsTest` cuenta 10 operaciones y verifica los dos permisos nuevos.
- [X] T002b [P] Roles V9, `backend/servicios/roles/src/main/resources/db/migration/roles/V9__resolucion_para_quien_aprueba.sql`: «`RESOLUCION_OCS:LECTURA`, `TODO`» para VICERRECTOR, RECTOR y MIEMBRO_OCS (análisis S1). Antes, ver fallar en `backend/servicios/roles/src/integrationTest/.../MatrizD16IntegrationTest.java` los permisos de esos tres roles.

  **Resultado (2026-11-15):** `MatrizD16IntegrationTest` se vio fallar con los tres permisos nuevos y pasa con V9. Las pruebas de Roles siguen en verde.
- [X] T003 [P] Los ocho códigos en `contratos/comun/errores.md` (8 columnas, `contracts/validacionocs.md`) y en `OCS/main/.../adaptadores/entrada/ErroresValidacionocs.java`. Antes, ver fallar `ErroresValidacionocsTest`.

  **Resultado (2026-11-15):** `ErroresValidacionocsTest` se vio fallar con las ocho entradas nuevas y pasa con ellas en el código y en `errores.md`. `VALIDACIONOCS_RESOLUCION_ILEGIBLE` va con la clasificación «Sistema», como `GENERACION_INFORME_ILEGIBLE`: «Técnico», del contrato del plan, no es una de las admitidas. Las ocho excepciones se crearon aquí, vacías, para que el catálogo compile; las lanzan T005, T006, T007 y T012.
- [X] T004 [P] La migración `OCS/main/resources/db/migration/validacionocs/V7__resolucion_ocs.sql`, según `data-model.md`. Antes, ver fallar en `OCS/integrationTest/.../ValidacionocsEsquemaIntegrationTest.java`:
  - inserta una resolución con su transición;
  - una segunda de la misma versión falla;
  - el número de solo espacios, de 61 caracteres, un `sha256` mal formado y un tamaño de 0 o de más de 10 MB, fallan.

  **Resultado (2026-11-15):** la prueba nueva de `ValidacionocsEsquemaIntegrationTest` se vio fallar sin la tabla y pasa con V7.
- [X] T005 [P] El dominio de valores. Ver fallar primero:
  - `ResolucionOcsTest`: el número vacío, de espacios, de 60 y de 61, recortado;
  - `ArchivoDeResolucionTest`: vacío, sin `%PDF-`, sin `%%EOF` al final, de 10 MB justos y de 10 MB más 1 byte; la huella SHA-256; el nombre sin ruta.

  Luego, `ResolucionOcs`, `ArchivoDeResolucion` y sus excepciones (`NumeroDeResolucionInvalido`, `ResolucionNoEsPdf`, `ResolucionDemasiadoGrande`).

  **Resultado (2026-11-15):** `ResolucionOcsTest` (3) y `ArchivoDeResolucionTest` (3). Fallaron 5 contra los esqueletos; la del número de 60 pasaba porque el esqueleto no validaba nada. Pasan con la implementación. Las excepciones ya existían (T003). `ResolucionOcs` exige además la huella y un tamaño desde 1, como la tabla.
- [X] T006 [P] El agregado. Ver fallar primero en `OCS/test/.../dominio/ProcesoAprobacionTest.java`:
  - **en EN_OCS, el rol de la etapa registra:** un `PASO_DE_ETAPA` a APROBADO por quien registra, con la versión y el hash de la congelación y la resolución;
  - **la fecha:** del día de entrada en EN_OCS y de hoy (en `America/Guayaquil`, también cerca de la medianoche), y fuera de rango (`ResolucionFueraDeRango`);
  - **otro rol:** `RolDeOtraEtapa`;
  - **otra etapa:** `EtapaNoAdmiteResolucion`;
  - **el avance genérico desde EN_OCS:** `RequiereResolucion`;
  - **las acciones:** `REGISTRAR_RESOLUCION` solo para el rol en EN_OCS;
  - **`resolucion()`.**

  Además, en `TransicionTest`, la resolución solo en EN_OCS → APROBADO.

  Luego, `Transicion.resolucion`, `AccionDelProceso.REGISTRAR_RESOLUCION`, las excepciones y los métodos.

  **Resultado (2026-11-15):** 7 pruebas nuevas en `ProcesoAprobacionTest` y 3 en `TransicionTest`. Fallaron las 8 que dependen del comportamiento nuevo; la de los constructores de antes pasaba con el esqueleto. Pasan con la implementación. La prueba parametrizada de F12-H02 sobre «etapa con operación propia» excluye ahora EN_OCS, que tiene su propio 409 (`RequiereResolucion`). `ProcesoAprobacion.ZONA` es `America/Guayaquil`. Las 404 de Validación OCS en verde.

**Checkpoint**: el contrato, el catálogo, la tabla y las reglas están; ninguna operación nueva funciona todavía.

---

## Phase 3: User Story 1 - La Secretaría registra la resolución y el distributivo queda aprobado (Priority: P1) 🎯 MVP

**Goal**: con el distributivo en EN_OCS, el rol de la etapa registra el número, la fecha y el PDF, y el distributivo queda en APROBADO.

**Independent Test**: `secretaria.demo` registra con el distributivo en EN_OCS; queda APROBADO, con la resolución, el archivo en el volumen y la entrada en la bitácora; el avance genérico desde EN_OCS da 409.

- [X] T007 [US1] El almacén. Ver fallar primero en `OCS/integrationTest/.../AlmacenDeResolucionesEnDiscoIntegrationTest.java` (o una prueba unitaria con una carpeta temporal):
  - guarda en `resoluciones/<sha256>.pdf`;
  - otra vez el mismo, sin reescribir;
  - leer devuelve los bytes;
  - un archivo que falta o no coincide con su huella: `ResolucionIlegible`.

  Luego, `puertos/salida/AlmacenDeResoluciones` y `adaptadores/salida/AlmacenDeResolucionesEnDisco` (temporal y movimiento atómico), y su bean con `mdd.archivos.raiz`.

  **Resultado (2026-11-15):** `AlmacenDeResolucionesEnDiscoTest` (3, unitaria con una carpeta temporal), vista fallar contra el esqueleto. Pasa con el almacén: temporal y enlace, como `AlmacenDeDocumentosEnDisco` de Firma; la huella se comprueba al leer; una huella mal formada no sale de la carpeta. El bean usa `mdd.archivos.raiz`.
- [X] T008 [US1] El repositorio. Ver fallar primero en `OCS/integrationTest/.../RepositorioProcesoIntegrationTest.java`: la transición a APROBADO con su resolución se guarda y se lee; `resolucion()` la devuelve. Luego, `RepositorioProcesoJdbc` inserta y lee `resolucion_ocs`.

  **Resultado (2026-11-15):** la prueba nueva se vio fallar al leer: sin la resolución, la transición a APROBADO no cumple su regla. Pasa con la inserción y la lectura de `resolucion_ocs`. En las etapas de esta prueba, EN_OCS es del VICERRECTOR, así que registra con ese rol. Las de integración de Validación OCS en verde.
- [X] T009 [US1] El caso de uso. Ver fallar primero en `OCS/test/.../aplicacion/RegistrarResolucionServicioTest.java`:
  - el archivo inválido se rechaza sin guardar ni bloquear;
  - el archivo se guarda antes de la transacción;
  - con el proceso bloqueado, el agregado decide;
  - la transición con su resolución y el cambio `RESOLUCION_REGISTRADA` (versión, hash, número, fecha y huella) en la misma transacción;
  - cada rechazo del agregado, sin guardar;
  - sin distributivo: `SinDistributivo`;
  - el periodo cerrado: `PeriodoCerrado`, sin guardar el archivo (análisis U1).

  Luego, `puertos/entrada/RegistrarResolucion`, `CambioDeFlujo.Accion.RESOLUCION_REGISTRADA` con su rama en `FlujoDelDistributivoServicio.cambio`, y `RegistrarResolucionServicio` con su bean.

  **Resultado (2026-11-15):** `RegistrarResolucionServicioTest` (5), vista fallar contra el esqueleto; pasa con la implementación. Para que un rechazo no deje ni el archivo, el agregado suma `validarResolucion` (las mismas reglas, sin cambiar nada), que `registrarResolucion` reutiliza; lo cubren sus pruebas de T006. Sin proceso todavía, el distributivo no llegó a EN_OCS: `EtapaNoAdmiteResolucion`. El servicio responde el estado del flujo con `ConsultarFlujo`. La bitácora lleva el número, la fecha (texto ISO) y la huella.
- [X] T010 [US1] El controlador. Ver fallar primero en `OCS/test/.../adaptadores/entrada/RegistrarResolucionContratoTest.java`:
  - el 200 con el flujo en APROBADO;
  - cada 400, el 413 y el 403 (sin permiso y sin rol);
  - el 409 de la etapa;
  - el 409 del periodo cerrado;
  - el 409 `VALIDACIONOCS_REQUIERE_RESOLUCION` del avance desde EN_OCS.

  Todos validados contra el contrato. Luego, el endpoint multipart, en `FlujoControlador`, junto a aprobar y avanzar (análisis A1). `PermisosValidacionocsTest` en verde.

  **Resultado (2026-11-15):** `RegistrarResolucionContratoTest` (4), con el caso de uso simulado. Contra el esqueleto fallaron el 200 y los siete rechazos del caso de uso; el 400 común (una parte que falta, una fecha mal formada) y el 403 sin permiso ya pasaban, porque los dan la validación del contrato y `@PreAuthorize`. Pasan con el endpoint.
  - **El 409 del avance desde EN_OCS** va en `FlujoContratoTest`, con el dominio real (`FlujoEnMemoria.enOcs`). Pasó al escribirse, porque el bloqueo estaba en el dominio desde T006.
  - **`FlujoEnMemoria`** suma un `RegistrarResolucion` real sobre la memoria, que usará T012.
  - **`PermisosValidacionocsTest`** suma un doble de `RegistrarResolucion`.

  Las de Validación OCS en verde.
- [X] T011 [US1] Dos registros a la vez. Ver fallar primero en `OCS/integrationTest/...` (como la concurrencia de F12-H02): dos hilos registran sobre el mismo proceso en EN_OCS; queda una resolución y una transición, y el otro recibe `EtapaNoAdmiteResolucion`.

  **Resultado (2026-11-15):** la prueba nueva va en `RepositorioProcesoIntegrationTest`: dos hilos bloquean el proceso, esperan un momento y registran. Pasó al escribirse, porque el bloqueo (`FOR UPDATE`, F12-H02) y la regla de la etapa ya estaban: no se vio fallar.

**Checkpoint**: la resolución se registra por la API y el distributivo queda en APROBADO.

---

## Phase 4: User Story 2 - Ver la resolución y descargar su PDF (Priority: P2)

**Goal**: el estado del flujo trae la resolución; quien lo lee descarga el PDF idéntico.

**Independent Test**: con APROBADO, el flujo trae número, fecha, quién, cuándo y huella; la descarga devuelve los mismos bytes; sin resolución, 404.

- [X] T012 [US2] La vista y la descarga. Ver fallar primero en `OCS/test/.../adaptadores/entrada/FlujoContratoTest.java` (con `FlujoEnMemoria`):
  - `resolucion` en el estado del flujo para quien tiene `RESOLUCION_OCS:LECTURA`, y sin ella para quien solo lee el flujo (un coordinador);
  - `REGISTRAR_RESOLUCION` para el rol en EN_OCS;
  - la descarga: 200 `application/pdf` con su nombre, el 404 sin resolución, el 500 si el archivo falta y el 403 sin `RESOLUCION_OCS:LECTURA`.

  Luego, `FlujoDelDistributivo.Resolucion`, la vista en `FlujoDelDistributivoServicio`, `DescargarResolucion` y su servicio, y el endpoint de descarga.

  **Resultado (2026-11-15):** cuatro pruebas nuevas en `FlujoContratoTest`. Registran por el endpoint real, con el caso de uso real sobre `FlujoEnMemoria`.
  - Se vieron fallar las dos que dependen de la vista y la descarga. La de «quien no puede leerla» y la de la acción en EN_OCS pasaban, por el `@PreAuthorize` y por T006.
  - La vista trae siempre la resolución, y el controlador la quita a quien no tiene `RESOLUCION_OCS:LECTURA`: el permiso es de la entrada, no del dominio.
  - La descarga responde con `Content-Disposition` en UTF-8, con el nombre guardado.
  - La copia de la matriz D-16 de la prueba suma los permisos de Roles V9.

  Las de Validación OCS (unitarias y de integración) y `ModulosTest` en verde.

**Checkpoint**: la resolución se consulta y se descarga.

---

## Phase 5: User Story 3 - Registrar desde «Consolidado del periodo» (Priority: P2)

**Goal**: el formulario para el rol de la etapa y la constancia con la descarga.

**Independent Test**: en EN_OCS, `secretaria.demo` completa y confirma; ve APROBADO y la constancia, y descarga el PDF.

- [X] T013 [P] [US3] La consulta y la descarga en `SPA/pantallas/comun/flujo.ts`: `registrarResolucion(periodo, numero, fecha, archivo)` (multipart) y `descargarResolucion(periodo)`, como la descarga del PDF de «Pendientes de firma».

  **Resultado (2026-11-15):** como `adjuntarInforme` y `descargarInforme` de «Consolidado del periodo». El archivo va con su nombre explícito en el `FormData`.
- [X] T014 [US3] El formulario y la constancia. Ver fallar primero en `SPA/pantallas/comun/EstadoDelFlujo.test.tsx`:
  - con `REGISTRAR_RESOLUCION`: los tres campos con su etiqueta;
  - la confirmación «El distributivo quedará aprobado con esta resolución. Después no se corrige.»;
  - la solicitud multipart con los tres datos;
  - un 400 y un 413 mostrados en el formulario;
  - sin la acción, sin formulario;
  - con `resolucion`: la constancia «Resolución del OCS n.º …», la fecha, quién, cuándo, la huella copiable y «Descargar la resolución», que pide el archivo.

  Luego, el formulario y la constancia en `EstadoDelFlujo.tsx` (o en un componente aparte en `comun/`).

  **Resultado (2026-11-15):** cuatro pruebas nuevas en `EstadoDelFlujo.test.tsx`. Se vieron fallar tres; la de «sin la acción no hay formulario» pasaba sin el componente. Pasan con `comun/ResolucionDelOcs.tsx` (`RegistroDeLaResolucion` y `ConstanciaDeLaResolucion`), que `EstadoDelFlujo.tsx` muestra.
  - **Los campos** no llevan `required`, porque MUI agrega « *» a la etiqueta; el botón se habilita con los tres datos y el servidor los comprueba.
  - **El cuerpo multipart** se comprueba como texto: en jsdom, `formData()` no lee el archivo, y la API simulada pierde su nombre («blob») al copiar la petición.

  Tipos, ESLint, Prettier y Vitest 587/587.

  **La pantalla «Resolución del OCS» (decisión del autor, durante T015).** Al preparar Playwright se vio que la Secretaría general no abre «Consolidado del periodo»: la pantalla y su consulta piden `CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA`. El autor eligió una pantalla propia, `pantallas/resolucion/Resolucion.tsx` en `/resolucion-ocs`, con el permiso `RESOLUCION_DEL_OCS` (lectura o escritura de la resolución): el periodo y `EstadoDelFlujoDelPeriodo`, que ya muestra el formulario y la constancia.
  - `Resolucion.test.tsx` (2) se vio fallar sin la pantalla y pasa con ella, su ruta y su entrada del menú.
  - La spec (US3, FR-014, SC-003), el contrato de la SPA y D-107 quedan al día.
  - Vitest 589/589.
- [X] T015 [US3] Playwright en `frontend/e2e/consolidado.spec.ts`: el registro completo con el teclado, la constancia y la descarga, y axe-core sin fallas en EN_OCS y en APROBADO.

  **Resultado (2026-11-15):** en un archivo nuevo, `e2e/resolucion.spec.ts`, por la pantalla propia. `secretaria.demo` entra por el menú, registra con el teclado y confirma («Cancelar» enfocado; Tab y Enter a «Registrar y aprobar»). Después ve APROBADO con la constancia, y la descarga trae el nombre del servidor. axe-core sin fallas en EN_OCS, con la confirmación abierta y en APROBADO. La primera corrida falló por contraste en el diálogo, medido durante su animación de apertura: la prueba espera ahora a que termine. Se escribió con la pantalla hecha: no se vio fallar por falta de código. `consolidado.spec.ts`, 6/6.

**Checkpoint**: las tres historias funcionan en la API y en la SPA.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T016 El script `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - los pasos 13 a 15 después del 12;
  - `MDD_CONSERVAR=EN_OCS`, que se detiene después del paso 12;
  - la cabecera y el comentario del workflow, con F15-H01;
  - la limpieza, que borra también `validacionocs.resolucion_ocs` (o la deja ir en cascada) y `resoluciones/`.

  Correrlo sobre un perfil construido desde la rama: 16/16. shellcheck sin hallazgos.

  **Resultado (2026-11-15):** 16/16 en la primera corrida, en 90 s, sobre un perfil construido desde la rama. El PDF de prueba de la resolución es mínimo y ficticio («%PDF-1.7 … %%EOF»). El de más de 10 MB entra en el límite del proceso, de 11 MB, y lo rechaza el dominio con su 413. La fecha anterior a EN_OCS sale de la transición, en `America/Guayaquil`. shellcheck sin hallazgos. El comentario del paso en `perfil-consolidado.yml` nombra F15-H01.
- [ ] T017 Los scripts de `perfil-consolidado`, en su orden, sobre un perfil construido desde la rama (SC-004). Después, el recorrido a mano de `quickstart.md` §3, del autor (SC-003).
- [X] T018 Correr completos:
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification`;
  - en la SPA, los tipos, ESLint, Prettier, Vitest y Playwright.

  **Resultado (2026-11-15):**
  - **Backend:** `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue`, con solo las dos intermitentes de siempre (`ClienteDeEventosLocalTest`, `ConfirmarLasDiezCargasIntegrationTest`), que pasan con sus módulos completos. Con eso: 3 239 pruebas unitarias y 332 de integración, sin fallas.
  - **Cobertura de líneas:** Validación OCS 83,3 %, Roles 77,2 % (la más baja), Generación 90,1 % y Firma 80,7 %.
  - **Herramientas:** Spotless, SpotBugs, ArchUnit y Spring Modulith en verde.
  - **SPA:** tipos, ESLint, Prettier y Vitest 589/589; Playwright 130/130.
  - **T017:** los 36 scripts de `perfil-consolidado` pasan en su orden; `probar_documento_firmable.sh` en 76 s.
- [X] T019 [P] Revisar el JavaDoc:
  - registrar es aprobar;
  - la fecha en días de la institución;
  - por qué no se validan las firmas (D-19);
  - el archivo por su hash y antes de la transacción;
  - el 409 del avance desde EN_OCS.

  **Resultado (2026-11-15):** ya está, sin cambios aparte:
  - registrar es aprobar, en `ProcesoAprobacion.registrarResolucion` y `RegistrarResolucion`;
  - la fecha en días de la institución, con `ProcesoAprobacion.ZONA`;
  - D-19, en `ArchivoDeResolucion` y `RegistrarResolucionServicio`;
  - el archivo por su huella y antes de la transacción, en el servicio y en `AlmacenDeResolucionesEnDisco`;
  - el 409 del avance, con su `@throws` en `validarAvance`.
- [ ] T020 Crear `specs/052-resolucion-ocs/evidencia.md`, como la de 051.
- [ ] T021 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde. Verificar la demo (`contracts/demostracion.md`, solo lectura) y abrir el PR de evidencia con `Closes #66`. Cerrar la épica F15 (#14) si el autor lo aprueba. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001, hecha.
- **Fundacional:** T002 a T006 y T002b en paralelo.
- **US1:** T007 sin dependencias; T008 después de T004 y T006; T009 después de T005, T006, T007 y T008; T010 después de T002, T003 y T009; T011 después de T009.
- **US2:** T012 después de T002, T002b y T009.
- **US3:** T013 después de T002; T014 después de T013; T015 después de T010, T012 y T014.
- **Cierre:** T016 después de T010 y T012; T017 a T021, al final.

### Parallel Opportunities

```text
T002 a T006.
T007 (almacén) en paralelo con T008 (repositorio).
T013 y T014 (SPA) en paralelo con T011 y T012 (backend).
```

## Implementation Strategy

- **MVP:** Setup, Fundacional y US1 (T001 a T011): la resolución se registra y el distributivo queda aprobado.
- **Después:** US2, la consulta y la descarga; US3, la pantalla.
- **Luego:** el script, las regresiones y el cierre.
