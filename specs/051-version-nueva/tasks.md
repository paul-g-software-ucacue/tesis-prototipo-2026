---
description: "Tareas de F12-H04: crear una versión nueva y anular las firmas tras un rechazo en firma"
---

# Tasks: Crear una versión nueva y anular las firmas tras un rechazo en firma

**Input**: documentos de diseño en `specs/051-version-nueva/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero un esqueleto, y la implementación va en un commit aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-15, en la spec;
- D-106, escrita con el plan;
- D-83, D-95, D-99, D-100, D-101, D-105, ADR-018, MD-01, MD-04 y MD-05.

**Organization**:
- **Fundacional:** los contratos, las tres migraciones y el dominio.
- **US1:** el rechazo en firma crea la versión nueva (MVP).
- **US2:** las firmas de la versión rechazada quedan anuladas.
- **US3:** el Vicerrector sigue con la versión nueva.
- **Cierre:** el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, con `main`, `test` e `integrationTest` en `java/ec/mdd/<servicio>`:
  - `GEN` = `backend/servicios/generacion/src`;
  - `FIR` = `backend/servicios/firma/src`;
  - `OCS` = `backend/servicios/validacionocs/src`;
  - `APP` = `backend/apps/distributivo/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Escribir **D-106** en `docs/decisiones.md`, en la fila del índice y en su sección (research §1 a §9). Revisar en el glosario `VersionDistributivo`, `crearVersionNueva()`, `anularFirmas()`, `FirmasAnuladas` y «Anular firmas», y anotar aquí el resultado.

  **Resultado (2026-11-15):** escrita con el plan. El glosario tiene `crearVersionNueva()` («las ordena Aprobación», MD-04), `anularFirmas()` de `DocumentoFirmable`, el evento `FirmasAnuladas` de Firma, el estado ANULADA de la versión y «Anular firmas: … Se conservan en el historial». Se usan esos nombres. Da como motivos de la versión nueva «ajuste, devolución o reforma», sin «rechazo en firma»: D-106 lo anota para que el autor ponga al día el glosario.

  **Glosario (análisis, C1, 2026-11-15):** el autor eligió «rechazo en firma». Con su autorización, se sumó a los motivos de `VersionDistributivo` en `sdd/modelo_dominio.md`: «(ajuste, devolución, reforma o rechazo en firma)». El `.docx` lo pone al día el autor. El glosario está fuera del repositorio.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T008.

- [X] T002 [P] Los contratos, según `contracts/firma.md` y `contracts/validacionocs.md`:
  - en `contratos/firma/openapi.yaml`: `AnulacionDeFirmas` («`motivo` enum `[RECHAZO_EN_FIRMA]`, `firmasAnuladas` `minimum: 0`, `anuladaEn` `date-time`», `additionalProperties: false`) y `EstadoDelDocumento.anulacion`;
  - en `contratos/validacionocs/openapi.yaml`: `RechazoEnFirmaDelFlujo.versionAnulada` (obligatorio, `minimum: 1`) y `versionNueva` (`minimum: 2`), su descripción («presente desde el rechazo hasta la siguiente congelación») y `VERSION_NUEVA` en el tipo de transición.

  Regenerar los tipos de la SPA (`npm run generar:api`). Pasan `verificar:ruptura` y `BitacoraSinBorradoContratoTest`.

  **Resultado (2026-11-15):** los dos contratos actualizados; los tipos de la SPA regenerados; `verificar:ruptura` y `BitacoraSinBorradoContratoTest` pasan. Como `versionAnulada` es obligatorio, el DTO generado cambió su constructor: para compilar, la vista del flujo ya lleva `versionAnulada` (la versión de la transición del rechazo) y `versionNueva` vacío; el resto es de T018. `FlujoContratoTest` sigue en verde.
