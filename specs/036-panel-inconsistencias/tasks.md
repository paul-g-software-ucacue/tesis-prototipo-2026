---

description: "Tareas de F10-H01: ver el panel de inconsistencias globales"
---

# Tasks: Ver el panel de inconsistencias globales

**Input**: documentos de diseño en `specs/036-panel-inconsistencias/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las 7 aclaraciones del autor del 2026-11-02 (en la spec);
- D-86 y D-87, aprobadas por el autor el 2026-11-02 y por escribir en T001;
- los términos «Inconsistencia global», «Margen de la proporción» y «Justificación de una inconsistencia», aprobados por el autor el 2026-11-02 y por escribir en T001;
- D-13, D-68, D-72, D-73, D-74, D-83, D-85, ADR-010 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-24, RF-29, RN-02, RN-05, RN-10, RN-12, RN-17, RN-18 y MD-07.

Plan aprobado por el autor el 2026-11-02.

**Organization**:
- **Fundacional:** las decisiones, los contratos, los errores, los datos de los otros procesos y el motor.
- **US1, ver; US2, la acción; US3, justificar; US4, el 60/40; US5, el Vicerrectorado.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US5.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `SPA` = `frontend/src/pantallas`;
  - dentro de cada servicio, `main` y `test` llevan `java/ec/mdd/<servicio>`.

---

## Phase 1: Setup

- [X] T001 Escribir en `docs/decisiones.md`, con sus filas en la tabla, D-86 (el margen de 60/40 es un dato de la normativa, con 5 puntos por omisión, y RN-05 revisa la planificación y la evaluación) y D-87 (RN-18 se mide por paralelo en el panel, y la matriz D-16 no cambia: el Administrador lee el panel). Agregar «Inconsistencia global», «Margen de la proporción» y «Justificación de una inconsistencia» a `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §7.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: los contratos, los datos y el motor.

**⚠️ CRITICAL**: el motor se escribe con sus pruebas primero, y se ve fallar antes de su código.

- [X] T002 Actualizar los contratos según `contracts/`: `contratos/generacion/openapi.yaml` (las 2 operaciones, con `x-mdd-permiso`), `contratos/normativa/openapi.yaml` (`reglas-del-panel`), `contratos/docentes/openapi.yaml` (`cargos-vigentes`) y `contratos/academico/openapi.yaml` (`horasDeLaAsignatura`). Generar los modelos.
- [X] T003 Agregar `GENERACION_INCONSISTENCIA_NO_ENCONTRADA` y `GENERACION_INCONSISTENCIA_DURA` a `contratos/comun/errores.md`, con sus 8 columnas, y a `GEN/main/adaptadores/entrada/ErroresGeneracion.java`, con sus excepciones (FR-029). Actualizar `ErroresGeneracionTest`.
- [X] T004 [P] Crear `NOR/main/resources/db/migration/normativa/V6__margen_y_actividades_obligatorias.sql`: `margen_puntos` 5 en RN-05 y `obligatoria` en `140-b` y `140-g`, en cada versión y con su registro (FR-015, D-68). Copiar `obligatoria` en `RepositorioVersionesJdbc`, y probarlo con una versión copiada. Actualizar `PoliticaDe2024IntegrationTest`. Agregar «Margen (puntos)» a `SPA/normativa/reglas.ts`.
- [X] T005 [P] Agregar a Normativa el caso de uso `reglas-del-panel` y su ruta interna, con pruebas primero: las severidades, la meta, el margen, el rango de Tipo 2 y las actividades obligatorias salen de la versión; sin alguno, falla sin inventarlo (principio VI).
- [X] T006 [P] Agregar a Docentes la ruta interna `cargos-vigentes`, con pruebas primero: solo vigentes, con la carrera si la tiene, y sin datos personales.
- [X] T007 [P] Agregar `horasDeLaAsignatura` a la ruta interna de la oferta en Académico, con pruebas primero: las horas actuales, aunque el paralelo tenga las suyas.
- [X] T008 Agregar el puerto `GEN/main/puertos/salida/DatosParaElPanel.java` y su implementación en `DatosDesdeProcesoDatos`, con pruebas primero en `DatosDesdeProcesoDatosTest` (`ReceptorSimulado`). Agregar `horasDeLaAsignatura` a `AsignaturaParalelo`, conservando los constructores cortos, que la igualan a sus horas: así no cambian sus 21 usos.
- [X] T009 Crear los tipos del dominio en `GEN/main/dominio/`, con esqueleto del motor: `TipoDeInconsistencia`, `InconsistenciaGlobal`, `ObjetoAfectado`, `ReglasDelPanel` y `CargoVigente`, con `MotorReglas.evaluarVersion` devolviendo una lista vacía. `ReglasDelPanel` no se construye sin una severidad o un parámetro.

