---

description: "Tareas de F06-H04: asignar actividades no lectivas"
---

# Tasks: Asignar actividades no lectivas

**Input**: documentos de diseño en `specs/032-actividades-no-lectivas/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero. En F06-H03 este orden se rompió en parte; aquí se cumple.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-11-01 (en la spec);
- D-79, aprobada por el autor el 2026-11-01 y por escribir en T001;
- el tope de 40 h, aprobado por el autor el 2026-11-01 (FR-006);
- D-01, D-16, D-64, D-65, D-68, D-72, D-74, D-75, D-77, D-78, ADR-010 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-21, RF-26, RN-22, RN-25, C-12 y C-13 de la ERS.

Plan aprobado por el autor el 2026-11-01.

**Organization**:
- **Fundacional:** la decisión, los contratos, los errores, las migraciones y el dominio.
- **US1, asignar; US2, el proyecto; US3, la gestión; US4, ver; US5, las horas.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US5.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `SPA` = `frontend/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** `ActividadDelCatalogo`, `ActividadNoLectiva` y las horas son dominio puro, sin Spring ni HTTP. `ArquitecturaTest` lo comprueba.
- **Límites (principio V):** sin llaves foráneas a otros esquemas; Generación recibe sus propios valores.
- **Rutas internas (ADR-023):** solo el rol `PROCESO`; un token de usuario recibe 403 y el API Gateway no las enruta.
- **Trazabilidad (principio VII):** cada cambio, en `generacion.registro_cambio`, en la misma transacción, con las horas anteriores.
- **Datos (principio IX):** la ruta de Docentes no lleva contacto ni contrato. Solo datos de prueba ficticios.
- **Decimales:** horas con `BigDecimal` y `compareTo` (D-77).
- **La regla está en el servidor:** la pantalla solo muestra `disponible` y `motivo`; nunca compara tipos ni cargos.
- **Mensajes:** en «tú» (D-74); los errores, del catálogo con sus 8 columnas (D-72), y un solo estado por código.
- **Validación (principio XIV):** `horas` mayor que 0 y hasta 40, con dos decimales; `actividad` `^14[0-3]-[a-p]$`; `proyecto` `^[0-9A-Z-]{1,30}$`; `docente` `^[0-9A-Z]{1,13}$`; `carrera`, con la regla de Académico. En el contrato, el servidor y la pantalla.
- **Pruebas:**
  - Mockito solo en la aplicación;
  - fuera de ahí, dobles escritos a mano;
  - el cliente entre procesos, contra `ReceptorSimulado`;
  - los repositorios, contra PostgreSQL en `integrationTest`;
  - los filtros, con tokens con reclamos reales;
  - tipos de la SPA con `npm run tipos`, no con `tsc -p .`.

---

## Phase 1: Setup

- [X] T001 Escribir D-79 en `docs/decisiones.md`, con su fila en la tabla, y subir el siguiente número libre a D-80.

---

## Phase 2: Foundational

**Purpose**: los contratos, el catálogo y el dominio que usan todas las historias.

- [X] T002 Contratos: `contratos/generacion/openapi.yaml` (tag `Actividades`: `consultarActividades`, `consultarActividadesPosibles`, `asignarActividad`, `quitarActividad`), `contratos/normativa/openapi.yaml` (`consultarActividadesDelPeriodo`), `contratos/docentes/openapi.yaml` (`consultarProyectosYCargosDelDocente`) y los 4 códigos de `contratos/comun/errores.md`, según [contracts/generacion.md](contracts/generacion.md), con un `example` en cada parámetro de ruta. Sumar el 409 `GENERACION_PERIODO_CERRADO` a `quitarPropuesta`, y actualizar `errores.md`. Generar y compilar.
- [X] T003 [P] Prueba de integración de Normativa en `NOR/integrationTest/java/ec/mdd/normativa/`, contra PostgreSQL, que falle primero:
  - cada versión tiene las 47 actividades, comparadas con una lista escrita aparte en la prueba;
  - 14, 9, 9 y 15 por componente;
  - no están `140-a`, `140-p`, `141-j`, `142-j`, `143-i` ni `143-q`;
  - lo que exige cada una, según research §2;
  - ninguna exige proyecto y cargo a la vez;
  - una versión nueva copia las de la anterior.
