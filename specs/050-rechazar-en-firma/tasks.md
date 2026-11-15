---
description: "Tareas de F14-H04: rechazar en firma con una observación"
---

# Tasks: Rechazar en firma con una observación

**Input**: documentos de diseño en `specs/050-rechazar-en-firma/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero un esqueleto, y la implementación va en un commit aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-15, en la spec;
- D-105, que se escribe en T001;
- D-98, D-102, D-103, D-104, ADR-018 y MD-05.

**Organization**:
- **Fundacional:** D-105, los contratos, los errores, las migraciones y el dominio.
- **US1:** el firmante en turno rechaza con una observación (MVP).
- **US2:** el distributivo vuelve a revisión con la observación a la vista.
- **US3:** rechazar desde «Pendientes de firma».
- **Cierre:** el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, con `main`, `test` e `integrationTest` en `java/ec/mdd/<servicio>`:
  - `FIR` = `backend/servicios/firma/src`;
  - `OCS` = `backend/servicios/validacionocs/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Escribir **D-105** en `docs/decisiones.md`, en la fila del índice y en su sección:
  - el rechazo en Firma, en `rechazo_en_firma`, que cierra el documento (research §1);
  - la carrera con la subida: el bloqueo de la generación y la regla de la subida tardía (research §2);
  - la observación de 10 a 500, después del turno (research §3);
  - el evento `RechazoEnFirma` y la vuelta a la etapa activa anterior por `sistema` (research §4 y §5);
  - aprobar otra vez bloqueado hasta la versión nueva (research §6), y el límite con BORRADOR_CARRERA;
  - el script aparte (research §9).

  Relacionadas: RF-36, RF-59, D-98, D-101, D-102, D-103, D-104, ADR-018, MD-05.

  Revisar en el glosario «Rechazo en firma», `RechazoEnFirma` (§6), `rechazarEnFirma()` (§5) y `Observacion`, y anotar aquí el resultado.

  **Resultado (2026-11-15):** el glosario tiene «Rechazo en firma» («Es una devolución a EN_REVISION_VRA», §9), el evento `RechazoEnFirma` (§6: «El firmante en turno no firma» → Aprobación devuelve a EN_REVISION_VRA), la operación `rechazarEnFirma()` de `DocumentoFirmable` (§5) y `Observacion` («de una devolución o de un rechazo en firma»). Se usan esos nombres. Con la configuración por defecto la etapa de llegada es EN_REVISION_VRA, como dice el glosario; D-105 anota el caso desactivado.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T007.

- [X] T002 [P] Los contratos, según `contracts/firma.md` y `contracts/validacionocs.md`:
  - en `contratos/firma/openapi.yaml`: `rechazarEnFirma` (`POST /api/firma/documentos/{documento}/rechazos`, `x-mdd-permiso` `DISTRIBUTIVO:FIRMA`), `SolicitudDeRechazo` («`observacion`, `maxLength: 500`»), `RechazoEnFirma` y sus respuestas; la descripción de `subirFirma` con el documento rechazado;
  - en `contratos/validacionocs/openapi.yaml`: `RechazoEnFirmaDelFlujo`, `EstadoDelFlujo.rechazoEnFirma`, `RECHAZO_EN_FIRMA` en el tipo de transición y el 409 de la aprobación y de la revisión;
  - ninguna descripción de una operación que escribe nombra la bitácora (`BitacoraSinBorradoContratoTest`).

  **Resultado (2026-11-15):** los dos contratos actualizados; `BitacoraSinBorradoContratoTest` y `verificar:ruptura` pasan. La operación nueva la implementa `FirmasPendientesControlador`, el de la subida, no `DocumentoControlador`: queda un esqueleto que responde 500 hasta T011. `PermisosFirmaTest` cuenta seis operaciones.