**Checkpoint**: los datos llegan, y el motor compila vacío.

---

## Phase 3: User Story 1 - Ver lo que falta por resolver (Priority: P1) 🎯 MVP

**Goal**: el panel, con las comprobaciones de RF-29 y RN-18.

**Independent Test**: una versión con un docente sin clase, un paralelo sin propuesta y dos coordinadores.

- [X] T010 [US1] Agregar a `GEN/test/dominio/EvaluarVersionTest.java`, nuevo, y verlas fallar: RN-17 (FR-007), con licencia e inactivos fuera y sin contar paralelos retirados; RF-24 (FR-008), sin retirados; RN-12 con 0, 1 y 2 coordinadores (FR-009), solo en carreras con oferta; RN-10 en los dos límites y fuera, contando solo G06 a G21 (FR-010); RN-18 agrupada por carrera (FR-011); sobrecarga en todas las carreras (FR-012), con el total justo en el límite sin inconsistencia; la severidad de la versión (FR-006).
- [X] T011 [US1] Implementar esas comprobaciones en `GEN/main/dominio/MotorReglas.java`, en un comando aparte.
- [X] T012 [US1] Crear `GEN/main/resources/db/migration/generacion/V5__justificacion_inconsistencia.sql`, con la tabla solo de inserción, `accion` en `varchar(30)` y `INCONSISTENCIA_JUSTIFICADA`. Agregar la lectura a `RepositorioDistributivos`, con su prueba de integración.
- [X] T013 [US1] Crear `GEN/test/aplicacion/ConsultarPanelServicioTest.java`, con esqueleto, y verlo fallar: evalúa al abrir, sin guardar (FR-001); combina el motor, las justificaciones y las excepciones de RN-19 (FR-016); ordena primero las duras y cuenta las abiertas (FR-003); sin distributivo responde vacío (FR-028); un periodo cerrado se lee; una regla sin dato falla (edge case). Después crear el caso de uso en `GEN/main/puertos/entrada/` y `GEN/main/aplicacion/ConsultarPanelServicio.java`.
- [X] T014 [US1] Crear `GEN/main/adaptadores/entrada/PanelControlador.java` con el GET, sus datos por inconsistencia (FR-002), sus `@PreAuthorize` y la lista `sinEvaluar` (FR-005), con pruebas primero en un `PanelContratoTest` nuevo. Actualizar `PermisosGeneracionTest`, de 12 a 14.
- [X] T015 [US1] Crear `SPA/inconsistencias/consultas.ts` e `Inconsistencias.tsx`, con la ruta en `frontend/src/rutas.tsx` y la entrada en `frontend/src/navegacion/Marco.tsx`. Escribir primero `Inconsistencias.test.tsx`: los totales, el orden, las justificadas aparte (FR-004), sin ninguna abierta, las reglas sin evaluar (FR-005), el aviso sin distributivo, y un error que no se presenta como panel vacío (FR-030).

**Checkpoint**: se ve el panel.

---

## Phase 4: User Story 2 - Saber cómo corregir cada una (Priority: P1)

**Goal**: la indicación y el enlace.

**Independent Test**: un paralelo sin propuesta lleva a la oferta.

- [X] T016 [US2] Agregar a `PanelContratoTest`, y verlo fallar: cada tipo da su detalle, su indicación y su pantalla (FR-017), según la tabla de `data-model.md` y sin lenguaje técnico. Después implementar en `PanelControlador`, sin cambiar datos (FR-019).
- [X] T017 [US2] Escribir primero en `Inconsistencias.test.tsx`: cada pantalla lleva a su ruta, y sin permiso sale la indicación sin enlace (FR-018). Después implementarlo.

**Checkpoint**: cada inconsistencia dice qué hacer.

---

## Phase 5: User Story 3 - Justificar una blanda (Priority: P1)

**Goal**: una blanda deja de estar abierta.

**Independent Test**: justificar una blanda y verla justificada.