- [X] T003 [P] La migración `GEN/main/resources/db/migration/generacion/V8__version_nueva.sql`:
  - `version_distributivo` suma `motivo varchar(20)` («`RECHAZO_EN_FIRMA`; `check ((numero = 1) = (motivo is null))`»), `creada_por varchar(64)` y `creada_en timestamptz`;
  - el índice único de `informe_sustentado.clave` pasa a `(version_id, clave)`.

  Antes, ver fallar en `GEN/integrationTest/.../`, en la prueba de esquema de Generación:
  - una versión 2 sin motivo falla por el `check`;
  - una versión 1 con motivo, también;
  - la misma `clave` en dos versiones se admite, y dos veces en la misma, no.

  **Resultado (2026-11-15):** la prueba nueva de `GeneracionEsquemaIntegrationTest` se vio fallar sin las columnas y pasa con V8. Además del `check` entre número y motivo, la columna solo admite `RECHAZO_EN_FIRMA`. El `check` entre número y motivo se llama `version_distributivo_motivo_solo_desde_la_2`: el nombre por defecto choca con el `check` de la columna.
- [X] T004 [P] La migración `FIR/main/resources/db/migration/firma/V7__anulacion_de_firmas.sql`, según `data-model.md`: `anulacion_de_firmas` con su clave `(periodo, version, tipo)`, la foránea a `generacion_documento`, «`firmas_anuladas >= 0`», el motivo y `revoke update, delete, truncate`. Antes, ver fallar en `FIR/integrationTest/.../DocumentoFirmableEsquemaIntegrationTest.java`:
  - inserta una anulación;
  - una segunda del mismo documento falla por la clave;
  - sin generación, falla por la foránea;
  - no puede actualizar ni borrar.

  **Resultado (2026-11-15):** tres pruebas nuevas; se vieron fallar las dos que insertan una anulación válida. La de los valores inválidos pasaba sin la tabla, porque cualquier inserción fallaba: pasa con V7 por sus `check` y su foránea.
- [X] T005 [P] La migración `OCS/main/resources/db/migration/validacionocs/V6__version_nueva.sql`: `VERSION_NUEVA` en `transicion_tipo_check`. Antes, ver fallar en `OCS/integrationTest/.../ValidacionocsEsquemaIntegrationTest.java` una transición `VERSION_NUEVA`.

  **Resultado (2026-11-15):** la prueba nueva se vio fallar por el `check` y pasa con V6.
- [X] T006 [P] El tipo del motivo en la interfaz publicada de Generación: `GEN/main/.../puertos/entrada/MotivoDeVersion.java` (`RECHAZO_EN_FIRMA`), no en el dominio, porque lo usa Aprobación (principio V, análisis C2). Sin regla propia: no lleva prueba.
- [X] T007 [P] El dominio de Aprobación. Ver fallar primero en `OCS/test/.../dominio/ProcesoAprobacionTest.java`:
  - `registrarVersionNueva` después del `RECHAZO_EN_FIRMA` de esa versión: una transición `VERSION_NUEVA` por `sistema`, de EN_REVISION_VRA a EN_REVISION_VRA, con la versión nueva y su hash;
  - repetida, sin rechazo antes, o con otra versión anulada: vacía;
  - después, `rechazoPendiente()` vacío, `acciones` con APROBAR_CONTENIDO y `validarAprobacion` sin `RequiereVersionNueva`;
  - `rechazoVigente()`: el rechazo desde que se registra hasta la congelación siguiente, y vacío después;
  - un segundo rechazo, sobre la versión 2 aprobada: vuelve a EN_REVISION_VRA, `registrarVersionNueva(2, 3, …)` y `rechazoVigente()` es el segundo.

  Luego, `TipoDeTransicion.VERSION_NUEVA` y los dos métodos.

  **Resultado (2026-11-15):** 7 pruebas nuevas en `ProcesoAprobacionTest`, también la de la revisión desactivada. Fallaron 6 contra el esqueleto; la de «sin rechazo de esa versión» pasaba porque el esqueleto no hacía nada. Pasan con la implementación, y las 95 de Validación OCS siguen en verde. `registrarVersionNueva` exige que el rechazo de `anulada` sea la última transición: así la idempotencia sale de la regla de D-105.
