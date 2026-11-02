---

description: "Tareas de F07-H02: impedir una asignación que viola una regla dura"
---

# Tasks: Impedir una asignación que viola una regla dura

**Input**: documentos de diseño en `specs/034-reglas-duras/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md), [contracts/normativa.md](contracts/normativa.md), [contracts/docentes.md](contracts/docentes.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, no la implementación.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-11-01 (en la spec);
- D-83, aprobada por el autor el 2026-11-01 y por escribir en T001;
- el término «Límite del docente», aprobado por el autor el 2026-11-01 y por escribir en T001;
- D-01, D-72, D-74, D-75, D-77, D-78, ADR-010 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-28, RN-02, RN-13, RN-25 y MD-01.

Plan aprobado por el autor el 2026-11-01.

**Organization**:
- **Fundacional:** la decisión, los contratos, los errores, las rutas internas y el motor.
- **US1, las horas; US2, el tope de clase; US3, a la vez; US4, la sugerencia; US5, un docente excedido; US6, sin proyecto.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US6.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `SPA` = `frontend/src/pantallas`;
  - dentro de cada servicio, `main` y `test` llevan `java/ec/mdd/<servicio>`.

---

## Phase 1: Setup

- [X] T001 Escribir D-83 en `docs/decisiones.md`, con su fila en la tabla: el agregado de MD-01 se protege bloqueando la versión al guardar, en lugar de un bloqueo optimista, para que el segundo coordinador reciba el error de horas. Cierra el aviso de D-78: desde esta historia ya no se puede guardar sin horas. En `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md`: agregar «Límite del docente» (`limite()`) y aclarar en `MotorReglas` que evalúa comparando la carga antes y después, y en MD-01 que D-83 precisa el bloqueo.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: los contratos, las cifras y el motor, que todas las historias usan.

**⚠️ CRITICAL**: el motor se escribe con sus pruebas primero, y se ve fallar antes de su código.

- [X] T002 Actualizar `contratos/normativa/openapi.yaml` con `consultarReglasDeAsignacion`, `contratos/docentes/openapi.yaml` con `consultarDocentesConProyecto`, `contratos/generacion/openapi.yaml` con `Candidato.motivoParaNoProponer`, sin `ActividadGuardada.horasSuficientes` y con los dos códigos en los 409 de `proponerDocente` y `asignarActividad`, y `contratos/comun/errores.yaml` para que `detalles` no sea solo de errores de entrada, según [contracts/](contracts/). Quitar del contrato las frases que dicen que se guarda sin horas (D-78). Generar los modelos.
- [X] T003 Agregar `GENERACION_HORAS_EXCEDIDAS` y `GENERACION_TOPE_DE_CLASE` a `contratos/comun/errores.md`, con sus 8 columnas, y a `GEN/main/adaptadores/entrada/ErroresGeneracion.java`, con el detalle `horas` (FR-018, FR-019). Título del primero: «Horas laborales excedidas». `VerificadorDeCatalogo` debe pasar.
- [X] T004 [P] Escribir la prueba del servicio y la de contrato de Normativa: las dos cifras de la versión, otra versión con otras cifras, 404 sin versión, 403 con token de usuario, y fallo si falta una cifra (FR-008, FR-025). Con esqueleto, verla fallar.
- [X] T005 Implementar en `NOR/main/` `consultarReglasDeAsignacion`, leyendo `horas_maximas_docente` y `horas_clase_maximo` de la fila `DOCENTE_INVESTIGACION_VINCULACION`, hasta pasar T004.
- [X] T006 [P] Escribir la prueba del servicio y la de contrato de Docentes: los tipos de los proyectos asignables por docente en el periodo, sin contacto, sin proyectos no asignables; 403 con token de usuario. Con esqueleto, verla fallar.
- [X] T007 Implementar en `DOC/main/` `consultarDocentesConProyecto` hasta pasar T006.
- [X] T008 [P] Escribir `GEN/test/dominio/MotorReglasTest.java`, sin dobles, con esqueleto, y verla fallar. Casos: RN-02 con 38 + 4 de 40; justo en el límite; contratadas menores que el tope; tope menor que contratadas; dos decimales; RN-13 con y sin proyecto; 16 h exactas; otras de docencia no cuentan; las dos reglas: gana RN-02; excedido que reduce, que repite y que aumenta; las horas que quedan nunca bajan de 0; `conProyecto` con cada tipo.
- [X] T009 Crear `GEN/main/dominio/ReglasDelPeriodo.java`, `Inconsistencia.java` y `MotorReglas.java` hasta pasar T008.
- [X] T010 [P] Agregar a las pruebas de `VersionDistributivo` `conPropuesta` y `conActividad`: agrega, reemplaza y no toca otras. Con esqueleto, verla fallar. Después implementar.
- [X] T011 [P] Agregar a las pruebas del cliente, con `ReceptorSimulado`, las dos rutas internas. Con esqueleto, verla fallar. Después agregar `reglas(periodo)` y `docentesConProyecto(periodo)` a los puertos y a `GEN/main/adaptadores/salida/DatosDesdeProcesoDatos.java`.

**Checkpoint**: el motor está probado sin dobles. Las historias pueden empezar.

---

## Phase 3: User Story 1 - No pasarse de las horas del docente (Priority: P1) 🎯 MVP

**Goal**: una asignación que no cabe no se guarda.

**Independent Test**: con un docente al que le quedan 2 h, proponerlo para un paralelo de 4 h.

- [X] T012 [US1] Actualizar `ProponerDocenteServicioTest` y `AsignarActividadServicioTest`: sin horas, `HorasExcedidas` con las que quedan y nada guardado (FR-001, FR-026); justo en el límite se guarda; reemplazar cuenta las nuevas; sin reglas o sin proyectos no se guarda (FR-024); en una carrera sin parcial, un rechazo no crea el parcial (FR-026). Quitar las pruebas del aviso de D-78. Verla fallar.
- [X] T013 [US1] Crear `GEN/main/puertos/entrada/HorasExcedidas.java` y agregar a `ProponerDocenteServicio` y `AsignarActividadServicio` la consulta de reglas y proyectos antes de la transacción, y el motor dentro, hasta pasar T012. Quitar `horasSuficientes` de `ActividadGuardada`.
- [X] T014 [US1] Actualizar `PropuestaContratoTest` y `ActividadesContratoTest` con los dos códigos, y el controlador de actividades.
- [X] T015 [US1] Escribir primero en `SPA/actividades/Actividades.test.tsx` y `SPA/oferta/Oferta.test.tsx`: el error se ve, con las horas que quedan, y el formulario sigue abierto (FR-022); ya no hay aviso de horas (FR-023). Después quitar el aviso de `SPA/actividades/Actividades.tsx` y `DialogoAsignar.tsx`, y mostrar `detalles` donde falte.

**Checkpoint**: los dos primeros escenarios del issue funcionan.

---

## Phase 4: User Story 2 - El tope de 16 h con proyecto (Priority: P1)

**Goal**: quien tiene proyecto de investigación o vinculación no supera las 16 h de clase.

**Independent Test**: con un proyecto de investigación y 14 h de clase, proponer 4 h.

- [X] T016 [US2] Agregar a `ProponerDocenteServicioTest`: con proyecto de investigación y de vinculación, `TopeDeClase`; con prácticas y servicio comunitario, se guarda; otra cifra en la normativa (FR-007 a FR-010). Agregar a `AsignarActividadServicioTest`: otras de docencia se guardan con el tope alcanzado. Verla fallar.
- [X] T017 [US2] Crear `GEN/main/puertos/entrada/TopeDeClase.java` y traducir la inconsistencia en los dos servicios, hasta pasar T016.

**Checkpoint**: el tercer escenario del issue funciona.

---

## Phase 5: User Story 3 - Dos coordinadores a la vez (Priority: P1)

**Goal**: solo se guarda lo que cabe.

**Independent Test**: dos asignaciones a la vez cuya suma no cabe.

- [X] T018 [US3] Escribir en `GEN/integrationTest/` una prueba con PostgreSQL y dos hilos: `bloquearVigente` serializa, y el segundo lee lo que guardó el primero. Con esqueleto sin bloqueo, verla fallar (FR-015, FR-017).
- [X] T019 [US3] Agregar `bloquearVigente(periodo)` a `RepositorioDistributivos` y `GEN/main/adaptadores/salida/RepositorioDistributivosJdbc.java`, con `FOR UPDATE` sobre la versión, hasta pasar T018.
- [X] T020 [US3] Mover el motor adentro de la transacción en los dos servicios, sobre la versión bloqueada, sin llamadas a otros procesos ahí (FR-016). Agregar a sus pruebas que evalúa con la versión releída.

**Checkpoint**: el quinto escenario del issue funciona.

---

## Phase 6: User Story 4 - Saber a quién se le puede proponer (Priority: P2)

**Goal**: la sugerencia dice lo mismo que el guardado.

**Independent Test**: un candidato sin horas sale con su motivo y sin «Proponer».

- [X] T021 [US4] Actualizar `SugerenciaTest` y `ConsultarSugerenciaServicioTest`: `motivoParaNoProponer` con `HORAS` y `TOPE_DE_CLASE`, con el mismo motor; el límite con un tope menor; la lectura de `docentesConProyecto` (FR-020, SC-003); el orden pone primero a quienes se pueden proponer (FR-028); el candidato ya propuesto en ese paralelo no se cuenta dos veces, y un excedido que ya está propuesto ahí no sale con motivo, porque repetir no sube sus horas. Verla fallar.
- [X] T022 [US4] Agregar `limite` y `motivo` a `GEN/main/dominio/Candidato.java`, y cambiar `Sugerencia`, con `orden` usando el motivo, `ConsultarSugerenciaServicio` y `SugerenciaControlador`, hasta pasar T021. Actualizar `SugerenciaContratoTest`.
- [X] T023 [US4] Usar el límite también en `ConsultarCargaHorariaServicio`, con su prueba, para que el panel diga lo mismo.
- [X] T024 [US4] Escribir primero en `SPA/oferta/Oferta.test.tsx`: sin botón «Proponer» y con el motivo en cada caso; con horas, sí se propone (FR-021). Después cambiar `SPA/oferta/SugerenciaDelParalelo.tsx`.

**Checkpoint**: la pantalla evita el intento fallido.

---

## Phase 7: User Story 5 - Corregir a un docente ya excedido (Priority: P2)

**Goal**: se puede bajar, no subir.

**Independent Test**: con un docente excedido, reducir una actividad.

- [X] T025 [US5] Agregar a `AsignarActividadServicioTest` y `ProponerDocenteServicioTest`: un excedido reduce, repite la propuesta y quita; no aumenta ni agrega (FR-011 a FR-013). Ajustar lo que falle. Agregar que no se vuelve a validar lo guardado (FR-014).

---

## Phase 8: User Story 6 - Las horas sin proyecto (Priority: P3)

**Goal**: RN-25 queda probada junto a las demás.

- [X] T026 [US6] Comprobar en `AsignarActividadServicioTest` y en el script que investigación sin proyecto sigue sin guardarse (FR-027).

---

## Phase 9: Polish & Cross-Cutting Concerns

- [X] T027 [P] Agregar `frontend/e2e/reglas.spec.ts`, con la API simulada: el error en el formulario, el candidato sin «Proponer» y axe. Actualizar `frontend/e2e/actividades.spec.ts` y `oferta.spec.ts`.
- [X] T028 Actualizar los scripts que guardan sin horas o leen `horasSuficientes`:
  - `deploy/probar_totales.sh`, paso 5: crear el exceso directamente en la base, porque asignar 40 h ahora da 409;
  - `deploy/probar_sugerencia.sh`: agregar el motivo al cálculo aparte;
  - `deploy/probar_propuestas.sh` y `deploy/probar_actividades.sh`: esperar 409 donde antes se guardaba con aviso, y usar horas que quepan donde el paso solo necesita guardar.
- [X] T029 Crear `deploy/probar_reglas_duras.sh` y agregarlo a `.github/workflows/perfil-consolidado.yml`. Parte de los datos de prueba, con dos coordinadores, y comprueba:
  - las horas: 409 con el código y las horas que quedan, justo al límite se guarda, y en dos carreras frente a una suma aparte (SC-001);
  - el tope con proyecto de investigación, sin efecto con prácticas;
  - 20 pares de intentos a la vez: nunca excedido, y los rechazados con el error de horas (SC-002);
  - dos docentes distintos a la vez se guardan;
  - la sugerencia frente al guardado (SC-003);
  - un docente excedido, creándolo en la base: reduce sí, aumenta no;
  - investigación sin proyecto (FR-027);
  - guardar en menos de 3 s (SC-005);
  - con `datos` detenido, el mensaje en menos de 10 s, sin guardar (SC-006);
  - la base limpia al terminar.
- [X] T033 Agregar a `.github/workflows/perfil-consolidado.yml`, con `if: failure()`, un paso que guarde `docker compose logs` como artefacto, con 7 días de retención, antes de bajar el perfil. No resuelve el fallo intermitente, pero permite diagnosticarlo si se repite. Revisar antes que los registros no lleven secretos, porque el repositorio es público y los artefactos se pueden descargar.
- [X] T034 Corregir el fallo intermitente de `perfil-consolidado`, encontrado al correr los scripts: `deploy/docker/red.security`, `deploy/docker/java.Dockerfile` y D-84.
- [X] T030 Comprobar que el motor tiene 80 % o más en JaCoCo (SC-007, RNF-MAN-02).
- [X] T031 Crear `specs/034-reglas-duras/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` completo;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck y Gitleaks, con copias nuevas en el scratchpad;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #51`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [X] T032 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Antes de la comprobación, indicarle al autor qué probar. Después: la comprobación del autor, con SC-004, y el PR de evidencia con `Closes #51`, que dice qué no se vio en la demo.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 y T003 antes de todo el código. T004-T005, T006-T007, T008-T009, T010 y T011 no dependen entre sí, salvo su orden interno.
- **US1:** depende de la fase 2.
- **US2:** depende de T013.
- **US3:** depende de T013.
- **US4:** depende de T009 y T011.
- **US5:** depende de T013.
- **US6:** depende de T013.
- **Cierre:** al final.

## Parallel Example

```text
Después de T003: T004-T005 (Normativa), T006-T007 (Docentes) y T008-T009 (motor) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 5 dan los cinco escenarios del issue, salvo el de RN-25, que ya funciona.
- **Después:** la sugerencia, el docente excedido y el cierre. Cada fase deja el sistema en verde.