- [X] T018 [US3] Crear `GEN/test/aplicacion/JustificarInconsistenciaServicioTest.java`, con esqueleto, y verlo fallar: una blanda se guarda y va al registro, sin el texto; cambiarla agrega una fila (FR-023); una dura se rechaza (FR-021); una que ya no existe se rechaza; el periodo cerrado se rechaza (FR-024); RN-19 no se acepta (FR-025); una justificación guardada no cierra una dura; el texto va sin espacios en los extremos y con 10 a 500 caracteres (FR-020).
- [X] T019 [US3] Crear el caso de uso y `GEN/main/aplicacion/JustificarInconsistenciaServicio.java`, y la escritura en el repositorio, en un comando aparte. La evaluación va fuera de la transacción (D-83). Agregar la prueba de integración de la inserción, de la última vigente, con dos a la vez, y de que vale para esa versión (FR-022).
- [X] T020 [US3] Agregar el PUT a `PanelControlador`, con pruebas primero en `PanelContratoTest`: el 200, el 400 del campo `texto` y los errores del catálogo.
- [X] T021 [US3] Escribir primero en `Inconsistencias.test.tsx`: justificar una blanda; una dura no lo ofrece, y en un periodo cerrado no sale «Justificar» en ninguna (FR-024); el texto se valida en la interfaz; un error deja lo escrito; cambiar una justificación. Después crear el diálogo con `SPA/inconsistencias/`, reutilizando `Marco`, `Avisos` y `useEnvio`.

**Checkpoint**: las blandas se justifican.

---

## Phase 6: User Story 4 - El 60/40 de cada docente (Priority: P2)

**Goal**: RN-05 en el panel.

**Independent Test**: con meta 60 y margen 5, 65 % cumple y 66 % no.

- [X] T022 [P] [US4] Agregar a `EvaluarVersionTest`, y verlas fallar: 55 y 65 % cumplen, 54 y 66 % no (FR-013); otra meta y otro margen; sin horas de docencia no aparece; faltan una o las dos actividades obligatorias, y son dos inconsistencias aparte de la proporción (FR-014); un docente sin clase no entra en la de actividades.
- [X] T023 [US4] Implementar RN-05 en `MotorReglas`, en un comando aparte, y agregar su detalle y su indicación en `PanelControlador` (con T016).

**Checkpoint**: están las siete comprobaciones.

---

## Phase 7: User Story 5 - El Vicerrectorado sigue el estado (Priority: P2)

**Goal**: leer, sin escribir.

**Independent Test**: entrar como Vicerrector.

- [X] T024 [US5] Agregar a `PanelContratoTest` los permisos (FR-026, FR-027): Consolidación, Vicerrectorado y Administrador leen; un coordinador, 403; solo Consolidación justifica.
- [X] T025 [US5] Agregar a `Inconsistencias.test.tsx`: sin permiso de escritura no sale «Justificar»; el menú solo aparece a quien puede leer. Implementarlo con `frontend/src/sesion/funcionalidades.ts`.

**Checkpoint**: la historia está completa.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T026 [P] Agregar `frontend/e2e/inconsistencias.spec.ts`, con la API simulada: abrir, seguir un enlace, justificar y axe en cada estado.
- [X] T027 Crear `deploy/probar_panel.sh` y agregarlo a `.github/workflows/perfil-consolidado.yml`, según `quickstart.md`, con la base limpia al terminar. Revisar que los demás scripts sigan limpiando con las tablas nuevas.
- [X] T028 Comprobar que el motor tiene 80 % o más en JaCoCo (SC-007, RNF-MAN-02).
- [X] T029 Crear `specs/036-panel-inconsistencias/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` completo;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck con una copia nueva en el scratchpad y Gitleaks en modo `git` sobre un clon, con `.gitleaks.toml`;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #53`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [ ] T030 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Comprobación del autor con SC-004, medida con cronómetro. PR de evidencia con `Closes #53`, y borrar las ramas.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 y T003 antes del código. T004 a T007 no dependen entre sí. T008 depende de T005 a T007, y T009 de T008.
- **US1:** depende de la fase 2.
- **US2:** depende de T014 y de T015.
- **US3:** depende de T012 y de T014.
- **US4:** depende de T011.
- **US5:** depende de T015 y de T020.
- **Cierre:** al final.

## Parallel Example

```text
Después de T003: T004 (migración), T005 (Normativa), T006 (Docentes) y T007 (Académico) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 3 dan el primer escenario del issue, y RN-18 y RN-10 sus dos siguientes.
- **Después:** la acción, justificar, el 60/40 y los permisos. Cada fase deja el sistema en verde.