- [X] T003 [P] Los dos códigos en `contratos/comun/errores.md`, con sus 8 columnas. Ver fallar primero `ErroresFirmaTest` con `ObservacionInvalida` y `ErroresValidacionocsTest` con `RequiereVersionNueva`; luego, `ErroresFirma` y `ErroresValidacionocs`.
- [X] T004 [P] La migración `FIR/main/resources/db/migration/firma/V6__rechazo_en_firma.sql` según `data-model.md`. Antes, ver fallar en `DocumentoFirmableEsquemaIntegrationTest`, con el rol del proceso:
  - inserta un rechazo; un segundo del mismo documento falla por la clave;
  - una observación de 9 caracteres, recortada, falla por el `check`;
  - no puede actualizar ni borrar.
- [X] T005 [P] La migración `OCS/main/resources/db/migration/validacionocs/V5__rechazo_en_firma.sql`: el tipo `RECHAZO_EN_FIRMA` en `transicion_tipo_check` y la tabla `rechazo_en_firma`. Antes, ver fallar en `OCS/integrationTest/.../ValidacionocsEsquemaIntegrationTest.java`: inserta una transición `RECHAZO_EN_FIRMA` con su rechazo, y el rol no puede actualizarlo.

  **Resultado (2026-11-15):**
  - Hallazgo: `transicion.tipo` era `varchar(15)` y `RECHAZO_EN_FIRMA` tiene 16 caracteres; V5 lo ensancha a 20.
  - Validación OCS no revoca permisos en ninguna de sus tablas, y sus migraciones de prueba corren como superusuario: «solo de inserción» lo cumple el código, como `observacion_de_devolucion` (V4). La prueba comprueba la tabla, una fila por transición y el `check` de la observación, no el `revoke`. En Firma sí se revoca (V6), como en V2 a V5.
- [X] T006 [P] El dominio de Firma. Ver fallar primero:
  - `RechazoDelFirmanteTest`: la observación vacía, de 9 y de 501 caracteres, y con espacios en los extremos (se recorta);
  - `FirmasDelDocumentoTest`: con rechazo, `pasoEnTurno()` vacío y `enTurno` falso; el constructor de cuatro componentes, sin rechazo.

  Luego, `RechazoEnFirma`, `ObservacionInvalida` y `FirmasDelDocumento.rechazo`.

  **Resultado (2026-11-15):** el objeto de valor del dominio se llama `RechazoDelFirmante` (`RechazoDelFirmanteTest`), no `RechazoEnFirma`: el glosario usa ese nombre para el evento hacia Aprobación (§6), que va en `puertos.entrada`, y dos clases con el mismo nombre en Firma confundirían. Las pruebas se vieron fallar contra un esqueleto (4 fallas) y pasan con la implementación; las demás de Firma siguen en verde.
- [X] T007 [P] El dominio de Aprobación. Ver fallar primero:
  - `ProcesoAprobacionTest` `rechazarEnFirma`:
    - en CONGELADO_EN_FIRMA con esa versión: una transición `RECHAZO_EN_FIRMA` a EN_REVISION_VRA por `sistema`, con el hash de la congelación y el rechazo;
    - con EN_REVISION_VRA desactivada en la copia: a la etapa activa anterior;
    - repetido, en otra etapa o con otra versión: vacío;
  - con un rechazo pendiente: `validarAprobacion` y `validarRevision` (si congela al avanzar) lanzan `RequiereVersionNueva`; `acciones` no trae APROBAR_CONTENIDO;
  - `TransicionTest`: el rechazo solo en `RECHAZO_EN_FIRMA`, y siempre.

  Luego `TipoDeTransicion.RECHAZO_EN_FIRMA`, `RechazoDelFirmante`, `Transicion.rechazo` (conservando los constructores de antes), `anteriorActiva`, `rechazoPendiente` y `RequiereVersionNueva`.

  **Resultado (2026-11-15):** 6 pruebas vistas fallar contra los esqueletos; pasan con la implementación, y las demás de Validación OCS siguen en verde. El bloqueo está en `validarAprobacion`, en `validarRevision` y en `validarAvance` cuando congelan al avanzar: es la regla de D-100 («todo paso hacia CONGELADO_EN_FIRMA congela»), y el envío del último parcial no llega a congelar porque, después de un rechazo, todos los parciales ya están enviados.

**Checkpoint**: el dominio decide, sin persistencia ni eventos.

---