- [X] T008 [P] El dominio de Firma. Ver fallar primero en `FIR/test/.../dominio/FirmasDelDocumentoTest.java`: con anulación, `pasoEnTurno()` vacío y nadie en turno; los constructores de antes, sin anulación. Luego, `AnulacionDeFirmas` y `FirmasDelDocumento.anulacion`.

  **Resultado (2026-11-15):** 3 pruebas nuevas en `FirmasDelDocumentoTest`. Contra el esqueleto (el record y el componente, sin la regla del turno) falló la del turno; las de los constructores de antes y de la anulación inválida pasaron con él, porque el esqueleto ya traía esas reglas. Se conservan los constructores de cuatro y cinco componentes. `AnulacionDeFirmas` guarda el motivo como texto: el enum es de la API publicada, y el dominio no depende de los puertos.

**Checkpoint**: los contratos, las migraciones y el dominio están; ninguna operación nueva funciona todavía.

---

## Phase 3: User Story 1 - El rechazo en firma crea una versión nueva (Priority: P1) 🎯 MVP

**Goal**: al rechazarse en firma, la versión congelada queda ANULADA y existe la siguiente, EDITABLE, con las mismas asignaciones; el flujo tiene `RECHAZO_EN_FIRMA` y `VERSION_NUEVA`.

**Independent Test**: con F1 y F2 aceptadas, el Rector rechaza en F3; sin nada más, la versión 1 queda ANULADA, la 2 EDITABLE con las mismas asignaciones, y el distributivo en EN_REVISION_VRA con la versión 2.

- [X] T009 [US1] El repositorio de Generación. Ver fallar primero en `GEN/integrationTest/.../RepositorioDistributivosIntegrationTest.java` (o el que prueba ese repositorio), con una versión congelada que tiene de todo:
  - `crearVersionNueva` deja la anterior ANULADA, sin cambios en su contenido;
  - crea la siguiente EDITABLE con el motivo, `sistema` y el momento;
  - compara tabla por tabla: parciales con su estado, propuestas, actividades, justificaciones, consolidaciones e informes (la misma `clave` y huella);
  - el consolidado de la nueva no queda desactualizado, y el hash de la nueva difiere del de la anulada (lleva el número).

  Luego, `RepositorioDistributivos.crearVersionNueva` y su implementación con `INSERT … SELECT` en `RepositorioDistributivosJdbc`, en la transacción que bloquea la versión (D-83): nadie edita mientras se copia, y en EN_REVISION_VRA el consolidado tampoco admite cambios (D-99). El JavaDoc dice que una tabla nueva de la versión exige su copia.

  **Resultado (2026-11-15):** `VersionNuevaIntegrationTest` (nueva, en lugar de sumarla a `RepositorioDistributivosIntegrationTest`, por su preparación propia) arma una versión congelada con dos parciales enviados, una propuesta, una actividad, una justificación, una consolidación y su informe. Se vio fallar contra el esqueleto y pasa con la copia: las seis tablas iguales en las dos versiones, la anulada intacta, el motivo, quién y cuándo de la nueva, la misma consolidación (así no queda desactualizada) y un hash distinto. Se sumó `estadoDe(periodo, numero)` al repositorio, para la idempotencia de T010.
- [X] T010 [US1] La operación publicada. Ver fallar primero en `GEN/test/.../aplicacion/ParcialesDelDistributivoServicioTest.java`:
  - la vigente congelada es la del aviso: anula, crea y devuelve el número y el hash;
  - la vigente es la siguiente, creada por ese motivo: la devuelve sin cambiar nada;
  - un segundo rechazo: con la v2 congelada, anula la v2 y crea la v3;
  - otro caso: vacío; sin distributivo: `DistributivoNoEncontrado`.

  Luego, `ParcialesDelDistributivo.crearVersionNueva`, `VersionNuevaEnGeneracion` y el servicio, con la versión bloqueada (`bloquearVigente`).

  **Resultado (2026-11-15):** 5 pruebas nuevas en `ParcialesDelDistributivoServicioTest`. Contra el esqueleto fallaron 4; la de «otra situación» pasaba porque el esqueleto devolvía vacío. Pasan con la implementación y las de Generación siguen en verde. La idempotencia mira el número y que la anterior esté ANULADA, no el motivo: hoy hay uno solo (está en el comentario).
