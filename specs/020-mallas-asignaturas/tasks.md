---

description: "Tareas de F03-H02: mantener carreras, mallas y asignaturas"
---

# Tasks: Mantener carreras, mallas y asignaturas

**Input**: documentos de diseño en `specs/020-mallas-asignaturas/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/academico.md](contracts/academico.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-16, D-65, D-69, D-70, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-12, RF-14, RN-12, RN-18, RN-19 y RN-24 de la ERS; el glosario §7 («Carrera», «Modalidad», «Nivel de formación», «Malla», «Asignatura», «Ciclo», «Tronco común», «Campo de conocimiento», «Jornada»).

**Organization**:
- **Fundacional:** el contrato, el dominio, la migración V4 y el repositorio.
- **US1:** mantener las carreras.
- **US2:** mantener las mallas y sus asignaturas.
- **US3:** los datos para la afinidad.
- **US4:** la pantalla «Carreras y mallas».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US4.
- Rutas relativas a la raíz. Abreviaturas: `ACA` = `backend/servicios/academico/src`, `JACA` = `ACA/main/java/ec/mdd/academico`, `PACA` = `ACA/test/java/ec/mdd/academico` e `IACA` = `ACA/integrationTest/java/ec/mdd/academico`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura. `ArquitecturaTest` y `ModulosTest` lo comprueban.
- **Soporte (principio II):** `Carrera`, `Malla`, `Asignatura` y `CampoConocimiento` son valores con sus reglas; `DatosMaestros` tiene las reglas entre entidades; sin agregado elaborado ni eventos.
- **Principio VI:** cada lista de valores permitidos vive en una enumeración del dominio, comparada con el diccionario de datos; los 11 campos amplios, solo en `V4` y en las pruebas.
- **Principio VII:** nada se borra; cada cambio, en `academico.registro_cambio`.
- **Datos de prueba:** las cargas 02 y 04 de la ERS (sintéticas, RNF-SEG-06), copiadas a `ACA/test/resources/academico/datos-de-prueba/`.
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers; contrato con swagger-request-validator; 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores; la evidencia va en `specs/020-mallas-asignaturas/evidencia.md` y los commits llevan `Refs #36`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/020-mallas-asignaturas/evidencia.md` con estas secciones: entorno; base (contrato, dominio, migración y repositorio); carreras (US1); mallas y asignaturas (US2); datos para la afinidad (US3); pantalla (US4); cierre; equipo de demostración; notas. Copiar `02_carreras.csv` y `04_asignaturas_malla.csv` de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `ACA/test/resources/academico/datos-de-prueba/`, con su origen en la evidencia.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV, research §7)

- [X] T002 Cambiar `contratos/academico/openapi.yaml` según contracts/academico.md: el tag `Carreras`, los esquemas y las 13 operaciones, las de lectura con `x-mdd-permiso: [DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` y las de escritura con `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`; `example` en cada parámetro de ruta (`550613A01-P-0301`, `DS-2019`, `DS-B-I-MD-101`); en `AsignaturaNueva`, `carrera` y `horasSemanalesClase` obligatorios (escenario 1); el comentario de cabecera cita F03-H02, RF-12, RF-14 y D-70. Agregar los 4 códigos a `contratos/comun/errores.md`. Regenerar (`openApiGenerate`, `generar:api`); `verificar:ruptura` en verde.

### Dominio (research §1 a §4, data-model.md)