## Phase 3: User Story 1 - El firmante en turno rechaza con una observación (Priority: P1) 🎯 MVP

**Goal**: el rechazo registrado, el documento cerrado y el aviso publicado.

**Independent Test**: con F3 en turno, el Rector rechaza sin observación (400) y con una (201); después nadie está en turno.

- [X] T008 [US1] El repositorio. Ver fallar primero en `RepositorioDocumentosIntegrationTest`:
  - `registrarRechazo` guarda y `firmas` lo lee;
  - con una firma registrada entretanto, devuelve falso sin guardar;
  - un rechazo y una subida aceptada a la vez, en dos hilos: queda solo uno;
  - `registrarValidacion` aceptada con un rechazo registrado devuelve falso.

  Luego `RepositorioDocumentos.registrarRechazo` y `RepositorioDocumentosJdbc`.

  **Resultado (2026-11-15):** las 4 pruebas nuevas, vistas fallar contra un esqueleto, pasan; las de integración y unitarias de Firma siguen en verde. El doble en memoria de `DocumentoFirmableServicioTest` suma el método sin usarlo.
- [X] T009 [US1] `RechazarEnFirmaServicio`. Ver fallar primero en `RechazarEnFirmaServicioTest`:
  - fuera de turno: `SinTurnoDeFirma`, sin mirar la observación ni registrar;
  - la observación inválida: `ObservacionInvalida`, sin registrar;
  - el registro, el cambio `RECHAZO_EN_FIRMA` para la bitácora y `AvisoDeRechazoEnFirma.avisar`, en la misma transacción;
  - perdido contra una firma: `SinTurnoDeFirma`, sin cambio ni aviso.

  Luego `RechazarEnFirma`, `RechazoRegistrado`, el evento `RechazoEnFirma` (en `puertos.entrada`), el puerto `AvisoDeRechazoEnFirma`, su adaptador con el publicador de Spring y el bean.

  **Resultado (2026-11-15):** `RechazarEnFirmaServicioTest` (5), visto fallar contra los esqueletos, pasa; las demás de Firma siguen en verde. El cambio para la bitácora es `RECHAZO_EN_FIRMA`, con el periodo, la versión y el paso en el objeto y la observación como valor nuevo.
- [X] T010 [US1] `SubirFirmaServicio`. Ver fallar primero en `SubirFirmaServicioTest`: aceptada con el documento rechazado entretanto, se registra como rechazada por «mismo documento» (la regla de la subida tardía). Luego, si hace falta, el servicio.

  **Resultado (2026-11-15):** sin código nuevo en el servicio ni prueba nueva. El servicio ya convierte cualquier `false` de `registrarValidacion` en la subida tardía (`dadaOtraSubidaQueFirmoAntes_cuandoSeRegistra_entoncesSeRechazaPorNoSerLaVersionVigente`), y que el repositorio devuelva `false` con un rechazo registrado lo prueba T008 contra PostgreSQL (`dadoUnRechazoRegistrado_cuandoLlegaUnaFirmaAceptada_entoncesNoSeRegistra`). Una prueba aquí repetiría la existente con otro nombre. Solo se ajustó el JavaDoc de `SubirFirmaServicio`.
- [X] T011 [US1] El controlador. Ver fallar primero en `RechazarEnFirmaContratoTest` y `PermisosFirmaTest`: el 201 con su cuerpo, los dos 400, el 403 sin permiso y fuera de turno, y el 401. Luego, el endpoint en `FirmasPendientesControlador`.

  **Resultado (2026-11-15):** `RechazarEnFirmaContratoTest` (6): fallaron 4 contra el esqueleto (el 201, el 400 del catálogo y los dos 403); el 400 común y el 401 ya pasaban porque los dan la validación del contrato y la seguridad. Con el endpoint pasan todas, y `PermisosFirmaTest` ve pasar el permiso de la operación nueva. El doble de `RechazarEnFirma` vive en `FirmasPendientesContratoTest.Dobles`, que comparten las tres pruebas del controlador.

**Checkpoint**: el firmante en turno rechaza y el documento queda cerrado.

---

## Phase 4: User Story 2 - El distributivo vuelve a revisión con la observación a la vista (Priority: P1)