- [X] T004 [P] Implementar `NOR/main/resources/db/migration/normativa/V5__catalogo_de_actividades.sql`, con el texto literal de los artículos, de la política de asignación de carga horaria de marzo de 2024 (`fuentes_institucionales`), y dejando registro de lo agregado a cada versión, como V4 (principio VII). Copiar el catálogo al guardar una versión nueva en `RepositorioVersionesJdbc`. Hace pasar T003.
- [X] T005 [P] Implementar `GEN/main/resources/db/migration/generacion/V4__actividades_no_lectivas.sql`, según [data-model.md](data-model.md): la tabla, el índice único con `coalesce`, los `check` y las dos acciones nuevas de `registro_cambio`.
- [X] T006 [P] Pruebas de `GEN/test/java/ec/mdd/generacion/dominio/ActividadDelCatalogoTest.java`, sin dobles, que fallen primero:
  - sin exigencias: disponible, y con un proyecto enviado lo rechaza;
  - con tipos de proyecto: exige uno de los proyectos del docente, del tipo que corresponde;
  - sin proyecto, de otro tipo o que no es del docente: rechazado;
  - con cargos: disponible con uno vigente, y rechazado sin él;
  - con varios cargos, basta uno;
  - la disponibilidad dice su motivo y sus proyectos posibles.
- [X] T007 [P] Implementar `Componente`, `ActividadDelCatalogo`, `ProyectosYCargosDelDocente`, `ProyectoNoValido` y `CargoNoVigente` en `GEN/main/java/ec/mdd/generacion/dominio/`. Hace pasar T006.
- [X] T008 [P] Pruebas en `GEN/test/java/ec/mdd/generacion/dominio/VersionDistributivoTest.java` y un `SumaPorComponenteTest.java` nuevo, sin dobles: `actividadDe` distingue el proyecto; `exigirEditable` sigue igual; la suma por componente, con decimales. Implementar `ActividadNoLectiva`, `SumaPorComponente` y `VersionDistributivo.actividades`. Tienen que fallar primero.

**Checkpoint**: el catálogo está en la base y la regla, probada sin dobles.

---

## Phase 3: User Story 1 - Asignar una actividad del catálogo (Priority: P1)

**Goal**: el coordinador asigna, reemplaza y quita una actividad.

**Independent Test**: con un distributivo, asignar horas de planificación a un docente y leerlas por la API.

- [X] T009 [P] [US1] Pruebas de `consultarActividadesDelPeriodo` en `NOR/test/java/ec/mdd/normativa/adaptadores/entrada/`: las actividades con sus exigencias, 404 sin versión y token de usuario 403. Implementarlo en `InternoNormativaControlador`, con su puerto.
- [X] T010 [P] [US1] Pruebas de `consultarProyectosYCargosDelDocente` en `DOC/test/java/ec/mdd/docentes/adaptadores/entrada/`: solo los proyectos asignables del periodo en los que participa; solo los cargos vigentes; 404 con un docente o un periodo que no existen; token de usuario 403; y que no trae contacto ni contrato. Implementarlo en `InternoDocentesControlador`, sobre `ConsultarProyectos` y `ConsultarCargos`.
- [X] T011 [US1] Sumar a `DatosDesdeProcesoDatosTest`: `actividades` y `proyectosYCargos`, con vacío ante un 404. Implementarlos en `DatosParaAfinidad`, `DatosParaSugerencia` y `DatosDesdeProcesoDatos`.
- [X] T012 [US1] Pruebas de `GEN/test/java/ec/mdd/generacion/aplicacion/AsignarActividadServicioTest.java`, con Mockito, que fallen primero:
  - guarda con quién y cuándo, y lo registra;
  - reemplazar registra las horas anteriores;
  - quitar, y quitar la que no existe;
  - quitar con el periodo cerrado se rechaza, sin borrar;
  - quitar la actividad de un docente que ya no está activo;
  - una carrera sin parcial lo recibe con la primera actividad;
  - sin distributivo, con el periodo cerrado, sin versión, otra carrera, parcial enviado, docente no activo y actividad fuera del catálogo: su excepción, sin guardar;
  - sin horas suficientes se guarda, con `horasSuficientes` en falso;
  - al reemplazar, el aviso no cuenta las horas anteriores de esa misma actividad: un docente de 40 h con 36 h en total, de las cuales 4 h son de esta actividad, admite cambiarla a 8 h sin aviso;
  - un fallo entre procesos no guarda nada.