- [X] T011 [US1] Aprobación ordena la versión nueva. Ver fallar primero en `OCS/test/.../aplicacion/RegistrarRechazoEnFirmaServicioTest.java`:
  - fuera de firma o con otra versión: no llama a Generación ni guarda nada;
  - en firma: llama a Generación y después, con el proceso bloqueado, guarda `RECHAZO_EN_FIRMA` y `VERSION_NUEVA`, sus dos cambios para la bitácora (`DISTRIBUTIVO_RECHAZADO_EN_FIRMA` y `DISTRIBUTIVO_VERSION_NUEVA`) y el aviso `FirmasPorAnular`;
  - un reintento con la versión ya creada en Generación completa las transiciones;
  - Generación responde vacío: no guarda nada y queda en el log.

  Luego:
  - `Distributivo.crearVersionNueva` y `DistributivoDesdeGeneracion`;
  - `AvisoDeFirma.avisar(FirmasPorAnular)`, sin oyente todavía;
  - `CambioDeFlujo.Accion.DISTRIBUTIVO_VERSION_NUEVA` y su rama en `FlujoDelDistributivoServicio.cambio`;
  - el servicio con los tres pasos de research §1.

  **Resultado (2026-11-15):** `RegistrarRechazoEnFirmaServicioTest` reescrita con 5 pruebas, más una de `admiteRechazoEnFirma` en `ProcesoAprobacionTest`. Contra los esqueletos fallaron 4. Las de «otro aviso ganó mientras tanto» y «sin proceso» pasaron porque el esqueleto conservaba el comportamiento de F14-H04. Pasan con la implementación, y las 385 de Validación OCS siguen en verde. Desvíos:
  - **El paso 1** necesita una pregunta al agregado sin bloquearlo: `ProcesoAprobacion.admiteRechazoEnFirma(version)`, que `rechazarEnFirma` también usa.
  - **El aviso a Firma** es un puerto propio, `AvisoDeAnulacion` (funcional), con `FirmasPorAnular`, y no un método más de `AvisoDeFirma`. Sumarlo a `AvisoDeFirma` le quitaba el `@FunctionalInterface` y rompía las lambdas de cuatro pruebas de integración y de `FlujoEnMemoria`. `AvisoDeFirmaSpring` implementa los dos. `FirmasPorAnular` lleva periodo, versión y momento, sin motivo: el único que Aprobación ordena hoy es el rechazo en firma.
  - **Los dobles de `Distributivo`** de cuatro pruebas de integración (`Aprobacion`, `Devolucion`, `EnvioARevision`, `EtapaDelConsolidado`) suman `crearVersionNueva`, vacío.
  - **La bitácora** de `DISTRIBUTIVO_VERSION_NUEVA` lleva la versión anulada (la anterior a la nueva) y el motivo en el detalle, con `version` antes y después.
  - **`FlujoEnMemoria`** suma `crearVersionNueva`, vacío.
- [X] T012 [US1] El repositorio de Aprobación. Ver fallar primero en `OCS/integrationTest/.../RepositorioProcesoIntegrationTest.java`: la transición `VERSION_NUEVA` se guarda y se lee, después del rechazo. Luego, lo que haga falta en `RepositorioProcesoJdbc`.

  **Resultado (2026-11-15):** la prueba nueva pasó al escribirse: `RepositorioProcesoJdbc` guarda y lee cualquier tipo de transición, y V6 ya admitía `VERSION_NUEVA`. Comprueba las tres transiciones en orden, `rechazoPendiente()` vacío y `rechazoVigente()` en el rechazo. Sin cambios en el repositorio.