**Goal**: EN_REVISION_VRA por `sistema`, una sola vez, con la observación en el flujo y la aprobación bloqueada.

**Independent Test**: tras el rechazo, el flujo está en EN_REVISION_VRA con `rechazoEnFirma`, y aprobar da 409.

- [X] T012 [US2] Aprobación. Ver fallar primero:
  - en `RegistrarRechazoEnFirmaServicioTest` (Mockito): guarda la transición con su rechazo y registra `DISTRIBUTIVO_RECHAZADO_EN_FIRMA` en la bitácora; repetido o en otra etapa, no guarda nada ni lanza;
  - en `RepositorioProcesoIntegrationTest` (PostgreSQL real): guarda y lee una transición `RECHAZO_EN_FIRMA` con su rechazo, y el proceso leído tiene el rechazo pendiente.

  Luego `RegistrarRechazoEnFirma`, `RegistrarRechazoEnFirmaServicio`, `CierreDeLaFirma.alRechazar`, `CambioDeFlujo.Accion.DISTRIBUTIVO_RECHAZADO_EN_FIRMA`, el repositorio de procesos con `rechazo_en_firma` y la configuración.

  **Resultado (2026-11-15):** `RegistrarRechazoEnFirmaServicioTest` (3) y la prueba nueva de `RepositorioProcesoIntegrationTest` se vieron fallar contra los esqueletos (una cada una; las de aviso repetido y sin proceso pasaban porque el esqueleto no hacía nada) y pasan con la implementación. El oyente se llama `alRechazarseEnFirma`, junto a `alCompletarseLaCadena`. La transición usa como momento el del rechazo, como la cadena completa usa el de la última firma. Las pruebas unitarias y de integración de Validación OCS siguen en verde.
- [X] T013 [US2] El flujo y la aprobación. Ver fallar primero en `FlujoContratoTest` (con `FlujoEnMemoria`) y `PermisosValidacionocsTest`:
  - `rechazoEnFirma` en el estado del flujo con un rechazo pendiente, y sin `APROBAR_CONTENIDO`;
  - aprobar con un rechazo pendiente: 409 `VALIDACIONOCS_REQUIERE_VERSION_NUEVA`, sin pedir a Generación que congele.

  Luego `FlujoControlador` y el caso de uso de aprobar, si hace falta.

  **Resultado (2026-11-15):** tres pruebas nuevas en `FlujoContratoTest`, con `FlujoEnMemoria.rechazarEnFirma`. Se vio fallar la de `rechazoEnFirma` en el estado del flujo; el 409 y la ausencia del campo sin rechazo pasaron al escribirse, porque el bloqueo ya estaba en el dominio (T007) y el campo es opcional. El caso de uso de aprobar no cambió: llama a `validarAprobacion` antes de pedir a Generación que congele (F13-H01). `PermisosValidacionocsTest` sigue en verde con el constructor de antes de la vista.
- [X] T014 [US2] En `apps:distributivo`, `DocumentoFirmableIntegrationTest`: F1 aceptada, el rechazo de F2, y Aprobación recibe el aviso una sola vez aunque se publique otra vez. Después del rechazo, la firma F1 y el archivo vigente siguen iguales (FR-007). `ModulosTest` sin ciclos.

  **Resultado (2026-11-15):** `dadaF1Aceptada_cuandoElVicerrectorRechazaEnF2_entoncesSeCierraYAprobacionRecibeElAviso`, con el PDF/A real, la CA en memoria y un doble de `RegistrarRechazoEnFirma`, como la cadena completa de F14-H03. Comprueba el aviso una sola vez después de confirmar, la subida y el segundo rechazo con `SinTurnoDeFirma`, F1 y su archivo iguales, y una sola entrada `RECHAZO_EN_FIRMA` para la bitácora. Se escribió con la implementación hecha: no se vio fallar. `ModulosTest` sin ciclos.

**Checkpoint**: el rechazo devuelve el distributivo a revisión.

---

## Phase 5: User Story 3 - Rechazar desde «Pendientes de firma» (Priority: P2)