- [X] T013 [US1] Implementar `AsignarActividad`, sus excepciones `ActividadFueraDelCatalogo` y `ActividadNoEncontrada`, y `AsignarActividadServicio`, con su bean. Hace pasar T012.
- [X] T014 [US1] Prueba de `RepositorioDistributivosJdbc` en `GEN/integrationTest/java/ec/mdd/generacion/RepositorioDistributivosIntegrationTest.java`: guardar y leer; reemplazar con la misma llave, con y sin proyecto; el mismo docente y actividad con otro proyecto son dos; quitar; horas en 0 y sin parcial, rechazadas por la base. Implementar `guardarActividad`, `quitarActividad` y la lectura en `vigenteDe`.
- [X] T015 [US1] Pruebas en `GEN/test/java/ec/mdd/generacion/adaptadores/entrada/ActividadesContratoTest.java` y `PermisosGeneracionTest`, con tokens con reclamos reales: 200, 204, 400 por cada campo, válidos e inválidos, también 0, 40, 40,01 y 3 decimales; 403 de otra carrera y sin carrera; 404; cada 409; 502 y 504. Implementar `ActividadesControlador` y los códigos en `ErroresGeneracion`, con `ErroresGeneracionTest`. `PermisosGeneracionTest` pasa a 11 operaciones.
- [X] T034 [US1] Corregir F06-H03 (FR-024): en `ProponerDocenteServicioTest` y `PropuestaContratoTest`, quitar una propuesta con el periodo cerrado responde 409 sin borrar. La prueba debe fallar primero; luego corregir `ProponerDocenteServicio.quitar`.

**Checkpoint**: se asigna y se quita por la API.

---

## Phase 4: User Story 2 - Vincular el proyecto (Priority: P1)

**Goal**: RN-25 se cumple al guardar.

**Independent Test**: asignar una actividad de investigación con y sin un proyecto del docente.

- [X] T016 [US2] Sumar a `AsignarActividadServicioTest`: con proyecto válido guarda su código y su nombre; sin proyecto, de otro tipo, ajeno o no asignable: `ProyectoNoValido`, sin guardar; un proyecto en una actividad que no lo pide: `ProyectoNoValido`. Debe fallar primero; ajustar el servicio.
- [X] T017 [US2] Pruebas de `GEN/test/java/ec/mdd/generacion/aplicacion/ConsultarActividadesPosiblesServicioTest.java`, con Mockito: cada actividad con su disponibilidad, sus proyectos y su motivo; las 47 llegan; docente no activo; sin versión. Implementar el servicio y `consultarActividadesPosibles` en `ActividadesControlador`, con su contrato en `ActividadesContratoTest`.

---

## Phase 5: User Story 3 - Registrar horas de gestión (Priority: P1)

**Goal**: las actividades que son cargos exigen el cargo.

**Independent Test**: asignar `143-d` a un docente con y sin ese cargo vigente.

- [X] T018 [US3] Sumar a `AsignarActividadServicioTest`: `143-d` con el cargo guarda en `GESTION`; sin él, `CargoNoVigente`, sin guardar; `143-a` con vicerrector guarda; `143-k` guarda sin pedir cargo. Debe fallar primero; ajustar el servicio.

---

## Phase 6: User Story 4 - Ver las actividades de mi carrera (Priority: P1)

**Goal**: la lista por docente, con la suma por componente.

**Independent Test**: asignar actividades a dos docentes y leerlas por carrera.

- [X] T019 [US4] Pruebas de `GEN/test/java/ec/mdd/generacion/aplicacion/ConsultarActividadesServicioTest.java`, con Mockito: por carrera y docente, con la suma; sin distributivo, `creado` en falso; solo su carrera; la carrera del coordinador aparece aunque no tenga parcial; un docente que ya no está activo se sigue viendo. Implementar el servicio.
- [X] T020 [US4] Pruebas de `consultarActividades` en `ActividadesContratoTest` y `PermisosGeneracionTest`: 200, sin distributivo, un coordinador solo ve su carrera, y sin carrera 403. Implementarlo en `ActividadesControlador`.

---

## Phase 7: User Story 5 - Ver las horas completas (Priority: P1)

**Goal**: las actividades cuentan en las horas del docente.

**Independent Test**: asignar actividades en una carrera y mirar la sugerencia desde otra.

- [X] T021 [US5] Sumar a `HorasDelPeriodoTest`, sin dobles, que falle primero: una actividad suma a las asignadas; la de otra carrera, también a las de otras carreras; con decimales; junto con propuestas; no se excluye por el paralelo mirado. Cambiar `HorasDelPeriodo.calcular`, y ajustar `ConsultarSugerenciaServicio`, `ProponerDocenteServicio`, `ConsultarParcialesServicio` y sus pruebas.
- [X] T022 [US5] Sumar a `ConsultarSugerenciaServicioTest` y `ProponerDocenteServicioTest`: las horas con actividades, y `horasSuficientes` en falso por ellas.