**Checkpoint**: el rechazo deja la versión nueva y el flujo en EN_REVISION_VRA con ella; las firmas todavía no se anulan.

---

## Phase 4: User Story 2 - Las firmas de la versión rechazada quedan anuladas (Priority: P1)

**Goal**: Firma registra la anulación del documento de la versión rechazada, por orden de Aprobación; las firmas se conservan y nadie está en turno.

**Independent Test**: tras el rechazo en F3, el documento de la versión 1 está anulado con 2 firmas, sus archivos y validaciones intactos; al aprobar la versión 2, su documento pide F1.

- [X] T013 [US2] El repositorio de Firma. Ver fallar primero en `FIR/integrationTest/.../RepositorioDocumentosIntegrationTest.java`:
  - `registrarAnulacion` inserta con el número de firmas aceptadas, y la segunda vez devuelve `false`;
  - `firmasDe` trae la anulación;
  - con una anulación, `registrarValidacion` aceptada y `registrarRechazo` devuelven `false`;
  - las filas de `validacion_firma` no cambian.

  Luego, `RepositorioDocumentos.registrarAnulacion` y las comprobaciones en `RepositorioDocumentosJdbc`, con la generación bloqueada.

  **Resultado (2026-11-15):** 3 pruebas nuevas en `RepositorioDocumentosIntegrationTest`, vistas fallar contra el esqueleto: la anulación con F1, idempotente; una firma y un rechazo después de anular; una anulación y una firma a la vez. En la última, la anulación cuenta las firmas que quedaron, gane quien gane. `registrarAnulacion` recibe el documento y devuelve la anulación con las firmas contadas dentro del bloqueo, o vacío si ya había una. El doble en memoria de `DocumentoFirmableServicioTest` suma el método sin usarlo. Las de Firma siguen en verde.
- [X] T014 [US2] El caso de uso. Ver fallar primero en `FIR/test/.../aplicacion/AnularFirmasServicioTest.java`:
  - anula, anota `FIRMAS_ANULADAS` en la bitácora y publica `FirmasAnuladas` en la transacción;
  - ya anulado: nada;
  - sin documento para esa versión: nada, en el log.

  Luego:
  - `puertos/entrada/AnularFirmas`, `MotivoDeAnulacion` (`RECHAZO_EN_FIRMA`, análisis I2) y el evento `FirmasAnuladas` (API publicada);
  - `puertos/salida/AvisoDeFirmasAnuladas` y `AvisoDeFirmasAnuladasSpring`;
  - `CambioDeFirma.FIRMAS_ANULADAS`;
  - `AnularFirmasServicio` y su bean en `ConfiguracionFirma`.

  **Resultado (2026-11-15):** `AnularFirmasServicioTest` (3). Contra el esqueleto falló la de la anulación; las de «ya anulado» y «sin documento» pasaban porque el esqueleto no hacía nada. Pasan con la implementación, y las de Firma siguen en verde. La anulación guarda como momento el del rechazo, que trae la orden, como las transiciones de Aprobación. El aviso Spring se arma dentro del bean, sin bean propio.
- [X] T015 [US2] El estado del documento. Ver fallar primero en la prueba de contrato del documento de `FIR/test/.../adaptadores/entrada/`: con anulación, `anulacion` con el motivo, cuántas y cuándo; sin ella, el campo falta. Luego, `EstadoDelDocumento.anulacion`, el servicio que lo consulta y `DocumentoControlador`.

  **Resultado (2026-11-15):** una prueba nueva en `DocumentoContratoTest`, vista fallar sin el mapeo; valida el contrato y que el campo falta sin anulación. `EstadoDelDocumento` suma `anulacion` y conserva el constructor de seis componentes. La consulta de `DocumentoFirmableServicio` lee la anulación de las firmas del documento generado. Esa lectura no tiene prueba unitaria propia, porque el doble en memoria no registra anulaciones: la cubre la prueba de punta a punta de T017.
