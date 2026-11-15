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

- [ ] T001 Escribir **D-105** en `docs/decisiones.md`, en la fila del índice y en su sección:
  - el rechazo en Firma, en `rechazo_en_firma`, que cierra el documento (research §1);
  - la carrera con la subida: el bloqueo de la generación y la regla de la subida tardía (research §2);
  - la observación de 10 a 500, después del turno (research §3);
  - el evento `RechazoEnFirma` y la vuelta a la etapa activa anterior por `sistema` (research §4 y §5);
  - aprobar otra vez bloqueado hasta la versión nueva (research §6), y el límite con BORRADOR_CARRERA;
  - el script aparte (research §9).

  Relacionadas: RF-36, RF-59, D-98, D-101, D-102, D-103, D-104, ADR-018, MD-05.

  Revisar en el glosario «Rechazo en firma», `RechazoEnFirma` (§6), `rechazarEnFirma()` (§5) y `Observacion`, y anotar aquí el resultado.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T007.

- [ ] T002 [P] Los contratos, según `contracts/firma.md` y `contracts/validacionocs.md`:
  - en `contratos/firma/openapi.yaml`: `rechazarEnFirma` (`POST /api/firma/documentos/{documento}/rechazos`, `x-mdd-permiso` `DISTRIBUTIVO:FIRMA`), `SolicitudDeRechazo` («`observacion`, `maxLength: 500`»), `RechazoEnFirma` y sus respuestas; la descripción de `subirFirma` con el documento rechazado;
  - en `contratos/validacionocs/openapi.yaml`: `RechazoEnFirmaDelFlujo`, `EstadoDelFlujo.rechazoEnFirma`, `RECHAZO_EN_FIRMA` en el tipo de transición y el 409 de la aprobación y de la revisión;
  - ninguna descripción de una operación que escribe nombra la bitácora (`BitacoraSinBorradoContratoTest`).
- [ ] T003 [P] Los dos códigos en `contratos/comun/errores.md`, con sus 8 columnas. Ver fallar primero `ErroresFirmaTest` con `ObservacionInvalida` y `ErroresValidacionocsTest` con `RequiereVersionNueva`; luego, `ErroresFirma` y `ErroresValidacionocs`.
- [ ] T004 [P] La migración `FIR/main/resources/db/migration/firma/V6__rechazo_en_firma.sql` según `data-model.md`. Antes, ver fallar en `DocumentoFirmableEsquemaIntegrationTest`, con el rol del proceso:
  - inserta un rechazo; un segundo del mismo documento falla por la clave;
  - una observación de 9 caracteres, recortada, falla por el `check`;
  - no puede actualizar ni borrar.
- [ ] T005 [P] La migración `OCS/main/resources/db/migration/validacionocs/V5__rechazo_en_firma.sql`: el tipo `RECHAZO_EN_FIRMA` en `transicion_tipo_check` y la tabla `rechazo_en_firma`. Antes, ver fallar en la prueba de esquema de Validación OCS: inserta una transición `RECHAZO_EN_FIRMA` con su rechazo, y el rol no puede actualizarlo.
- [ ] T006 [P] El dominio de Firma. Ver fallar primero:
  - `RechazoEnFirmaTest`: la observación vacía, de 9 y de 501 caracteres, y con espacios en los extremos (se recorta);
  - `FirmasDelDocumentoTest`: con rechazo, `pasoEnTurno()` vacío y `enTurno` falso; el constructor de cuatro componentes, sin rechazo.

  Luego, `RechazoEnFirma`, `ObservacionInvalida` y `FirmasDelDocumento.rechazo`.
- [ ] T007 [P] El dominio de Aprobación. Ver fallar primero:
  - `ProcesoAprobacionTest` `rechazarEnFirma`:
    - en CONGELADO_EN_FIRMA con esa versión: una transición `RECHAZO_EN_FIRMA` a EN_REVISION_VRA por `sistema`, con el hash de la congelación y el rechazo;
    - con EN_REVISION_VRA desactivada en la copia: a la etapa activa anterior;
    - repetido, en otra etapa o con otra versión: vacío;
  - con un rechazo pendiente: `validarAprobacion` y `validarRevision` (si congela al avanzar) lanzan `RequiereVersionNueva`; `acciones` no trae APROBAR_CONTENIDO;
  - `TransicionTest`: el rechazo solo en `RECHAZO_EN_FIRMA`, y siempre.

  Luego `TipoDeTransicion.RECHAZO_EN_FIRMA`, `RechazoDelFirmante`, `Transicion.rechazo` (conservando los constructores de antes), `anteriorActiva`, `rechazoPendiente` y `RequiereVersionNueva`.

**Checkpoint**: el dominio decide, sin persistencia ni eventos.

---

## Phase 3: User Story 1 - El firmante en turno rechaza con una observación (Priority: P1) 🎯 MVP

**Goal**: el rechazo registrado, el documento cerrado y el aviso publicado.

**Independent Test**: con F3 en turno, el Rector rechaza sin observación (400) y con una (201); después nadie está en turno.

- [ ] T008 [US1] El repositorio. Ver fallar primero en `RepositorioDocumentosIntegrationTest`:
  - `registrarRechazo` guarda y `firmas` lo lee;
  - con una firma registrada entretanto, devuelve falso sin guardar;
  - un rechazo y una subida aceptada a la vez, en dos hilos: queda solo uno;
  - `registrarValidacion` aceptada con un rechazo registrado devuelve falso.

  Luego `RepositorioDocumentos.registrarRechazo` y `RepositorioDocumentosJdbc`.