- [X] T003 Prueba primero, en `PACA/dominio/ValoresPermitidosTest.java`: cada enumeración (`NivelDeFormacion`, `Modalidad`, `Jornada`, `UnidadDeOrganizacionCurricular`, `EstadoDeCarrera`) tiene exactamente los valores del diccionario de datos, leídos del formato de su columna (separados por «,» o por «;», como el de las jornadas) en `backend/servicios/cargas/src/main/resources/cargas/diccionario-de-datos.yaml` como archivo (sin depender del módulo Cargas), y los 11 campos amplios coinciden con su catálogo `CAMPOS_AMPLIOS_CINE`; el estado de la asignatura del diccionario es exactamente `ACTIVA, INACTIVA`, que el modelo guarda como `activa`. `EstadoDeMalla` (`VIGENTE`, `DEROGADA`) no está en el diccionario: viene de la aclaración 2 y no se compara. Crear las enumeraciones en `JACA/dominio/`. T003 pasa.
- [X] T004 Prueba primero, en `PACA/dominio/CarreraTest.java`, `MallaTest.java`, `AsignaturaTest.java` y `CampoConocimientoTest.java`, sin dobles: cada regla de research §3 con su campo y su motivo en español en `DatoInvalido` (código, nombre, siglas, jornadas vacías o repetidas, campo específico que no empieza por el amplio, detallado que no empieza por el específico, número de ciclos y ciclo fuera de 1 a 10, horas fuera de 0,5 a 20 o con más de dos decimales, prácticas fuera de 0 a 20, créditos no positivos, menos de 2 palabras clave, palabras repetidas o vacías); los nombres y las palabras se guardan sin espacios en los extremos; `corregida` no cambia el código (ni la carrera ni la malla en una asignatura).
- [X] T005 Prueba primero, en `PACA/dominio/DatosMaestrosTest.java`, sin dobles: una carrera con una sede inexistente, inactiva o de otro tipo da `DatoInvalido` en `sede`; una malla de una carrera no vigente, en `carrera`; una asignatura con carrera no vigente, malla derogada o de otra carrera, o ciclo mayor que el número de ciclos, en su campo; bajar el número de ciclos por debajo del ciclo de una asignatura activa, en `numeroCiclos`; reactivar una asignatura de una malla derogada, en `malla`; las **7 carreras, 12 mallas y 300 asignaturas** de las cargas 02 y 04 de prueba (en `PACA/dominio/DatosMaestrosDePrueba.java`, que las lee de los recursos) se registran todas en orden, y cada malla suma 100 horas. Crear en `JACA/dominio/` `CampoConocimiento`, `CampoAmplio`, `Carrera`, `Malla`, `Asignatura` y `DatosMaestros`, con JavaDoc en español que cite RF-12, RF-14, RN-19 y el glosario. T004 y T005 pasan.

### Persistencia (data-model.md)

- [ ] T006 Prueba primero, en `IACA/AcademicoEsquemaIntegrationTest.java` (ampliar): `campo_amplio` trae los 11 de CINE; `carrera`, `malla` y `asignatura` y sus `check` rechazan cada combinación inválida (siglas repetidas, jornadas vacías, campo específico de otro amplio, número de ciclos 0, horas 0,4, una sola palabra clave, una asignatura con una malla de otra carrera); `registro_cambio` acepta las 9 acciones nuevas y sigue aceptando las 8 de antes. Tiene que fallar sin la migración.
- [ ] T007 Crear `ACA/main/resources/db/migration/academico/V4__carreras_mallas_asignaturas.sql` según data-model.md, con los 11 campos amplios del catálogo del diccionario y un comentario que cite RF-12, RF-14 y F03-H02. T006 pasa.
- [ ] T008 Prueba primero, en `IACA/RepositorioCarrerasJdbcIntegrationTest.java`: registra y lee una carrera, una malla y una asignatura igual, campo por campo (con las jornadas y las palabras clave en su orden); un código o unas siglas repetidos no se guardan (el método devuelve el motivo); `guardar` cambia cada dato y el estado; las horas de una malla suman solo las asignaturas activas; `bloquear` toma el candado de las tres tablas. Crear `JACA/puertos/salida/RepositorioCarreras.java` y `JACA/adaptadores/salida/RepositorioCarrerasJdbc.java`. T008 pasa.

**Checkpoint**: el contrato, el dominio, V4 y el repositorio están listos.

---

## Phase 3: User Story 1 - Mantener las carreras (Priority: P1) 🎯 MVP

**Goal**: registrar, consultar, corregir y cambiar el estado de las carreras (FR-001, FR-003, FR-005, FR-006, FR-008, FR-009, FR-010).

**Independent Test**: registrar una carrera en la sede matriz, consultarla, corregir su nombre y ponerla en cierre; cada acción en el registro.