- [X] T016 [US2] El oyente de Aprobación. Ver fallar primero en `OCS/test/.../adaptadores/salida/AnulacionDeLasFirmasTest.java`: el aviso llama a `AnularFirmas` con el periodo, la versión, el motivo y el momento. Luego, `AnulacionDeLasFirmas` (`@ApplicationModuleListener`) y su bean en `ConfiguracionValidacionocs`.

  **Resultado (2026-11-15):** `AnulacionDeLasFirmasTest`, vista fallar contra el esqueleto; pasa con el oyente, que pasa `MotivoDeAnulacion.RECHAZO_EN_FIRMA` a Firma. El aviso es `AvisoDeAnulacion.FirmasPorAnular` (T011).
- [X] T017 [US2] De punta a punta en el proceso. Ver fallar primero en `APP/integrationTest/.../DocumentoFirmableIntegrationTest.java` (con el PDF/A real y la CA en memoria, como F14-H04): con F1 aceptada, el rechazo en F2 deja:
  - la v1 ANULADA y la v2 EDITABLE;
  - EN_REVISION_VRA con `VERSION_NUEVA`;
  - el documento v1 anulado con 1 firma.

  Después, aprobar la v2 genera su documento con F1 en turno, y «Pendientes de firma» de `docente.demo` solo trae la v2. La prueba de F14-H04 conserva su doble `@Primary` de `RegistrarRechazoEnFirma`; la nueva usa los casos de uso reales, en su propia clase de prueba si el doble lo exige (análisis A2). `ModulosTest` sin ciclos.

  **Resultado (2026-11-15):** una prueba nueva, `dadoElRechazoEnF2_cuandoAprobacionOrdenaAnular_entoncesFirmaAnulaYLaVersionDosEmpiezaEnF1`. Sube F1 y rechaza en F2 con los casos de uso reales de Firma. Después publica la orden real de Aprobación (`FirmasPorAnular`) en una transacción. Comprueba:
  - la anulación con 1 firma y por `sistema`;
  - F1 y su archivo conservados;
  - la entrada `FIRMAS_ANULADAS` para la bitácora;
  - que al entrar la versión 2 en firma su documento tiene F1 en turno y `docente.demo` solo la ve a ella.

  **Desvíos:**
  - **El alcance:** la vuelta a revisión y la versión nueva no van en esta prueba, porque necesitan un proceso de aprobación y un distributivo reales que el proceso no tiene sin datos. Las cubren `RegistrarRechazoEnFirmaServicioTest`, `VersionNuevaIntegrationTest` y el script (T022). Por eso la prueba de F14-H04 conserva su doble de `RegistrarRechazoEnFirma`.
  - **El doble del contenido** devuelve la versión de `Dobles.VERSIONES`.
  - **TDD:** se escribió con T013 a T016 hechos y pasó al escribirse: no se vio fallar.

  `ModulosTest` en verde.

**Checkpoint**: el rechazo deja la versión nueva y las firmas anuladas; la versión nueva se aprueba y empieza su cadena.

---

## Phase 5: User Story 3 - El Vicerrector sigue con la versión nueva (Priority: P1)

**Goal**: el estado del flujo muestra el rechazo con la versión anulada y la nueva, y «Aprobar el contenido» vuelve.

**Independent Test**: en «Consolidado del periodo», el Vicerrector ve la versión 2, el aviso del rechazo con «La versión 1 quedó anulada con sus firmas…» y «Aprobar el contenido».