**Goal**: el diálogo y el aviso del flujo, según `contracts/spa.md`.

**Independent Test**: en «Pendientes de firma», rechazar con observación; la fila desaparece.

- [X] T015 [US3] Regenerar los tipos de la API. Ver fallar primero en `FirmasPendientes.test.tsx`:
  - «Rechazar» abre el diálogo; sin observación o con 9 caracteres no envía;
  - con 201, «Rechazo registrado» y la lista otra vez; con 400, el mensaje junto al campo; con 403, el mensaje común.

  Luego `SPA/pantallas/firmas/FirmasPendientes.tsx` y `consultas.ts`.

  **Resultado (2026-11-15):** 5 pruebas nuevas en `FirmasPendientes.test.tsx`, vistas fallar (no había botón); pasan con «Rechazar» en cada fila, el diálogo nuevo `DialogoDeRechazo.tsx` (como el de devolución de F12-H05) y `rechazarEnFirma` en `consultas.ts`. Un 400 se muestra dentro del diálogo; cualquier otro fallo (el 403 de quien ya no está en turno) lo cierra, avisa y vuelve a pedir la lista. ESLint pidió quitar `autoFocus` (jsx-a11y): el diálogo de MUI ya lleva el foco adentro.
- [X] T016 [US3] Ver fallar primero en la prueba de `EstadoDelFlujo` el aviso «Rechazado en firma» con la observación y la nota de la versión nueva. Luego `SPA/pantallas/comun/EstadoDelFlujo.tsx`.

  **Resultado (2026-11-15):** 2 pruebas nuevas en `EstadoDelFlujo.test.tsx`; se vio fallar la del aviso y pasan con «Rechazado en firma» (paso y rol, quién, cuándo, la observación y la nota de la versión nueva). Sin la acción, no hay «Aprobar el contenido»: lo decide la API. La constancia de la versión congelada sigue a la vista, porque la versión sigue congelada hasta F12-H04.
- [X] T017 [US3] `e2e/firmas-pendientes.spec.ts`: el rechazo con teclado, el foco al cerrar y axe-core sin fallas con el diálogo abierto y después del rechazo.

  **Resultado (2026-11-15):** una prueba nueva: abre el diálogo con Enter, Escape lo cierra y el foco vuelve a «Rechazar», rechaza con el teclado y ve «Rechazo registrado» y la lista vacía; axe-core sin fallas con el diálogo abierto y después. Las 6 del archivo pasan. Se escribió con la pantalla ya hecha: no se vio fallar.

**Checkpoint**: el firmante rechaza sin salir de la pantalla.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T018 `deploy/probar_rechazo_en_firma.sh` según `contracts/demostracion.md` (10 pasos, `MDD_CONSERVAR=1`), en `.github/workflows/perfil-consolidado.yml` después de `probar_documento_firmable.sh`. La limpieza borra las dos tablas nuevas. shellcheck sin hallazgos.

  **Resultado (2026-11-15):** 10/10 en la primera corrida, en 61 s, sobre un perfil construido desde la rama; EN_REVISION_VRA unos 1 s después del rechazo. La preparación (cargas, consolidado, aprobación, firma de prueba y limpieza) es la de `probar_documento_firmable.sh`, copiada como en los demás scripts; los pasos 2 a 9 son nuevos. En el paso 3, además de `rectorado.demo`, rechazan fuera de turno `docente.demo` (ya firmó F1) y `consolidacion.demo`. La limpieza borra `firma.rechazo_en_firma` antes de la generación; `validacionocs.rechazo_en_firma` se va en cascada con el proceso. shellcheck sin hallazgos.
