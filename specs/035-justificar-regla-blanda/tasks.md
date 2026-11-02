---

description: "Tareas de F07-H03: justificar una asignación con una regla blanda"
---

# Tasks: Justificar una asignación con una regla blanda

**Input**: documentos de diseño en `specs/035-justificar-regla-blanda/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md), [contracts/normativa.md](contracts/normativa.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, no la implementación. Cada paso va en un comando aparte.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-11-01 (en la spec);
- D-85, aprobada por el autor el 2026-11-01 y por escribir en T001;
- el término «Advertencia», aprobado por el autor el 2026-11-01 y por escribir en T001;
- D-72, D-74, D-83, ADR-010 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-28, RN-19, MD-07 y RNF-EQU-01.

Plan aprobado por el autor el 2026-11-01.

**Organization**:
- **Fundacional:** la decisión, los contratos, los errores, la severidad y el motor.
- **US1, la advertencia; US2, la justificación; US3, la severidad; US4, las duras primero.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `SPA` = `frontend/src/pantallas`;
  - dentro de cada servicio, `main` y `test` llevan `java/ec/mdd/<servicio>`.

---

## Phase 1: Setup

- [X] T001 Escribir D-85 en `docs/decisiones.md`, con su fila en la tabla: la proporción 60/40 se justifica en F10-H01, con el panel, porque es global. Agregar la nota a `specs/033-totales-del-docente/spec.md` (FR-023). En `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md`: agregar «Advertencia».

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: los contratos, la severidad y el motor.

**⚠️ CRITICAL**: el motor se escribe con sus pruebas primero, y se ve fallar antes de su código.

- [X] T002 Actualizar `contratos/generacion/openapi.yaml` con `ResultadoDePropuesta`, `Advertencia`, `Propuesta.inconsistencia`, el motivo `AFINIDAD` y el código en el 409 de `proponerDocente`, y `contratos/normativa/openapi.yaml` con `severidadAfinidad`, según [contracts/](contracts/). Actualizar la descripción de `proponerDocente`: sin justificación responde 200 con una advertencia; el 400 queda solo para un texto de largo inválido. Generar los modelos.
- [X] T003 Agregar `GENERACION_AFINIDAD_INSUFICIENTE` a `contratos/comun/errores.md`, con sus 8 columnas, y a `GEN/main/adaptadores/entrada/ErroresGeneracion.java`, con `AfinidadInsuficiente`. Actualizar `ErroresGeneracionTest`.
- [X] T004 [P] Agregar a `ConsultarReglasDeAsignacionServicioTest` y a `InternoNormativaContratoTest` la severidad de RN-19, y que sin ella falla sin inventarla (FR-018). Con esqueleto, verla fallar. Después implementar en `NOR/main/`.
- [X] T005 [P] Agregar a `DatosDesdeProcesoDatosTest` la severidad. Con esqueleto, verla fallar. Después agregar `severidadAfinidad` a `GEN/main/dominio/ReglasDelPeriodo.java` y su lectura.
- [X] T006 [P] Agregar a `GEN/test/dominio/MotorReglasTest.java`, con esqueleto: bajo el mínimo es RN-19 con la severidad de la normativa; alcanza justo el mínimo; tronco común exento, también con severidad dura. Verla fallar. Después agregar `severidad` a `Inconsistencia` y `RN_19` a `ReglaDeAsignacion`, e implementar `evaluarAfinidad`.
- [X] T007 [P] Crear `GEN/test/dominio/PropuestaTest.java`, con `inconsistencia()` con una excepción, y vacía con afinidad y disponibilidad (FR-013, FR-014). Con esqueleto, verla fallar. Después implementar.

**Checkpoint**: el motor y la severidad están probados.

---

## Phase 3: User Story 1 - Recibir una advertencia, no un error (Priority: P1) 🎯 MVP

**Goal**: sin justificación, una advertencia y nada guardado.

**Independent Test**: proponer bajo el mínimo sin texto.

- [X] T008 [US1] Actualizar `ProponerDocenteServicioTest`, con esqueleto: sin texto, o solo con espacios, devuelve la advertencia y no guarda, no crea el parcial ni deja registro (FR-006, FR-008, FR-009); otro mínimo se aplica. Actualizar `JustificacionTest`: `Justificacion.para` deja de recibir un texto vacío, y solo rechaza uno de 1 a 9 o de más de 500 caracteres. Verla fallar.
- [X] T009 [US1] Crear `GEN/main/puertos/entrada/ResultadoDePropuesta.java` y cambiar `ProponerDocente` y `GEN/main/aplicacion/ProponerDocenteServicio.java`, para evaluar RN-19 dentro de la transacción, después de las reglas duras, hasta pasar T008. El servicio detecta el texto vacío o con solo espacios antes de llamar a `Justificacion.para`, y devuelve la advertencia.
- [X] T010 [US1] Actualizar `PropuestaContratoTest` y `GEN/main/adaptadores/entrada/DistributivoControlador.java`: 200 con `guardada` falsa, la advertencia en palabras claras y sin `propuesta` (FR-007). Actualizar `DistributivoContratoTest`, donde el doble de `ProponerDocente` lanza `JustificacionObligatoria`: sigue siendo 400 para un texto corto, y se agrega el caso de la advertencia.
- [X] T011 [US1] Escribir primero en `SPA/oferta/Oferta.test.tsx`: si el servidor pide la justificación, se abre el campo, con un aviso y sin estilo de error, y no se cierra (FR-019, FR-020). Después cambiar `SPA/oferta/consultas.ts` y `SPA/oferta/SugerenciaDelParalelo.tsx`. Mirar `guardada` en todos los puntos que proponen. Actualizar los 12 usos de `Oferta.test.tsx` que simulan el 200 de `proponerDocente`, para que respondan `{ guardada: true, propuesta }`.

**Checkpoint**: el primer escenario del issue funciona.

---

## Phase 4: User Story 2 - Guardar con la justificación (Priority: P1)

**Goal**: con el texto, queda como excepción, justificada.

**Independent Test**: proponer con un texto y ver la propuesta justificada.

- [X] T012 [US2] Agregar a `ProponerDocenteServicioTest`: con texto queda «excepción»; con 1 a 9 o más de 500 caracteres, `JustificacionObligatoria` (FR-010); alcanza el mínimo y no conserva el texto (FR-005); tronco común queda «disponibilidad» (FR-004); el registro no lleva el texto (FR-012). Ajustar lo que falle.
- [X] T013 [US2] Agregar `inconsistencia` a las propuestas que devuelve el contrato, en `DistributivoControlador` y `SugerenciaControlador`, con su prueba de contrato. Una propuesta sin excepción no la trae (FR-014).
- [X] T014 [US2] Escribir primero en `SPA/oferta/Oferta.test.tsx`: la propuesta muestra que está justificada (FR-022); lo escrito sigue si vuelve un error (FR-021). Después cambiar `SPA/oferta/Oferta.tsx`.

**Checkpoint**: los dos escenarios del issue funcionan.

---

## Phase 5: User Story 3 - La normativa decide la severidad (Priority: P2)

**Goal**: dura bloquea.

**Independent Test**: con RN-19 dura, proponer con texto.

- [X] T015 [US3] Agregar a `ProponerDocenteServicioTest`: con RN-19 dura, `AfinidadInsuficiente`, con o sin texto, y nada guardado; tronco común sigue exento (FR-015). Verla fallar. Después implementar.
- [X] T016 [US3] Agregar a `SugerenciaTest` y `ConsultarSugerenciaServicioTest`: con RN-19 dura, motivo `RN_19` para quien está bajo el mínimo; las horas van primero; tronco común sin motivo (FR-017, SC-003). Verla fallar. Después cambiar `Sugerencia.armar` y `SugerenciaControlador`, con `SugerenciaContratoTest`.
- [X] T017 [US3] Escribir primero en `SPA/oferta/Oferta.test.tsx`: el motivo `AFINIDAD`, sin «Proponer». Después cambiar `SPA/oferta/SugerenciaDelParalelo.tsx`.

**Checkpoint**: la severidad de la normativa se respeta.

---

## Phase 6: User Story 4 - Primero las reglas duras (Priority: P2)

**Goal**: no se pide un texto inútil.

- [X] T018 [US4] Agregar a `ProponerDocenteServicioTest`: bajo el mínimo y sin horas, `HorasExcedidas`, con y sin texto; bajo el mínimo y con el tope, `TopeDeClase` (FR-002). Ajustar lo que falle.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T019 [P] Agregar a `frontend/e2e/oferta.spec.ts`, que ya tiene el simulador, la prueba con la API simulada: la advertencia abre el campo, se guarda y axe. El simulador responde `{ guardada: true, propuesta }`.
- [X] T020 Actualizar los scripts que proponen, porque un 200 ya no significa guardado:
  - `deploy/probar_propuestas.sh`, `deploy/probar_actividades.sh`, `deploy/probar_totales.sh` y `deploy/probar_reglas_duras.sh`: mirar `.guardada`, y pasar el texto cuando llegue la advertencia;
  - `deploy/probar_propuestas.sh`: esperar la advertencia donde antes esperaba 400.
- [X] T021 Crear `deploy/probar_justificacion.sh` y agregarlo a `.github/workflows/perfil-consolidado.yml`. Parte de los datos de prueba, con otra versión de normativa, y comprueba:
  - la advertencia, sin guardar ni dejar registro (SC-001);
  - el texto corto, 400;
  - la excepción, justificada;
  - tronco común y quien alcanza el mínimo, sin advertencia;
  - con RN-19 dura, 409 con o sin texto, y el motivo en la sugerencia (SC-002, SC-003);
  - bajo el mínimo y sin horas, el error de horas;
  - guardar en menos de 3 s (SC-005);
  - la base limpia al terminar.
- [X] T022 Comprobar que el motor tiene 80 % o más en JaCoCo (SC-006, RNF-MAN-02).
- [X] T023 Crear `specs/035-justificar-regla-blanda/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` completo;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck y Gitleaks, con copias nuevas en el scratchpad;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #52`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [X] T024 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Antes de la comprobación, indicarle al autor qué probar y cómo ver la advertencia: hace falta una asignatura que no sea de tronco común, porque la de SOCIALES está exenta de RN-19. Después: la comprobación del autor, con SC-004, y el PR de evidencia con `Closes #52`, que dice qué no se vio en la demo.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 y T003 antes del código. T004 a T007 no dependen entre sí, salvo T005 de T004.
- **US1:** depende de la fase 2.
- **US2:** depende de T009.
- **US3:** depende de T009.
- **US4:** depende de T009.
- **Cierre:** al final.

## Parallel Example

```text
Después de T003: T004-T005 (severidad), T006 (motor) y T007 (propuesta) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 4 dan los dos escenarios del issue.
- **Después:** la severidad, las duras primero y el cierre. Cada fase deja el sistema en verde.