- [X] T018 [US3] La vista del flujo. Ver fallar primero en `OCS/test/.../adaptadores/entrada/FlujoContratoTest.java`, con `FlujoEnMemoria`:
  - con la versión nueva: `rechazoEnFirma` con `versionAnulada` y `versionNueva`, `VERSION_NUEVA` en las transiciones y APROBAR_CONTENIDO en `acciones`;
  - sin versión nueva: `versionAnulada` sin `versionNueva`;
  - después de aprobar la versión nueva: sin `rechazoEnFirma`.

  Luego, `FlujoDelDistributivo.RechazoEnFirma` con las dos versiones (de `rechazoVigente()`) y `FlujoControlador`.

  **Resultado (2026-11-15):** dos pruebas nuevas en `FlujoContratoTest`, con `FlujoEnMemoria.versionNueva`; la de F14-H04 suma `versionAnulada` sin `versionNueva`. Se vio fallar la de la versión nueva. La de «aprobada otra vez» pasaba antes, porque la vista usaba `rechazoPendiente`. La vista toma ahora `rechazoVigente()` y la versión de la transición `VERSION_NUEVA` que lo sigue. Las 56 de `FlujoContratoTest` y las de Validación OCS en verde.
- [X] T019 [P] [US3] El aviso de la SPA. Ver fallar primero en `SPA/pantallas/comun/EstadoDelFlujo.test.tsx`:
  - con `versionNueva`: «La versión 1 quedó anulada con sus firmas; el distributivo sigue con la versión 2.» y sin «Para aprobar otra vez…»;
  - sin ella: el texto de F14-H04.

  Luego, `EstadoDelFlujo.tsx` y los tipos de `comun/flujo.ts`.

  **Resultado (2026-11-15):** una prueba nueva en `EstadoDelFlujo.test.tsx`, vista fallar; pasa con el texto que depende de `versionNueva`. El dato de F14-H04 suma `versionAnulada`, ahora obligatorio. `comun/flujo.ts` no cambió: toma el tipo del contrato generado.
- [X] T020 [P] [US3] Quitar la rama del rechazo de `SPA/pantallas/comun/AvisoDeVersionCongelada.tsx` (research §8), con sus dos pruebas en `Consolidado.test.tsx` e `Inconsistencias.test.tsx`. El aviso vuelve al texto de F13-H01, sin consultar el flujo. Si el componente queda trivial, se devuelve a cada pantalla como estaba.

  **Resultado (2026-11-15):** sin la rama, el componente solo era el `Alert` de F13-H01. Los cuatro archivos vuelven a como estaban antes de F14-H04 (`a8ac00e`), cuyo cambio en ellos era solo ese aviso, y se borra `AvisoDeVersionCongelada.tsx`. Tipos, ESLint y Prettier en verde; Vitest 582/582 (583, menos las dos quitadas, más la de T019).
- [X] T021 [US3] Playwright: correr `e2e/consolidado.spec.ts`, `e2e/firmas-pendientes.spec.ts` y `e2e/inconsistencias.spec.ts` con axe-core. Sin pruebas nuevas si nada cambió en los controles.

  **Resultado (2026-11-15):** 14/14, con axe-core sin fallas. Sin pruebas nuevas: los controles no cambiaron, solo el texto del aviso.

**Checkpoint**: las tres historias funcionan en la API y en la SPA.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T022 El script `deploy/probar_rechazo_en_firma.sh`, según `contracts/demostracion.md`:
  - el paso 6 suma la v1 ANULADA, la v2 EDITABLE con el motivo, las mismas asignaciones (los totales por tabla), `VERSION_NUEVA` y la anulación en Firma, en menos de 30 s;
  - el paso 8 aprueba la v2: CONGELADO_EN_FIRMA con la v2, su documento y `docente.demo` con F1 de la v2;
  - el paso 9 suma la bitácora de la versión nueva y de la anulación;
  - la limpieza borra también `firma.anulacion_de_firmas`.

  Correrlo sobre un perfil construido desde la rama; shellcheck sin hallazgos.

  **Resultado (2026-11-15):** 10/10 en 55 s, sobre un perfil construido desde la rama. EN_REVISION_VRA unos 0 s y las firmas anuladas unos 1 s después de rechazar (SC-001).
  - **El paso 6** compara las seis tablas de la versión por número de filas. La primera corrida falló porque exigía filas en todas, y la preparación no asigna actividades no lectivas: esa tabla queda en 0 en las dos versiones, y las demás tienen que tener alguna fila.
  - **`id_del_documento`** toma la versión 1, porque con la versión 2 hay dos documentos.
  - **La cabecera, el comentario del workflow y el mensaje de `MDD_CONSERVAR`** nombran F12-H04 y el quickstart de 051.

  shellcheck sin hallazgos.