- [X] T019 Correr todos los scripts del workflow sobre un perfil construido desde la rama. Recorrer `quickstart.md` §3 a mano: SC-002 y SC-003.

  **Resultado (2026-11-15):** los 36 de `perfil-consolidado` pasan, en su orden, sobre un perfil construido desde la rama; `probar_rechazo_en_firma.sh` en 48 s y `probar_documento_firmable.sh` en 73 s. SC-002: EN_REVISION_VRA unos 1 s después del rechazo. El recorrido a mano de `quickstart.md` §3, del autor: «Rechazar en firma» no se envía sin observación ni con menos de 10 caracteres; con una observación, «Rechazo registrado» en 45 s (SC-003); en «Consolidado del periodo», EN_REVISION_VRA con «Rechazado en firma», el paso, quién, cuándo y la observación, sin «Aprobar el contenido»; `rectorado.demo` sin pendientes. Del recorrido salieron dos textos corregidos: el aviso y el diálogo dicen «2026-I, versión 1 (paso «Revisado por»)» en lugar de empezar por el rótulo, y el aviso de la versión congelada, con un rechazo pendiente, dice que hace falta una versión nueva (`AvisoDeVersionCongelada`, en «Consolidado del periodo» y en el panel de inconsistencias). Vitest 583/583; Playwright de las tres pantallas, 14/14.
- [X] T020 Correr completos:
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification`;
  - en la SPA, los tipos, ESLint, Prettier, Vitest y Playwright (SC-005).

  **Resultado (2026-11-15):**
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue`: 3 180 pruebas unitarias y 318 de integración. Fallaron 3, en módulos que esta rama no toca: las dos intermitentes de siempre (`ClienteDeEventosLocalTest` y `ConfirmarLasDiezCargasIntegrationTest`) y `EntregaIntegrationTest` de `plataforma:eventos` (un `ConditionTimeoutException`, mientras Vitest corría a la vez). Las tres pasan corridas solas (5/5, 9/9 y 3/3). Spring Modulith en verde.
  - Cobertura de líneas: Firma 81,8 %, Validación OCS 82,9 %; la más baja, Roles, 77,2 %.
  - Spotless y SpotBugs en verde.
  - SPA: tipos, ESLint y Prettier en verde; Vitest 581/581 (con Gradle corriendo a la vez, 7 pruebas de otras pantallas agotaron los 15 s; solo, todo pasa); Playwright 129/129.
- [X] T021 Revisar el JavaDoc (principio XII): el cierre del documento, la carrera con la subida, la etapa de llegada y el bloqueo de aprobar.

  **Resultado (2026-11-15):** el cierre del documento en `FirmasDelDocumento` y `FirmanteEnTurno`; la carrera con la subida en `RepositorioDocumentos.registrarRechazo` y en `SubirFirmaServicio`; el orden turno → observación en `RechazarEnFirmaServicio` y su puerto; la etapa activa anterior y la idempotencia en `ProcesoAprobacion.rechazarEnFirma`; el bloqueo en `validarAprobacion`, `validarRevision` y `validarAvance`, con su `@throws`; el evento en `RechazoEnFirma`. Se completaron el `@throws` de `validarAvance` y la mención del rechazo en `FirmanteEnTurno`.
- [ ] T022 Crear `specs/050-rechazar-en-firma/evidencia.md`, como la de 049.
- [ ] T023 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde. Verificar la demo (`quickstart.md` §4, solo lectura) y abrir el PR de evidencia con `Closes #65`. Cerrar la épica F14 (#13) si el autor lo aprueba. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001.
- **Fundacional:** T002 a T007 en paralelo, después de T001.
- **US1:**
  - T008 después de T004 y T006;
  - T009 después de T008;
  - T010 después de T008;
  - T011 después de T002, T003 y T009.
- **US2:**
  - T012 después de T005, T007 y T009;
  - T013 después de T002, T003 y T012;
  - T014 después de T011 y T012.
- **US3:** T015 después de T011; T016 después de T013; T017 después de T015 y T016.
- **Cierre:** T018 después de T014 y T017; T019 a T023, al final.

### Parallel Opportunities

```text
T002, T003, T004, T005, T006 y T007.
T012 (Aprobación) en paralelo con T010 y T011 (Firma).
T015 y T016 (SPA) en paralelo.
```

## Implementation Strategy

- **MVP:** Setup, Fundacional y US1 (T001 a T011): el firmante en turno rechaza y el documento queda cerrado.
- **Después:** US2, la vuelta a revisión con la observación y el bloqueo; US3, la pantalla.
- **Luego:** el script, las regresiones y el cierre.