- [ ] T009 [US1] Prueba primero, en `PACA/aplicacion/MantenerCarrerasServicioTest.java` (Mockito): registrar una carrera bloquea, valida la sede con la estructura, guarda y registra `CARRERA_REGISTRADA`; un código repetido lanza `CarreraRepetida` y unas siglas repetidas `SiglasRepetidas`, sin registro; corregir registra `CARRERA_CORREGIDA` con cada cambio, y sin cambios no registra; cambiar el estado registra `CARRERA_ESTADO_CAMBIADO`, y el mismo estado no; una carrera que no existe lanza `CarreraNoEncontrada`. Y `ConsultarCarrerasServicioTest`: los campos amplios, las carreras por código con el resumen de sus mallas, una carrera (404). Crear los puertos de entrada y los servicios. T009 pasa.
- [ ] T010 [US1] Prueba primero, en `PACA/adaptadores/entrada/CarrerasContratoTest.java` (con un doble en memoria que usa las reglas reales del dominio): `registrarCarrera` 201; 409 `ACADEMICO_CARRERA_REPETIDA` y `ACADEMICO_SIGLAS_REPETIDAS`; 400 con el campo `sede` y con `campoEspecifico`; `listarCarreras`, `consultarCarrera` y `listarCamposAmplios` validan contra el contrato; 404; `corregirCarrera` 200; `cambiarEstadoCarrera` 200. `ErroresAcademicoTest` con los 4 códigos. Crear `JACA/adaptadores/entrada/CarrerasControlador.java` (lecturas con `hasAnyAuthority('PERMISO_DISTRIBUTIVO:LECTURA','PERMISO_CATALOGOS:LECTURA')`, escrituras con `hasAnyAuthority('PERMISO_CATALOGOS:ESCRITURA','PERMISO_DISTRIBUTIVO:ESCRITURA')`) y las entradas del catálogo. T010 pasa.
- [ ] T011 [US1] Ampliar `PACA/adaptadores/entrada/PermisosAcademicoTest.java` con `CarrerasControlador`: las 13 operaciones nuevas exigen sus permisos alternativos (25 en total con las de antes); con solo `CATALOGOS:LECTURA`, registrar da 403; con `DISTRIBUTIVO:ESCRITURA` (el Vicerrector), no. Registrar los beans en `ConfiguracionAcademico`. T011 pasa.

**Checkpoint**: las carreras se mantienen por la API.

---

## Phase 4: User Story 2 - Mantener las mallas y sus asignaturas (Priority: P1)

**Goal**: mallas con su versión y su estado, y asignaturas en ellas (FR-002 a FR-005, FR-009, FR-012; SC-002, SC-003).

- [ ] T012 [US2] Prueba primero, en `MantenerCarrerasServicioTest` (ampliar): registrar una malla registra `MALLA_REGISTRADA`; un código repetido lanza `MallaRepetida`; cambiar su estado registra `MALLA_ESTADO_CAMBIADO`; registrar una asignatura registra `ASIGNATURA_REGISTRADA`, y una repetida lanza `AsignaturaRepetida`; corregirla registra `ASIGNATURA_CORREGIDA`; inactivarla y reactivarla, `ASIGNATURA_INACTIVADA` y `ASIGNATURA_REACTIVADA`; corregir el número de ciclos de una carrera revalida sus asignaturas. Ampliar los servicios. T012 pasa.
- [ ] T013 [US2] Prueba primero, en `PACA/adaptadores/entrada/CarrerasContratoTest.java` (ampliar): `registrarMalla` 201 y 409 `ACADEMICO_MALLA_REPETIDA`; `cambiarEstadoMalla` 200; **una asignatura sin `carrera` y sin `horasSemanalesClase` da 400 `COMUN_SOLICITUD_INVALIDA` con un detalle por cada uno** (escenario 1); `registrarAsignatura` 201; con malla derogada, 400 en `malla`; con ciclo de más, 400 en `ciclo`; con una palabra clave, 400 en `palabrasClave`; 409 `ACADEMICO_ASIGNATURA_REPETIDA`; `listarAsignaturasDeCarrera`, `corregirAsignatura` y `cambiarEstadoAsignatura` validan contra el contrato; `listarCarreras` trae en cada malla sus asignaturas activas y sus horas. Ampliar el controlador. T013 pasa.

---

## Phase 5: User Story 3 - Los datos para la afinidad (Priority: P1)

**Goal**: la asignatura con su campo amplio y su nombre, su campo específico, sus palabras clave y su tronco común (FR-007).

- [ ] T014 [US3] Prueba en `CarrerasContratoTest`: `consultarAsignatura` trae `campoAmplio` con su código y su nombre CINE (por ejemplo, `05` «Ciencias naturales, matemáticas y estadística»), `campoEspecifico`, `palabrasClave` una por una y `troncoComun` (escenario 2). Si el controlador ya lo cubre y pasa a la primera, comprobar una vez que falla si se quita el nombre del campo amplio, restaurarlo y anotarlo en la evidencia.

---

## Phase 6: User Story 4 - Carreras y asignaturas en pantalla (Priority: P1)

**Goal**: «Carreras y mallas» con las carreras, sus mallas y las asignaturas por ciclo (FR-011; SC-001).

