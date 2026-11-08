---

description: "Tareas de F10-H03: enviar a revisión con cero inconsistencias abiertas"
---

# Tasks: Enviar a revisión con cero inconsistencias abiertas

**Input**: documentos de diseño en `specs/042-enviar-revision/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-08, en la spec;
- D-97, escrita en [`docs/decisiones.md`](../../docs/decisiones.md) el 2026-11-08;
- MD-07, D-83, D-95, ADR-003 y D-16;
- RF-29, RF-32 y RF-34.

**Organization**:
- **Fundacional:** el contrato, los errores, el valor `CondicionesDeRevision` y el método publicado de Generación.
- **US1:** el envío que pasa (MVP).
- **US2:** el rechazo con inconsistencias abiertas.
- **US3:** el rechazo sin consolidar, desactualizado o sin informe.
- **US4:** solo el rol de EN_CONSOLIDACION envía.
- **Cierre:** la pantalla, el script, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>`, e `integrationTest` donde aplica:
  - `GEN` = `backend/servicios/generacion/src`;
  - `VAL` = `backend/servicios/validacionocs/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [ ] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §6 y §7). «Panel de inconsistencias», «Informe sustentado», «Responsable de consolidación» y `ConsolidadoEnviadoARevision` ya están. Confirmar si «Condiciones de revisión» es un término nuevo del negocio. Si lo es, proponerlo al autor y anotarlo en `evidencia.md`. Anotar el resultado aquí.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T004.

- [ ] T002 Agregar a `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`:
  - la operación `enviarARevision` (`POST /api/validacionocs/periodos/{periodo}/revision`), en la etiqueta `Aprobacion`, con `x-mdd-permiso: CONSOLIDACION:APROBACION`, sin cuerpo y con `periodo` `^[A-Za-z0-9._-]{1,20}$`;
  - sus respuestas 200 `EstadoDelFlujo`, 400, 401, 403, 404, 409 (los seis códigos nuevos y `VALIDACIONOCS_PERIODO_CERRADO`), 500, 502 y 504;
  - `ENVIAR_A_REVISION` en `AccionDelFlujo`.

  Regenerar `SPA/api/generado/validacionocs.d.ts` y compilar `:servicios:validacionocs`.

  La descripción de `enviarARevision` no nombra la bitácora: `BitacoraSinBorradoContratoTest` solo admite que la nombre la escritura de Usuarios, y en F12-H02 eso rompió la CI. Gradle no vuelve a correr Usuarios cuando cambia otro contrato, así que después correr `./gradlew :servicios:usuarios:test --rerun`.
- [ ] T003 Agregar a `contratos/comun/errores.md`, con sus 8 columnas (research §11), estos seis códigos, todos 409:
  - `VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION`;
  - `VALIDACIONOCS_PARCIALES_PENDIENTES`;
  - `VALIDACIONOCS_SIN_CONSOLIDAR`;
  - `VALIDACIONOCS_CONSOLIDADO_DESACTUALIZADO`;
  - `VALIDACIONOCS_SIN_INFORME`;
  - `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS`.

  Crear sus excepciones en `VAL/main/.../dominio/`. `InconsistenciasAbiertas` lleva `duras` y `blandas`. Agregar sus entradas en `VAL/main/.../adaptadores/entrada/ErroresValidacionocs.java`; la de `INCONSISTENCIAS_ABIERTAS` va con `EntradaCatalogo.conDetalles`, con el campo `inconsistencias` y el texto «Quedan N: D duras y B blandas sin justificar.» (singular y plural correctos). Escribir primero su prueba en `ErroresValidacionocsTest`.
- [ ] T004 [P] En Generación, agregar a `GEN/main/.../puertos/entrada/ParcialesDelDistributivo.java` el método `condicionesDeRevision(periodo)`, y crear el registro `CondicionesDeRevision` (`version`, `consolidado`, `desactualizado`, `informe: Optional<String>`, `duras`, `blandas`), con tipos simples (`contracts/generacion.md`). Ver fallar primero `GEN/test/.../aplicacion/ParcialesDelDistributivoServicioTest.java` (Mockito):
  - con un `ConsolidadoDelPeriodo` de prueba, cada campo sale del consolidado: sin consolidar, desactualizado, con informe (su huella) y las abiertas del panel, cada una en los dos sentidos;
  - sin distributivo, `DistributivoNoEncontrado`;
  - solo cuenta `abiertas`: las reglas de `sinEvaluar` no bloquean (Edge Cases);
  - no guarda nada.

  Después, en un comando aparte, implementarlo en `ParcialesDelDistributivoServicio`, que recibe `ConsultarConsolidado`, y conectarlo en la configuración de Generación.

**Checkpoint**: el contrato compila, los errores pasan su prueba y Generación entrega las condiciones.

---

## Phase 3: User Story 1 - Enviar el consolidado a revisión (Priority: P1) 🎯 MVP

**Goal**: con todo cumplido, el Responsable envía y el distributivo pasa a la siguiente etapa activa, con una entrada en la bitácora.

**Independent Test**: en la prueba de integración, con condiciones cumplidas, el distributivo queda en EN_REVISION_VRA con una transición y una entrada `CONSOLIDADO_ENVIADO_A_REVISION` (research §10).

- [ ] T005 [US1] Crear `VAL/main/.../dominio/CondicionesDeRevision.java` (data-model.md: `consolidado`, `desactualizado`, `informe` opcional, `duras ≥ 0`, `blandas ≥ 0`). Agregar a `VAL/test/.../dominio/ProcesoAprobacionTest.java` y ver fallar:
  - `validarRevision` exige `estado == EN_CONSOLIDACION` (`EtapaNoAdmiteRevision`) y todos los parciales `ENVIADO` (`ParcialesPendientes`), y no cambia nada;
  - `enviarARevision` con todo cumplido deja un `PASO_DE_ETAPA` de EN_CONSOLIDACION a EN_REVISION_VRA, con la versión, el hash, quién y cuándo;
  - con EN_REVISION_VRA desactivada en la copia de la configuración, pasa a la siguiente activa (US1-3);
  - un segundo envío, ya en EN_REVISION_VRA, lanza `EtapaNoAdmiteRevision` sin agregar transiciones (US1-4).

  Después, en un comando aparte, `validarRevision` y `enviarARevision` en `ProcesoAprobacion`.
- [ ] T006 [US1] Crear `VAL/main/.../puertos/entrada/EnviarARevision.java`, y agregar `condicionesDeRevision(periodo)` a `VAL/main/.../puertos/salida/Distributivo.java`. Agregar a `VAL/test/.../aplicacion/FlujoDelDistributivoServicioTest.java` y ver fallar:
  - el orden de research §2: periodo abierto, `validarRevision`, `condicionesDeRevision` fuera de la transacción y, en la transacción con el proceso bloqueado, `enviarARevision`;
  - una sola entrada en `RegistroDeCambios`, con la acción `CONSOLIDADO_ENVIADO_A_REVISION`; en el detalle, `periodo`, `version`, `hash` e `informe`; en `anterior` y `nuevo`, el estado (`contracts/bitacora.md`);
  - si `validarRevision` rechaza, no llama a Generación;
  - un periodo cerrado responde `PeriodoCerrado`;
  - un rechazo no registra nada;
  - si `condicionesDeRevision` falla porque Datos no responde, el error del proceso se propaga (502 o 504 del catálogo), sin transición ni entrada (Edge Cases);
  - `condicionesDeRevision` se llama en cada envío: no se reutiliza lo que la pantalla vio antes (FR-008, US2-3).

  Después, en un comando aparte:
  - `enviarARevision` en `FlujoDelDistributivoServicio`;
  - la acción `CONSOLIDADO_ENVIADO_A_REVISION` en `CambioDeFlujo`, que `cambio()` elige para un paso desde EN_CONSOLIDACION y que lleva la huella del informe;
  - el método en `VAL/main/.../adaptadores/salida/DistributivoDesdeGeneracion.java`.
- [ ] T007 [US1] Agregar a `VAL/test/.../adaptadores/entrada/FlujoContratoTest.java` y ver fallar: el 200 de `enviarARevision` cumple `EstadoDelFlujo` con la etapa nueva; el 400 de un `periodo` inválido; el 404 sin distributivo. Después, la operación en `VAL/main/.../adaptadores/entrada/FlujoControlador.java`, con `RolDeOtraEtapa` traducido a `AccessDeniedException`, y el caso de uso en `VAL/main/.../infraestructura/ConfiguracionValidacionocs.java`. Sumar `enviarARevision` a `PermisosValidacionocsTest`.
- [ ] T008 [US1] Crear `VAL/integrationTest/.../EnvioARevisionIntegrationTest.java` con PostgreSQL real. Usa `FlujoDelDistributivoServicio`, `RepositorioProcesoJdbc` y la transacción reales, y un `Distributivo` falso: proceso en EN_CONSOLIDACION, condiciones cumplidas. Comprueba:
  - el envío deja el proceso en EN_REVISION_VRA, con su transición guardada en `validacionocs.transicion`;
  - un `RegistroDeCambios` falso, que cuenta lo que recibe, recibe una sola entrada (la entrega a la bitácora ya la prueba F17-H01);
  - con dos hilos que envían a la vez, un solo `PASO_DE_ETAPA` desde EN_CONSOLIDACION, y el otro recibe `EtapaNoAdmiteRevision` (SC-004).

**Checkpoint**: el envío que pasa funciona por la API, con su entrada en la bitácora.

---

## Phase 4: User Story 2 - No enviar con inconsistencias abiertas (Priority: P1)

**Goal**: el 409 con el número de abiertas, duras y blandas, igual al del panel.

**Independent Test**: con el panel de los datos de prueba, el 409 dice el mismo número que `abiertas` del panel; al justificar una blanda, baja en uno (`probar_revision.sh`, pasos 5 y 6).

- [ ] T009 [US2] Agregar a `ProcesoAprobacionTest` y ver fallar:
  - una dura abierta lanza `InconsistenciasAbiertas(1, 0)`;
  - 2 duras y 1 blanda, `InconsistenciasAbiertas(2, 1)`;
  - 0 duras y 0 blandas, con blandas ya justificadas (que no cuentan, MD-07), pasa;
  - un rechazo no cambia el estado ni agrega transiciones.

  Después, en un comando aparte, completar `enviarARevision`.
- [ ] T010 [US2] Agregar a `FlujoContratoTest` el 409 `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS`, con el detalle `inconsistencias` y su texto en singular y en plural.

**Checkpoint**: los dos escenarios del issue funcionan por la API.

---

## Phase 5: User Story 3 - No enviar sin consolidar ni sin informe (Priority: P1)

**Goal**: cada falta del consolidado tiene su 409, en el orden de research §8.

**Independent Test**: sin consolidar, 409 `SIN_CONSOLIDAR`; al consolidar, 409 `SIN_INFORME`; al adjuntar el informe, solo quedan las inconsistencias (`probar_revision.sh`, pasos 3 a 5).

- [ ] T011 [US3] Agregar a `ProcesoAprobacionTest` y ver fallar, una por condición, con las demás cumplidas:
  - `SinConsolidar`;
  - `ConsolidadoDesactualizado`;
  - `SinInforme`;
  - con todas fallando, el error es `ParcialesPendientes` si hay un parcial pendiente, y si no, `SinConsolidar` (el orden de research §8).

  Después, en un comando aparte, completar `enviarARevision`.
- [ ] T012 [US3] Agregar a `FlujoContratoTest` los 409 `VALIDACIONOCS_PARCIALES_PENDIENTES`, `SIN_CONSOLIDAR`, `CONSOLIDADO_DESACTUALIZADO`, `SIN_INFORME`, `ETAPA_NO_ADMITE_REVISION` y `PERIODO_CERRADO`, cada uno con su código y su estado.

**Checkpoint**: todas las condiciones de D-97 se comprueban.

---

## Phase 6: User Story 4 - Solo el Responsable de consolidación envía (Priority: P1)

**Goal**: el 403 con cualquier otro rol, y la acción visible solo para el rol de la etapa.

**Independent Test**: los 9 roles que no son el de EN_CONSOLIDACION reciben 403 con el mismo cuerpo; `consultarFlujo` muestra `ENVIAR_A_REVISION` solo al Responsable.

- [ ] T013 [US4] Agregar a `ProcesoAprobacionTest` y ver fallar:
  - `validarRevision` y `enviarARevision` exigen el rol de EN_CONSOLIDACION de la copia de la configuración (`RolDeOtraEtapa`), también con otro rol configurado para esa etapa;
  - `acciones` incluye `ENVIAR_A_REVISION` en EN_CONSOLIDACION con su rol, aunque falten condiciones, y no la incluye con otro rol ni en otra etapa (FR-013).

  Después, en un comando aparte, `AccionDelProceso.ENVIAR_A_REVISION` y `acciones`.
- [ ] T014 [US4] Agregar a la tabla de rol, operación y resultado de `FlujoContratoTest` la operación `enviarARevision`, en la etapa EN_CONSOLIDACION. Comprueba el 403 de cada uno de los 9 roles de la matriz D-16 que no son el Responsable de consolidación, con los permisos de `V2__matriz_d16.sql`, el mismo cuerpo en todos y sin decir qué falta, y el 200 del Responsable (SC-003).

**Checkpoint**: las cuatro historias funcionan por la API.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T015 [P] En la SPA, según `contracts/spa.md`, escribir primero las pruebas.
  - **`SPA/pantallas/comun/EstadoDelFlujo.test.tsx`:**
    - el botón «Enviar a revisión» solo con `ENVIAR_A_REVISION`;
    - con `faltan`, el botón deshabilitado y la lista con `aria-describedby`;
    - la confirmación;
    - el 409 con su detalle.
  - **`SPA/pantallas/consolidado/Consolidado.test.tsx`:** cada condición da su texto, también «Hay parciales sin enviar» con el flujo, la de las inconsistencias con el enlace al panel; con todo cumplido, el botón habilitado; al enviar, se invalidan el flujo y el consolidado.

  Después, el código de `EstadoDelFlujo.tsx` y `Consolidado.tsx`. Agregar a `frontend/e2e/consolidado.spec.ts` lo que falta y el envío exitoso con la API simulada, con axe-core.
- [ ] T016 Crear `deploy/probar_revision.sh` con los 7 pasos de `contracts/demostracion.md`, sobre los datos de prueba y con la base limpia al terminar, sin imprimir claves ni tokens. Agregar su paso a `.github/workflows/perfil-consolidado.yml` después de «Avance por etapas». Pasar shellcheck. Correr `probar_avance.sh` y `probar_revision.sh` en su orden sobre un perfil nuevo.
- [ ] T017 Revisar el JavaDoc de `validarRevision`, `enviarARevision`, `CondicionesDeRevision` (en los dos servicios), `ParcialesDelDistributivo.condicionesDeRevision` y `FlujoDelDistributivoServicio.enviarARevision` (principio XII). El de `enviarARevision` dice que no notifica (D-97).
- [ ] T018 Crear `specs/042-enviar-revision/evidencia.md`, como `specs/041-avanzar-etapas/evidencia.md`:
  - las pruebas unitarias e `integrationTest`, completas;
  - la cobertura por módulo (60 %, D-32), Spotless y SpotBugs;
  - la SPA: tipos, ESLint, Prettier, Vitest y Playwright;
  - los scripts de `perfil-consolidado` en su orden sobre un perfil nuevo, shellcheck y Gitleaks;
  - los criterios de terminado de la constitución §6;
  - la limitación de research §10: el envío que pasa se prueba solo en integración;
  - SC-005 pendiente hasta F12-H05 (#211), con el motivo.
- [ ] T019 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración: GitHub `demostracion` y los 8 contenedores sanos con la versión nueva. Hacer la comprobación del autor con `quickstart.md` §4, que incluye ver lo que falta y el 409 con el número del panel. SC-005 no se cronometra: con los datos de la demo no hay un consolidado listo; queda pendiente en la evidencia y en un comentario de #211. Abrir el PR de evidencia con `Closes #55` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 antes del código.
- **Fundacional:** T002 y T003 antes del código. T004 no depende de T002 ni de T003.
- **US1:** T005 depende de T003. T006 depende de T004 y T005. T007 depende de T002 y T006. T008 depende de T006.
- **US2:** T009 depende de T005. T010 depende de T007 y T009.
- **US3:** T011 depende de T005. T012 depende de T007 y T011.
- **US4:** T013 depende de T005. T014 depende de T007 y T013.
- **Cierre:** T015 depende de T002 y puede empezar con la API simulada. T016 depende de US1 a US4. T017 a T019, al final.

## Parallel Example

```text
Después de T003: T004 (Generación) y T005 (Aprobación) a la vez.
Después de T007: US2, US3 y US4 en cualquier orden, porque las tres agregan casos a ProcesoAprobacionTest y FlujoContratoTest (mismo archivo, así que una a la vez); T015 (la SPA) en paralelo.
```

## Implementation Strategy

- **MVP:** las fases 1 y 2 y US1 (T005 a T008): el envío que pasa, con su bitácora y su concurrencia.
- **Después:** US2 y US3 (las condiciones) y US4 (el rol). Las tres son casos del mismo agregado y del mismo controlador.
- **Luego:** la pantalla, el script y el cierre.