**Checkpoint**: los escenarios del issue y las horas completas responden por la API.

---

## Phase 8: La pantalla

**Purpose**: asignar y ver desde el navegador.

- [X] T023 [US1] Crear `SPA/pantallas/actividades/consultas.ts` y `datosDePrueba.ts`: `useActividades`, `useActividadesPosibles`, `asignarActividad` y `quitarActividad`.
- [X] T024 [US1] Pruebas en `SPA/pantallas/actividades/Actividades.test.tsx`, que fallen primero:
  - la entrada del menú y la ruta, con `LECTURA_DE_LA_OFERTA`;
  - el formulario lista las actividades por componente, sin comisiones;
  - horas vacías, en 0, mayores que 40 o con 3 decimales no se guardan, y lo dicen;
  - guardar recarga la lista;
  - el aviso de horas insuficientes;
  - un error del catálogo no cierra el diálogo, y un 400 se muestra en su campo;
  - quitar, con confirmación;
  - sin distributivo lo dice y no ofrece asignar;
  - quien no escribe ve la lista sin acciones.
- [X] T025 [US2] Pruebas en el mismo archivo: una actividad con proyectos pide elegir uno; sin proyectos, dice por qué y no deja guardar; una sin exigencias no pide proyecto.
- [X] T026 [US3] Pruebas en el mismo archivo: una actividad de cargo no disponible muestra su motivo y no deja guardar.
- [X] T027 [US4] Pruebas en el mismo archivo: cada docente con sus actividades, sus horas, su proyecto y la suma por componente; el alcance total ve varias carreras.
- [X] T028 [US1] Implementar `SPA/pantallas/actividades/Actividades.tsx` y su diálogo, con la ruta y la entrada en `SPA/navegacion/Marco.tsx`. Hace pasar T024 a T027.
- [X] T029 [P] [US1] En `SPA/pantallas/oferta/Oferta.test.tsx`, que falle primero: quien propone sin cambiar la oferta ve «No cambias la oferta, pero propones sus docentes.»; quien no hace ninguna de las dos sigue viendo «Solo lectura». Implementarlo en `Oferta.tsx`.
- [X] T030 [US1] `frontend/e2e/actividades.spec.ts`: asignar con y sin proyecto, y quitar, con `simularApi` y axe sobre la pantalla y el diálogo.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [X] T031 `deploy/probar_actividades.sh`, según [quickstart.md](quickstart.md), con los procesos reales, y su paso en `.github/workflows/perfil-consolidado.yml`:
  - asignar, reemplazar y quitar: una sola (SC-003);
  - el catálogo sin comisiones (SC-001);
  - con y sin proyecto, y un proyecto ajeno (SC-002);
  - con y sin cargo (SC-002);
  - horas en 0, 41 y con 3 decimales;
  - quitar una actividad y una propuesta con el periodo cerrado;
  - otra carrera, 403, y el Vicerrector no asigna;
  - las horas con propuestas y actividades, frente a una suma aparte, desde dos coordinadores (SC-004);
  - guardar en menos de 3 s (SC-006);
  - con `datos` detenido, el mensaje en menos de 10 s, sin guardar nada (SC-007);
  - la base limpia al terminar.

  Ajustar `deploy/probar_normativa.sh` si cuenta las filas de una versión.
- [X] T032 Crear `specs/032-actividades-no-lectivas/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` de Generación, Normativa y Docentes;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck y Gitleaks;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #49`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [X] T033 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Antes de la comprobación, indicarle al autor qué registrar para ver un proyecto y un cargo, si quiere. Después: la comprobación del autor, con SC-005, y el PR de evidencia con `Closes #49`, que dice qué no se vio en la demo.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 antes de todo el código; T003 antes que T004; T006 antes que T007; T004, T005, T007 y T008 no dependen entre sí.
- **US1:** T009 y T010 son [P]; T011 depende de los dos; T012 a T015, en orden; T034 depende de T002.
- **US2:** depende de T013. T016 y T017, en orden.
- **US3:** depende de T016.
- **US4:** depende de T014.
- **US5:** depende de T014.
- **Pantalla:** depende de T015, T017 y T020, por el contrato real.
- **Cierre:** al final.

## Parallel Example

```text
Después de T002: T003-T004 (catálogo), T005 (migración) y T006-T007 (la regla) a la vez.
Dentro de US1: T009 (Normativa) y T010 (Docentes) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 7 dan los escenarios del issue y las horas completas por la API.
- **Después:** la pantalla (fase 8) y el cierre (fase 9). Cada fase deja el sistema en verde.