- [ ] T015 [US4] Prueba primero, en `frontend/src/pantallas/carreras/Carreras.test.tsx` (Vitest con la API simulada): la tabla de carreras con código, nombre, siglas, sede, modalidad y jornadas en español, estado y sus mallas con sus horas; al elegir una malla, sus asignaturas por ciclo con las horas de cada ciclo y de la malla, y cada una con su campo amplio con nombre, sus palabras clave y «Tronco común»; con escritura (`CATALOGOS:ESCRITURA` o `DISTRIBUTIVO:ESCRITURA`), «Registrar carrera» (la sede de una lista de sedes, extensiones y campus activos; el nivel, la modalidad, las jornadas y el campo amplio de listas), «Registrar malla», «Derogar» o «Poner vigente», «Registrar asignatura» (la unidad de organización curricular y el campo amplio de listas; las palabras clave separadas por «;»), «Corregir» e «Inactivar» o «Reactivar», con confirmación; un 400 marca su campo y un 409 muestra su título; sin escritura, sin acciones; el menú y la ruta con `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`. Crear la pantalla en `frontend/src/pantallas/carreras/`, su ruta en `rutas.tsx`, su entrada en `Marco.tsx` debajo de «Estructura orgánica» y la lista de escritura en `funcionalidades.ts`. T015 pasa.
- [ ] T016 [US4] Prueba con Playwright, en `frontend/e2e/carreras.spec.ts`: `vicerrectorado.demo` registra una carrera, una malla y una asignatura en ella, y la ve en su ciclo con sus horas; axe-core sin fallas WCAG 2.1 AA en la tabla, en la malla abierta y en los diálogos de carrera y de asignatura.

**Checkpoint**: las cuatro historias pasan.

---

## Phase 7: Punta a punta y cierre

- [ ] T017 Crear `deploy/probar_carreras.sh`, con el estilo de `probar_estructura.sh` y los 7 pasos de research §9: lee `02_carreras.csv` y `04_asignaturas_malla.csv` de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/`, con un prefijo de 4 caracteres (una `P` y 3 dígitos al azar, como `P123`) delante de cada código de carrera, siglas, malla y asignatura; los datos de prueba dejan sitio: carrera 16 de 20, siglas 6 de 10, malla 13 de 20, asignatura 20 de 30. El script comprueba antes de empezar que ningún código con prefijo pasa su largo; cuelga las carreras de la sede matriz que ya tenga la base, o de una temporal. La limpieza borra en la base solo lo que creó (asignaturas, mallas, carreras y, si la hubo, la sede temporal) y sus registros. Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces seguidas y anotarlo; `shellcheck` sin observaciones.
- [ ] T018 Correr todo, como en las historias anteriores: backend, SPA, los 10 scripts con el perfil reconstruido, `probar_equipo.sh`, `shellcheck` y Gitleaks sobre un clon. Hacer commit y push, abrir el PR en borrador con `Refs #36` y marcarlo listo después de sus runs.
- [ ] T019 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después: esperar el despliegue («Sano en N s»), con `academico` en V4 y los 11 campos amplios; el autor entra con `vicerrectorado.demo`, registra una carrera en su sede matriz, una malla y una asignatura en menos de 2 minutos (SC-001), y la ve con `consolidacion.demo` en solo lectura; abrir el PR de evidencia con `Closes #36` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 primero; T003 antes que T004; T004 y T005 juntas (comparten el dominio); T006 antes que T007; T008 después de T005 y T007.
- **US1:** T009 después de la fase 2; T010 después de T009; T011 después de T010.
- **US2:** T012 después de T009; T013 después de T010 y T012.
- **US3:** T014 después de T013.
- **US4:** T015 después de T002; T016 después de T013 y T015.
- **Cierre:** T017 después de T013; T018 y T019 al final.

### Parallel Opportunities

- La SPA (T015) puede empezar después de T002, con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el dominio con los datos de prueba, V4 y el repositorio.
2. Las carreras por la API. **Validar:** las 7 carreras de prueba entran.

### Incremental

1. **US2 y US3:** mallas, asignaturas y los datos para la afinidad.
2. **US4:** la pantalla.
3. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- RN-18 se aplica en la oferta (F03-H03), con las horas de cada malla.
- El coordinador de cada carrera (RN-12) va con los cargos (F03-H05).
- Las cargas 2 y 4 por archivo se aplican con F01-H04, sobre las mismas reglas.
- Hacer commit por tarea o por grupo lógico en `020-mallas-asignaturas`, con `Refs #36`.
