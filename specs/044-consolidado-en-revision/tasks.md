---

description: "Tareas de #215: el consolidado no cambia durante la revisión"
---

# Tasks: El consolidado no cambia durante la revisión

**Input**: documentos de diseño en `specs/044-consolidado-en-revision/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-08, en la spec;
- D-99, escrita en [`docs/decisiones.md`](../../docs/decisiones.md) el 2026-11-08;
- D-83, D-95, D-97 y MD-04.

**Organization**:
- **Fundacional:** el puerto requerido y su implementación, comprobados contra Spring Modulith; el contrato y el error.
- **US1:** lo enviado a revisión no cambia (MVP).
- **US2:** la pantalla no lo ofrece.
- **US3:** antes de la revisión, todo sigue igual.
- **Cierre:** el script, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>`, e `integrationTest` donde aplica:
  - `GEN` = `backend/servicios/generacion/src`;
  - `VAL` = `backend/servicios/validacionocs/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [ ] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §6 y §7): «Consolidado», «Informe sustentado», «Justificación de una inconsistencia» y las etapas. Confirmar que «Etapa del consolidado» no es un término nuevo del negocio, sino el nombre técnico del puerto. Anotar el resultado aquí.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T004.

- [ ] T002 Comprobar primero el riesgo del plan, con esqueletos:
  - crear `GEN/main/java/ec/mdd/generacion/puertos/requeridos/EtapaDelConsolidado.java`, con `boolean admiteCambios(String periodo, Instant distributivoCreadoEn)`, y su `package-info.java` con `@NamedInterface("requeridos")`;
  - crear en Validación OCS `puertos/entrada/ConsultarEtapaDelConsolidado.java`, `aplicacion/EtapaDelConsolidadoServicio.java` (esqueleto que responde `true`) y `adaptadores/entrada/EtapaParaGeneracion.java`, que implementa el puerto de Generación y delega;
  - registrar los beans en `VAL/main/.../infraestructura/ConfiguracionValidacionocs.java`, e inyectar el puerto en `GEN/main/.../infraestructura/ConfiguracionGeneracion.java`, sin usarlo todavía;
  - en las pruebas de Spring de Generación que arman su configuración, un doble del puerto.

  Correr `:apps:distributivo:test` (`ModulosTest`), ArchUnit y las pruebas de los dos servicios. Si Spring Modulith o ArchUnit lo rechazan, detenerse y anotar el motivo aquí antes de seguir.
- [ ] T003 Agregar a `contratos/generacion/openapi.yaml`, según `contracts/generacion.md`:
  - `enRevision` (booleano, obligatorio) en `ConsolidadoDelPeriodo` y en el panel;
  - `GENERACION_CONSOLIDADO_EN_REVISION` en la descripción del 409 de `consolidar`, `justificarInconsistencia` y `adjuntarInformeSustentado`.

  Regenerar `SPA/api/generado/generacion.d.ts` y compilar `:servicios:generacion`; en los controladores, `enRevision` en falso hasta T006. Después correr `./gradlew :servicios:usuarios:test --rerun` (`BitacoraSinBorradoContratoTest`).
- [ ] T004 Agregar a `contratos/comun/errores.md`, con sus 8 columnas, `GENERACION_CONSOLIDADO_EN_REVISION` (409, «Consolidado en revisión», «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.»). Crear `GEN/main/.../dominio/ConsolidadoEnRevision.java` y su entrada en `ErroresGeneracion`. Escribir primero su prueba en `ErroresGeneracionTest`.

**Checkpoint**: el puerto cruza los módulos sin ciclo, el contrato compila y el error está en el catálogo.

---

## Phase 3: User Story 1 - Lo enviado a revisión no cambia (Priority: P1) 🎯 MVP

**Goal**: desde EN_REVISION_VRA, consolidar, justificar y adjuntar el informe se rechazan con 409 y nada cambia; sin carrera con el envío.

**Independent Test**: con el proceso en EN_REVISION_VRA, las tres operaciones responden 409 `GENERACION_CONSOLIDADO_EN_REVISION`, sin guardar ni registrar nada (`probar_consolidado_en_revision.sh`, paso 4).

- [ ] T005 [P] [US1] En Validación OCS, ver fallar primero:
  - `VAL/test/.../dominio/TipoDeEtapaTest.java`: `admiteCambiosEnElConsolidado()` es verdadera para BORRADOR_CARRERA, BORRADOR_UNIDADES y EN_CONSOLIDACION, y falsa para las otras 6;
  - `VAL/test/.../aplicacion/EtapaDelConsolidadoServicioTest.java`: con el proceso en cada etapa; sin proceso, `true`; con el proceso de otro distributivo (otra `distributivo_creado_en`), `true`.

  Después, en un comando aparte:
  - `TipoDeEtapa.admiteCambiosEnElConsolidado()`;
  - `RepositorioProceso.estadoCompartido(periodo)`, que devuelve el estado y `distributivoCreadoEn`, con su implementación en `RepositorioProcesoJdbc` (`SELECT … FOR SHARE`);
  - el servicio.
- [ ] T006 [US1] En Generación, agregar y ver fallar, con Mockito:
  - en `ConsolidarServicioTest`, `JustificarInconsistenciaServicioTest` e `InformeSustentadoServicioTest`:
    - si el puerto dice que no admite cambios, `ConsolidadoEnRevision`, sin guardar, sin registrar y, en el informe, sin guardar el archivo;
    - el puerto se consulta dentro de la transacción (un `Transaccion` de prueba que anota el orden), con el periodo y la fecha de creación del distributivo;
    - el periodo cerrado sigue respondiendo su error antes (research §4);
  - en las pruebas de `ConsultarConsolidado` y del panel: `enRevision` es lo contrario de lo que responde el puerto.

  Después, en un comando aparte: la comprobación en los tres servicios, `enRevision` en `ConsolidadoDelPeriodo` y en el panel, y su mapeo en los controladores.
- [ ] T007 [US1] Agregar a las pruebas de contrato de Generación (las de consolidado, informe y panel) y ver fallar: con el doble del puerto en «no admite», el 409 `GENERACION_CONSOLIDADO_EN_REVISION` en las tres operaciones, y `enRevision: true` en las dos consultas, que cumplen el esquema.
- [ ] T008 [US1] En `VAL/main/.../aplicacion/FlujoDelDistributivoServicio.java`, tomar el momento del envío a revisión dentro de la transacción bloqueada (research §2). Crear `VAL/integrationTest/.../EtapaDelConsolidadoIntegrationTest.java` con PostgreSQL real:
  - un hilo abre una transacción, llama a `admiteCambios` (que toma el `FOR SHARE`), espera 1 s y confirma;
  - otro envía a revisión al mismo tiempo, con el reloj del sistema;
  - el envío espera, y su transición queda con un momento posterior al de la confirmación del primero (SC-004);
  - otra prueba: con el proceso en EN_REVISION_VRA, `admiteCambios` es `false`; sin proceso, `true`.

**Checkpoint**: el defecto está corregido en la API.

---

## Phase 4: User Story 2 - La pantalla no lo ofrece (Priority: P1)

**Goal**: con `enRevision`, sin las tres acciones y con el aviso; la descarga sigue.

**Independent Test**: Vitest y Playwright de las dos pantallas con `enRevision` verdadero y falso.

- [ ] T009 [US2] Escribir primero las pruebas, según `contracts/spa.md`:
  - `SPA/pantallas/consolidado/Consolidado.test.tsx`: con `enRevision`, sin «Consolidar»/«Volver a consolidar» ni «Adjuntar el informe», con el aviso y con la descarga;
  - `SPA/pantallas/inconsistencias/Inconsistencias.test.tsx`: con `enRevision`, sin «Justificar» y con el aviso.

  Después, el código de `Consolidado.tsx` e `Inconsistencias.tsx`, y en `frontend/e2e/consolidado.spec.ts` el consolidado y el panel en revisión con axe-core.

**Checkpoint**: la pantalla refleja la regla.

---

## Phase 5: User Story 3 - Antes de la revisión, todo sigue igual (Priority: P1)

**Goal**: ninguna regresión en BORRADOR_CARRERA, EN_CONSOLIDACION ni tras una devolución.

**Independent Test**: las pruebas y los scripts de F10-H01, F10-H02, F10-H03 y F12-H05 pasan sin cambios.

- [ ] T010 [US3] Confirmar que las pruebas de Generación de T006 cubren «admite cambios: funciona como hoy» en las tres operaciones, y que `EtapaDelConsolidadoServicioTest` cubre BORRADOR_CARRERA tras una devolución. Correr completos `:servicios:generacion:test`, `:servicios:validacionocs:test` e `integrationTest` de los dos, y `probar_consolidado.sh`, `probar_justificacion.sh`, `probar_panel.sh`, `probar_revision.sh` y `probar_devolucion.sh` sobre un perfil construido desde la rama.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T011 Crear `deploy/probar_consolidado_en_revision.sh` con los 6 pasos de `contracts/demostracion.md`, a partir de `probar_devolucion.sh` (las cargas, la limpieza y las ayudas) y del armado de T020 de F12-H05 (la sugerencia en cada paralelo, prefiriendo docentes sin clase). Sin imprimir claves ni tokens, y con la base limpia al terminar. Agregar su paso a `.github/workflows/perfil-consolidado.yml` después de «Devolver parciales». Pasar shellcheck. Correr los scripts del workflow en su orden sobre un perfil nuevo, y anotar cuánto tarda el script nuevo.
- [ ] T012 Revisar el JavaDoc de `EtapaDelConsolidado`, `ConsultarEtapaDelConsolidado`, `EtapaDelConsolidadoServicio`, `EtapaParaGeneracion`, `ConsolidadoEnRevision`, `TipoDeEtapa.admiteCambiosEnElConsolidado` y las tres operaciones de Generación (principio XII). El de `enviarARevision` explica por qué el momento va dentro de la transacción.
- [ ] T013 Crear `specs/044-consolidado-en-revision/evidencia.md`, como `specs/043-devolver-parcial/evidencia.md`:
  - las pruebas unitarias e `integrationTest`, completas;
  - la cobertura por módulo, Spotless, SpotBugs, ArchUnit y Spring Modulith;
  - la SPA: tipos, ESLint, Prettier, Vitest y Playwright;
  - los scripts de `perfil-consolidado`, shellcheck y Gitleaks;
  - los criterios de terminado de la constitución §6 y las desviaciones.
- [ ] T014 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración. Hacer la comprobación del autor con `quickstart.md` §3 y §4: en la demo, 2026-I sigue en Consolidación y las tres acciones siguen. Abrir el PR de evidencia con `Closes #215`. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001 antes del código.
- **Fundacional:** T002 primero, porque comprueba el riesgo; T003 y T004 después, en cualquier orden.
- **US1:**
  - T005 depende de T002;
  - T006 depende de T002, T003 y T004;
  - T007 depende de T006;
  - T008 depende de T005.
- **US2:** T009 depende de T003 y puede empezar con la API simulada.
- **US3:** T010 depende de US1.
- **Cierre:** T011 depende de US1 y US3; T012 a T014, al final.

## Parallel Example

```text
Después de T004: T005 (Validación OCS) y T006 (Generación) a la vez; T009 (la SPA) en paralelo con la API simulada.
```

## Implementation Strategy

- **MVP:** las fases 1 y 2 y US1 (T005 a T008): el defecto corregido en la API, con el bloqueo.
- **Después:** US2 (la pantalla) y US3 (sin regresiones).
- **Luego:** el script y el cierre.
