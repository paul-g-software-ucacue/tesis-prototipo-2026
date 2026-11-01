---

description: "Tareas de F07-H01: ver los totales del docente al instante"
---

# Tasks: Ver los totales del docente al instante

**Input**: documentos de diseño en `specs/033-totales-del-docente/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md), [contracts/normativa.md](contracts/normativa.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-11-01 (en la spec);
- D-82, aprobada por el autor el 2026-11-01 y por escribir en T001;
- los términos del glosario, aprobados por el autor el 2026-11-01 y por escribir en T001;
- D-01, D-64, D-72, D-74, D-75, D-77, D-78, ADR-010 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-27 y RN-05 de la ERS.

Plan aprobado por el autor el 2026-11-01.

**Organization**:
- **Fundacional:** la decisión, el glosario, los contratos y el dominio.
- **US1, ver al guardar; US2, la proporción; US3, antes de guardar; US4, la lista; US5, lo que no se puede mostrar.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US5.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `SPA` = `frontend/src/pantallas`;
  - dentro de `GEN`, `main` y `test` llevan `java/ec/mdd/generacion`;
  - dentro de `NOR`, `main` y `test` llevan `java/ec/mdd/normativa`.

---

## Phase 1: Setup

- [X] T001 Escribir D-82 en `docs/decisiones.md`, con su fila en la tabla: la carga horaria se lee con los permisos de leer la oferta, el distributivo o los catálogos, sin filtro de carrera, como excepción a D-64, y solo con sumas. En `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md`: agregar `Proporcion` al término «Proporción 60/40», agregar «Meta de la proporción» (`MetaDeProporcion`) y aclarar en `CargaHoraria` que hasta F07-H02 puede superar las horas contratadas (D-78).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: los contratos y el cálculo, que todas las historias usan.

**⚠️ CRITICAL**: el dominio se escribe con sus pruebas primero, y cada grupo se ve fallar antes de su código.

- [X] T002 Actualizar `contratos/normativa/openapi.yaml` con `consultarProporcionDelPeriodo` y `contratos/generacion/openapi.yaml` con `consultarCargaHoraria`, `CargaHoraria`, `HorasPorComponente` con `clase`, `ActividadesDelDocente.totales` y las respuestas 502, 503 y 504 de `consultarActividades`, según [contracts/](contracts/). Cubrir el periodo y el docente con el patrón que ya usa el contrato (FR-023). Generar los modelos con `:servicios:generacion:openApiGenerate` y `:servicios:normativa:openApiGenerate`.
- [X] T003 [P] Escribir `GEN/test/dominio/ProporcionTest.java`, sin dobles: 8 y 4 dan 67 y 33; 1 y 2 dan 33 y 67; un medio exacto redondea hacia arriba; horas con dos decimales; solo clase da 100 y 0; solo otras da 0 y 100; 0 y 0 no dan proporción; siempre suman 100 (FR-008, FR-010, FR-011, SC-003). Verla fallar.
- [X] T004 Crear `GEN/main/dominio/Proporcion.java` y `GEN/main/dominio/MetaDeProporcion.java` hasta pasar T003.
- [X] T005 [P] Escribir `GEN/test/dominio/CargaHorariaTest.java`, sin dobles: suma los cinco componentes en dos carreras; excluye los paralelos retirados; usa las horas actuales del paralelo; conserva dos decimales; solo cuenta al docente pedido; `disponibles` nunca baja de 0; `exceso` con 33 h de 20 da 13; con 0 horas contratadas, `disponibles` es 0 y lo asignado es exceso; un docente inactivo sigue contando; investigación, vinculación y gestión no cambian la proporción (FR-001 a FR-004, FR-006, FR-007, FR-009). Agregar la igualdad con `HorasDelPeriodo`: con los mismos datos, `asignadas()` coincide (FR-005, SC-002). Verla fallar.
- [X] T006 Crear `GEN/main/dominio/CargaHoraria.java` y hacer que `HorasDelPeriodo` exponga a su paquete cómo una propuesta se convierte en horas, sin cambiar su comportamiento, hasta pasar T005.

**Checkpoint**: el cálculo está probado sin dobles. Las historias pueden empezar.

---

## Phase 3: User Story 1 - Ver los totales al guardar (Priority: P1) 🎯 MVP

**Goal**: consultar los totales de un docente, de todas las carreras, y verlos al guardar.

**Independent Test**: con horas en dos carreras, guardar y comparar el panel con una suma aparte.

### Tests for User Story 1

- [X] T007 [P] [US1] Escribir la prueba de Normativa en `NOR/integrationTest/java/ec/mdd/normativa/` para la meta sembrada y la de una versión nueva, y la prueba de contrato con el token de proceso; sin versión, 404. Verla fallar.
- [X] T008 [P] [US1] Escribir `GEN/test/aplicacion/ConsultarCargaHorariaServicioTest.java`, con Mockito: arma la carga con la versión vigente, la oferta y la meta; con un parcial enviado sus horas cuentan; en un periodo cerrado se puede consultar; no guarda nada (FR-027). Verla fallar.
- [X] T009 [P] [US1] Agregar a las pruebas del cliente interno de Generación, con `ReceptorSimulado`, la lectura de `/interno/normativa/periodos/{periodo}/proporcion`. Verla fallar.
- [X] T010 [P] [US1] Escribir la prueba de contrato de `consultarCargaHoraria` con `ValidadorDeContrato`, y agregar la operación a `VerificadorDePermisos`. Comprobar que un coordinador lee a un docente de otra carrera (D-82, FR-020) y que un rol sin esos permisos, como Docente u Operador de datos, recibe 403. Comprobar que la respuesta no trae asignaturas, actividades ni contacto (FR-021, SC-007). Verla fallar.

### Implementation for User Story 1

- [X] T011 [US1] Implementar en Normativa la lectura de los parámetros de RN-05, `consultarProporcionDelPeriodo` en `NOR/main/adaptadores/entrada/InternoNormativaControlador.java`, y su servicio y puerto, hasta pasar T007.
- [X] T012 [US1] Agregar `meta(periodo)` a `GEN/main/puertos/salida/DatosParaActividades.java` y a `GEN/main/adaptadores/salida/DatosDesdeProcesoDatos.java`, hasta pasar T009.
- [X] T013 [US1] Crear `GEN/main/puertos/entrada/ConsultarCargaHoraria.java`, `CargaDelDocente.java` y `GEN/main/aplicacion/ConsultarCargaHorariaServicio.java`, y registrarlo en `ConfiguracionGeneracion`, hasta pasar T008.
- [X] T014 [US1] Exponer `consultarCargaHoraria` en un controlador de `GEN/main/adaptadores/entrada/`, sin aplicar el filtro de carrera, hasta pasar T010.
- [X] T015 [US1] Escribir primero en `SPA/comun/CargaHoraria.test.tsx` el panel con los cinco componentes y las horas contratadas, asignadas y disponibles; después crear `SPA/comun/CargaHoraria.tsx` y su consulta, con la clave `['generacion', 'carga-horaria', periodo, docente]`.
- [X] T016 [US1] Escribir primero en `SPA/actividades/Actividades.test.tsx` y `SPA/oferta/Oferta.test.tsx` que el panel aparece al guardar y se actualiza al reemplazar y al quitar (FR-014, FR-015, FR-017). Después agregar el panel del último docente guardado a `SPA/actividades/Actividades.tsx`, y el de la propuesta a `SPA/oferta/SugerenciaDelParalelo.tsx`. Invalidar por el prefijo `['generacion', 'carga-horaria']` al guardar y al quitar, para que un panel de otro docente también se refresque.

**Checkpoint**: el primer escenario del issue funciona.

---

## Phase 4: User Story 2 - Ver la proporción 60/40 (Priority: P1)

**Goal**: ver los porcentajes y la meta, sin marcarlo como error.

**Independent Test**: con 8 h de clase y 4 h de otras, ver 67 % y 33 %, con la meta 60/40.

- [X] T017 [US2] Escribir primero en `SPA/comun/CargaHoraria.test.tsx`: «Clase 67 % · Otras 33 % (meta 60/40)»; apartarse de la meta no usa estilo de error (FR-013); sin horas dice «Todavía no hay proporción» (FR-011); una meta distinta se muestra tal como llega (FR-012). Después completar el panel.
- [X] T018 [US2] Agregar a `ConsultarCargaHorariaServicioTest` y a la prueba de contrato que `proporcion` no viene sin horas, y que `meta` viene de la versión del periodo (FR-012). Ajustar lo que falle.

**Checkpoint**: los dos escenarios del issue funcionan por la API y en la pantalla.

---

## Phase 5: User Story 3 - Ver si le caben horas antes de asignar (Priority: P2)

**Goal**: ver el panel al elegir al docente, antes de guardar.

**Independent Test**: elegir a un docente sin asignaciones en mi carrera y ver sus horas.

- [X] T019 [US3] Escribir primero en `SPA/actividades/Actividades.test.tsx`: al elegir un docente en el formulario se ve su panel, y al cambiarlo, el del otro (FR-016). Después agregar el panel a `SPA/actividades/DialogoAsignar.tsx`.
- [X] T020 [US3] Escribir primero en `SPA/oferta/Oferta.test.tsx`: al desplegar un candidato se pide y se ve su panel, y los que no se despliegan no se piden (FR-016). Después agregar el panel al detalle del candidato en `SPA/oferta/SugerenciaDelParalelo.tsx`.
- [X] T021 [US3] Escribir primero en `SPA/comun/CargaHoraria.test.tsx`: con exceso dice «Se pasó por 13 h» y sus disponibles son 0 (FR-007). Después completar el panel.

**Checkpoint**: el coordinador sabe si le caben horas antes de guardar.

---

## Phase 6: User Story 4 - Ver los totales en la lista de actividades (Priority: P2)

**Goal**: la columna de horas muestra los totales de todas las carreras.

**Independent Test**: un docente con actividades en dos carreras muestra en la lista los totales de las dos.

- [X] T022 [US4] Actualizar `ConsultarActividadesServicioTest`: `totales` trae la clase y las otras carreras; la lista sigue trayendo solo las actividades de esta carrera (FR-018, FR-021); un docente inactivo sigue en la lista. Verla fallar.
- [X] T023 [US4] Cambiar `GEN/main/aplicacion/ConsultarActividadesServicio.java`, `ActividadesDelDocente` y `GEN/main/adaptadores/entrada/ActividadesControlador.java`, pasarle la oferta en `ConfiguracionGeneracion`, y quitar `SumaPorComponente` si ya no se usa, hasta pasar T022. Actualizar `ActividadesContratoTest` y `deploy/probar_actividades.sh`, que lee `horasPorComponente`.
- [X] T024 [US4] Escribir primero en `SPA/actividades/Actividades.test.tsx`: la columna muestra «Clase» y los totales; el Vicerrector los ve sin acciones (FR-022); si la lista falla, se ve el mensaje traducido. Después cambiar `SPA/actividades/Actividades.tsx` y `SPA/actividades/datosDePrueba.ts`.

**Checkpoint**: la lista y el panel dicen lo mismo.

---

## Phase 7: User Story 5 - Lo que no se puede mostrar (Priority: P3)

**Goal**: un motivo claro, sin confundir ni impedir guardar.

**Independent Test**: con `datos` detenido, pedir los totales y ver el mensaje.

- [X] T025 [P] [US5] Agregar a las pruebas de `ConsultarCargaHorariaServicio` y de contrato: sin distributivo, `GENERACION_SIN_DISTRIBUTIVO`; docente no activo, `GENERACION_DOCENTE_NO_ACTIVO`; sin versión, `GENERACION_PERIODO_SIN_ABRIR`, cada una con su estado (FR-024). Ajustar lo que falle. Agregar la operación a esos códigos en `contratos/comun/errores.md`.
- [X] T026 [P] [US5] Escribir primero en `SPA/comun/CargaHoraria.test.tsx` y en las pantallas: el panel muestra el mensaje traducido y deja reintentar; si la asignación se guardó y el panel falla, dice que sí se guardó (FR-025, FR-026); sin permiso no se pide. Después completar el panel y las pantallas.

**Checkpoint**: todas las historias funcionan.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T027 [P] Agregar `frontend/e2e/totales.spec.ts`, con la API simulada: el panel en las dos pantallas, la proporción, el exceso y axe sobre el panel, incluido a 390 px.
- [X] T028 Crear `deploy/probar_totales.sh` y agregarlo a `.github/workflows/perfil-consolidado.yml`. Parte de los datos de prueba, con dos coordinadores y un docente en dos carreras, y comprueba:
  - los totales frente a una suma hecha aparte, sin el código de Generación (SC-001);
  - las horas asignadas frente a las de la sugerencia (SC-002);
  - la proporción con los porcentajes frente a un cálculo aparte (SC-003);
  - el exceso;
  - reemplazar una actividad cuenta solo las horas nuevas;
  - con el periodo cerrado, la consulta responde 200;
  - un coordinador lee a un docente de otra carrera y la respuesta no trae asignaturas ni actividades (SC-007);
  - la lista de actividades con los totales de las dos carreras;
  - guardar una asignación y consultar los totales, juntos, en menos de 3 s (SC-004);
  - con `datos` detenido, los totales y la lista responden con su mensaje en menos de 10 s (SC-006);
  - la base limpia al terminar.
- [X] T029 Ajustar lo que cuente operaciones o rutas en los scripts de `deploy/`, y actualizar el comentario de cabecera de `contratos/generacion/openapi.yaml`.
- [X] T030 Crear `specs/033-totales-del-docente/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` completo;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck y Gitleaks, con copias nuevas en el scratchpad;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #50`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [X] T031 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Antes de la comprobación, indicarle al autor qué mirar, con SC-005. Después: la comprobación del autor y el PR de evidencia con `Closes #50`, que dice qué no se vio en la demo.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 antes de todo el código; T003 antes que T004; T005 antes que T006.
- **US1:** depende de la fase 2. T007 a T010 son [P]; T011 a T014, después de sus pruebas; T015 y T016, por el contrato real.
- **US2:** depende de T013 y T015.
- **US3:** depende de T015.
- **US4:** depende de T006.
- **US5:** depende de T013 y T015.
- **Cierre:** al final.

## Parallel Example

```text
Después de T002: T003-T004 (proporción) y T005-T006 (carga) a la vez.
Dentro de US1: T007 (Normativa), T008 (servicio), T009 (cliente) y T010 (contrato) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 4 dan los dos escenarios del issue.
- **Después:** el panel antes de guardar, la lista, los errores y el cierre. Cada fase deja el sistema en verde.