- [ ] T023 Los scripts de `perfil-consolidado`, en su orden, sobre un perfil construido desde la rama (SC-005). Después, el recorrido a mano de `quickstart.md` §3, del autor.
- [X] T024 Correr completos:
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification`;
  - en la SPA, los tipos, ESLint, Prettier, Vitest y Playwright.

  **Resultado (2026-11-15):**
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` dejó tres fallas:
    - las dos intermitentes de siempre, `ClienteDeEventosLocalTest` y `ConfirmarLasDiezCargasIntegrationTest`, que pasan solas y con sus módulos completos;
    - `spotlessJavaCheck` de Validación OCS, por el formato del JavaDoc de T025. Después de `spotlessApply`, `:servicios:validacionocs:build integrationTest` pasa.
  - Con eso: 3 206 pruebas unitarias y 329 de integración, sin fallas.
  - Cobertura de líneas: Generación 90,1 %, Firma 80,7 % y Validación OCS 83,2 % (mínimo, 60 %); la más baja, Roles, 77,2 %. Spotless, SpotBugs, ArchUnit y Spring Modulith en verde.
  - SPA: tipos, ESLint, Prettier y Vitest 582/582 (T020); Playwright, en los tres archivos tocados, 14/14 (T021).
  - T023: los 36 scripts de `perfil-consolidado` pasan, en su orden; `probar_rechazo_en_firma.sh` en 51 s.
- [X] T025 [P] Revisar el JavaDoc: los tres pasos y su reintento en `RegistrarRechazoEnFirmaServicio`; la copia y la idempotencia en `crearVersionNueva`; la anulación y por qué no toca `validacion_firma`; `rechazoVigente` frente a `rechazoPendiente`.

  **Resultado (2026-11-15):** ya estaban los tres pasos y el reintento (`RegistrarRechazoEnFirmaServicio`), la copia y la idempotencia (`crearVersionNueva`), y `rechazoVigente` frente a `rechazoPendiente`. Se completaron `RegistrarRechazoEnFirma` y `CierreDeLaFirma`, que solo hablaban de la vuelta a revisión, y `AnulacionDeFirmas`, que ahora explica por qué la anulación es una fila del documento y no toca `validacion_firma` (principio VII).
- [ ] T026 Crear `specs/051-version-nueva/evidencia.md`, como la de 050.
- [ ] T027 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde. Verificar la demo (`contracts/demostracion.md`, solo lectura) y abrir el PR de evidencia con `Closes #59`. F12 (#11) sigue abierta por F12-H03 (#58). Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001, hecha.
- **Fundacional:** T002 a T008 en paralelo.
- **US1:**
  - T009 después de T003 y T006;
  - T010 después de T009;
  - T011 después de T005, T007 y T010;
  - T012 después de T005 y T007.
- **US2:**
  - T013 después de T004 y T008;
  - T014 después de T013;
  - T015 después de T002 y T014;
  - T016 después de T011 y T014;
  - T017 después de T011, T012 y T016.
- **US3:**
  - T018 después de T002 y T011;
  - T019 después de T002;
  - T020 sin dependencias;
  - T021 después de T019 y T020.
- **Cierre:** T022 después de T017 y T018; T023 a T027, al final.

### Parallel Opportunities

```text
T002 a T008.
T013 y T014 (Firma) en paralelo con T009 a T012 (Generación y Aprobación).
T019 y T020 (SPA) en paralelo con el backend de US2.
```

## Implementation Strategy

- **MVP:** Setup, Fundacional y US1 (T001 a T012): el rechazo crea la versión nueva y el flujo la registra.
- **Después:** US2, la anulación de las firmas; US3, la vista y la SPA.
- **Luego:** el script, las regresiones y el cierre.