- [ ] T009 [US1] `RechazarEnFirmaServicio`. Ver fallar primero en `RechazarEnFirmaServicioTest`:
  - fuera de turno: `SinTurnoDeFirma`, sin mirar la observación ni registrar;
  - la observación inválida: `ObservacionInvalida`, sin registrar;
  - el registro, el cambio `RECHAZO_EN_FIRMA` para la bitácora y `AvisoDeRechazoEnFirma.avisar`, en la misma transacción;
  - perdido contra una firma: `SinTurnoDeFirma`, sin cambio ni aviso.

  Luego `RechazarEnFirma`, `RechazoRegistrado`, el evento `RechazoEnFirma` (en `puertos.entrada`), el puerto `AvisoDeRechazoEnFirma`, su adaptador con el publicador de Spring y el bean.
- [ ] T010 [US1] `SubirFirmaServicio`. Ver fallar primero en `SubirFirmaServicioTest`: aceptada con el documento rechazado entretanto, se registra como rechazada por «mismo documento» (la regla de la subida tardía). Luego, si hace falta, el servicio.
- [ ] T011 [US1] El controlador. Ver fallar primero en `RechazarEnFirmaContratoTest` y `PermisosFirmaTest`: el 201 con su cuerpo, los dos 400, el 403 sin permiso y fuera de turno, y el 401. Luego, el endpoint en `DocumentoControlador`.

**Checkpoint**: el firmante en turno rechaza y el documento queda cerrado.

---

## Phase 4: User Story 2 - El distributivo vuelve a revisión con la observación a la vista (Priority: P1)

**Goal**: EN_REVISION_VRA por `sistema`, una sola vez, con la observación en el flujo y la aprobación bloqueada.

**Independent Test**: tras el rechazo, el flujo está en EN_REVISION_VRA con `rechazoEnFirma`, y aprobar da 409.

- [ ] T012 [US2] Aprobación. Ver fallar primero en `RegistrarRechazoEnFirmaServicioTest` (Mockito):
  - guarda la transición con su rechazo y registra `DISTRIBUTIVO_RECHAZADO_EN_FIRMA` en la bitácora;
  - repetido o en otra etapa, no guarda nada ni lanza.

  Luego `RegistrarRechazoEnFirma`, `RegistrarRechazoEnFirmaServicio`, `CierreDeLaFirma.alRechazar`, `CambioDeFlujo.Accion.DISTRIBUTIVO_RECHAZADO_EN_FIRMA`, el repositorio de procesos con `rechazo_en_firma` y la configuración.
- [ ] T013 [US2] El flujo y la aprobación. Ver fallar primero en las pruebas de contrato de Validación OCS:
  - `rechazoEnFirma` en el estado del flujo con un rechazo pendiente, y sin `APROBAR_CONTENIDO`;
  - aprobar con un rechazo pendiente: 409 `VALIDACIONOCS_REQUIERE_VERSION_NUEVA`, sin pedir a Generación que congele.

  Luego `FlujoControlador` y el caso de uso de aprobar, si hace falta.
- [ ] T014 [US2] En `apps:distributivo`, `DocumentoFirmableIntegrationTest`: F1 aceptada, el rechazo de F2, y Aprobación recibe el aviso una sola vez aunque se publique otra vez. `ModulosTest` sin ciclos.

**Checkpoint**: el rechazo devuelve el distributivo a revisión.

---

## Phase 5: User Story 3 - Rechazar desde «Pendientes de firma» (Priority: P2)

**Goal**: el diálogo y el aviso del flujo, según `contracts/spa.md`.

**Independent Test**: en «Pendientes de firma», rechazar con observación; la fila desaparece.

- [ ] T015 [US3] Regenerar los tipos de la API. Ver fallar primero en `FirmasPendientes.test.tsx`:
  - «Rechazar» abre el diálogo; sin observación o con 9 caracteres no envía;
  - con 201, «Rechazo registrado» y la lista otra vez; con 400, el mensaje junto al campo; con 403, el mensaje común.

  Luego `SPA/pantallas/firmas/FirmasPendientes.tsx` y `consultas.ts`.
- [ ] T016 [US3] Ver fallar primero en la prueba de `EstadoDelFlujo` el aviso «Rechazado en firma» con la observación y la nota de la versión nueva. Luego `SPA/pantallas/comun/EstadoDelFlujo.tsx`.
- [ ] T017 [US3] `e2e/firmas-pendientes.spec.ts`: el rechazo con teclado, el foco al cerrar y axe-core sin fallas con el diálogo abierto y después del rechazo.

**Checkpoint**: el firmante rechaza sin salir de la pantalla.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T018 `deploy/probar_rechazo_en_firma.sh` según `contracts/demostracion.md` (10 pasos, `MDD_CONSERVAR=1`), en `.github/workflows/perfil-consolidado.yml` después de `probar_documento_firmable.sh`. La limpieza borra las dos tablas nuevas. shellcheck sin hallazgos.
- [ ] T019 Correr todos los scripts del workflow sobre un perfil construido desde la rama. Recorrer `quickstart.md` §3 a mano: SC-002 y SC-003.
- [ ] T020 Correr completos:
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification`;
  - en la SPA, los tipos, ESLint, Prettier, Vitest y Playwright (SC-005).
- [ ] T021 Revisar el JavaDoc (principio XII): el cierre del documento, la carrera con la subida, la etapa de llegada y el bloqueo de aprobar.
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
