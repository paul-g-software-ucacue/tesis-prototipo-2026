---

description: "Tareas de F12-H01: cargar la configuración del flujo y del árbol de firmas"
---

# Tasks: Cargar la configuración del flujo y del árbol de firmas

**Input**: documentos de diseño en `specs/038-configuracion-flujo/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-11-02 (en la spec);
- D-89, aprobada por el autor el 2026-11-02 y por escribir en T001;
- el término «Configuración por defecto», aprobado por el autor el 2026-11-02 y por escribir en T001;
- D-13, D-14, D-19, D-63, D-65, D-72, D-74, ADR-004, ADR-007 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-34 y RF-58.

Plan aprobado por el autor el 2026-11-02.

**Organization**:
- **Fundacional:** la decisión, los contratos, los errores, las rutas internas y el dominio.
- **US1, cargar; US2, qué rige; US3, qué corregir; US4, en curso y permisos.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `VAL` = `backend/servicios/validacionocs/src`, con `main` y `test` en `java/ec/mdd/validacionocs`;
  - `ACA` = `backend/servicios/academico/src`;
  - `ROL` = `backend/servicios/roles/src`;
  - `SPA` = `frontend/src/pantallas`.

---

## Phase 1: Setup

- [X] T001 Escribir D-89 en `docs/decisiones.md`, con su fila en la tabla: la configuración es YAML, un solo archivo es la plantilla y la configuración por defecto, y se guarda el texto subido. Agregar «Configuración por defecto» a `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §7.
- [X] T002 Declarar `org.yaml:snakeyaml` en `backend/servicios/validacionocs/build.gradle`, sin versión: la fija Spring Boot. Agregar `contratos/validacionocs/openapi.yaml` a `generar:api` en `frontend/package.json`, y `mdd.procesos.identidad` a `backend/apps/distributivo/src/main/resources/application.yaml`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: los contratos, los errores, los datos y el dominio.

**⚠️ CRITICAL**: el dominio se escribe con sus pruebas primero, y se ve fallar antes de su código.

- [X] T003 Crear `contratos/validacionocs/openapi.yaml` según `contracts/validacionocs.md`, con `x-mdd-permiso`, activar `mdd.contrato` en `backend/servicios/validacionocs/build.gradle`, con las dependencias del patrón de Generación, y agregar la ruta de `contracts/academico.md` y la de `contracts/roles.md`. Generar los modelos.
- [X] T004 Agregar los 5 códigos a `contratos/comun/errores.md`, con sus 8 columnas, y crear `VAL/main/adaptadores/entrada/ErroresValidacionocs.java`, con sus excepciones y `ErroresValidacionocsTest` (FR-023).
- [X] T005 Crear `VAL/main/resources/db/migration/validacionocs/V2__configuracion_flujo.sql`, según `data-model.md`.
- [X] T006 [P] Agregar a Académico la ruta interna del estado del periodo, con pruebas primero.
- [X] T007 [P] Agregar a Roles la ruta interna de firmantes, con pruebas primero: los roles con usuario, si hay Responsable designado en ese periodo y sin datos personales.
- [X] T008 Crear el puerto `VAL/main/puertos/salida/DatosDelPeriodo.java` y `VAL/main/adaptadores/salida/DatosDesdeProcesos.java`, con pruebas primero (`ReceptorSimulado`): el estado de `datos` y los firmantes de `identidad`. Crear `VAL/main/infraestructura/ConfiguracionValidacionocs.java`, con el patrón de `ConfiguracionGeneracion`; cada tarea siguiente agrega ahí sus beans.
- [X] T009 Crear `VAL/test/dominio/ConfiguracionFlujoTest.java`, con esqueleto, y verlo fallar: cada regla de `data-model.md`, una prueba por regla y por límite (FR-004, FR-005, FR-006, FR-012); varios problemas juntos (FR-015). Después crear `TipoDeEtapa`, `Etapa`, `PasoFirma` y `ConfiguracionFlujo` en `VAL/main/dominio/`, en un comando aparte.
- [X] T010 Crear `VAL/main/resources/flujo-por-defecto.yaml`, con los valores de `research.md` §7 y comentarios en español (FR-007). Crear el puerto `LectorDeConfiguracion` y `VAL/main/adaptadores/salida/LectorYaml.java`, con pruebas primero: la plantilla es válida (FR-008); anclas, alias, etiquetas y claves repetidas se rechazan (FR-009, FR-011); un archivo roto da un mensaje sin detalles del lector; la profundidad y el tamaño tienen límite (FR-010).

**Checkpoint**: el archivo se lee y se valida.

---

## Phase 3: User Story 1 - Cargar una configuración válida (Priority: P1) 🎯 MVP

**Goal**: cargar y dejar vigente.

**Independent Test**: subir la plantilla sin cambios.

- [X] T011 [US1] Crear `VAL/main/puertos/salida/RepositorioConfiguracion.java` y su adaptador Jdbc, con prueba de integración primero: la última del periodo, su número, y que las anteriores se conservan (FR-017), también con dos a la vez.
- [X] T012 [US1] Crear `VAL/test/aplicacion/CargarConfiguracionServicioTest.java`, con esqueleto, y verlo fallar: guarda el texto, con quién y cuándo (FR-018); una carga nueva es la vigente. Después crear el caso de uso y el servicio.
- [X] T013 [US1] Crear `VAL/main/adaptadores/entrada/ConfiguracionControlador.java` con el PUT y la plantilla, sus `@PreAuthorize`, con pruebas primero en `ConfiguracionContratoTest`, y un `PermisosValidacionocsTest` nuevo.
- [X] T014 [US1] Crear `SPA/flujo/`, con la ruta en `frontend/src/rutas.tsx` y la entrada en `frontend/src/navegacion/Marco.tsx`. Escribir primero `Flujo.test.tsx`: descargar la plantilla y cargar un archivo.

**Checkpoint**: se carga.

---

## Phase 4: User Story 2 - Saber qué rige (Priority: P1)

**Goal**: ver lo vigente.

**Independent Test**: abrir un periodo sin archivo.

- [X] T015 [US2] Crear `VAL/test/aplicacion/ConsultarConfiguracionServicioTest.java`, con esqueleto, y verlo fallar: sin archivo rige la de por defecto (FR-001, FR-020); con archivo, la última, con su versión, quién y cuándo; `modificable` según el periodo. Después crear el caso de uso y el servicio.
- [X] T016 [US2] Agregar el GET a `ConfiguracionControlador`, con pruebas primero (FR-002, FR-003).
- [X] T017 [US2] Agregar a `Flujo.test.tsx`, y verlo fallar: las etapas con su estado, las firmas, el aviso de la configuración por defecto y la versión cargada, sin editar. Después implementarlo.

**Checkpoint**: se ve lo que rige.

---

## Phase 5: User Story 3 - Saber qué corregir (Priority: P1)

**Goal**: todos los problemas, y nada cambia.

**Independent Test**: un archivo con varios problemas.

- [X] T018 [US3] Agregar a `CargarConfiguracionServicioTest`, y verlo fallar: un rol firmante sin usuario (FR-013); sin Responsable designado (FR-014); los problemas del dominio y de los firmantes juntos (FR-015); un rechazo no guarda nada (FR-016). Después implementarlo.
- [X] T019 [US3] Agregar a `ConfiguracionContratoTest`, y verlo fallar: el 400 con un detalle por problema, el archivo ilegible y el demasiado grande. Después implementarlo.
- [X] T020 [US3] Agregar a `Flujo.test.tsx`, y verlo fallar: la lista de problemas, un archivo ilegible, más de 1 MB antes de subir, y lo vigente no cambia. Después implementarlo.

**Checkpoint**: un archivo malo no hace daño.

---

## Phase 6: User Story 4 - Un distributivo en curso no cambia (Priority: P2)

**Goal**: solo Planificación, y solo quien tiene el permiso.

**Independent Test**: cargar en un periodo vigente.

- [X] T021 [US4] Agregar a `CargarConfiguracionServicioTest`, y verlo fallar: fuera de Planificación se rechaza (FR-019); el periodo que no existe; otro proceso que no responde (FR-024). Después implementarlo.
- [X] T022 [US4] Agregar a `ConfiguracionContratoTest` los permisos (FR-021, FR-022): quien tiene FLUJO_FIRMAS carga y lee; los demás, 403.
- [X] T023 [US4] Agregar a `Flujo.test.tsx`: sin permiso de escritura o fuera de Planificación no sale «Cargar»; sin permiso de lectura no está en el menú. Implementarlo.

**Checkpoint**: la historia está completa.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T024 [P] Agregar `frontend/e2e/flujo.spec.ts`, con la API simulada: ver, cargar, un rechazo y axe en cada estado.
- [X] T025 Crear `deploy/probar_flujo.sh` y agregarlo a `.github/workflows/perfil-consolidado.yml`, según `quickstart.md`, con la llamada real de `distributivo` a `identidad` y la base limpia al terminar. Designa al Responsable por la API y borra `roles.designacion_responsable` al terminar, como `probar_periodos.sh`. Revisar que `SC-006` llegue al 80 % en JaCoCo.
- [X] T026 Crear `specs/038-configuracion-flujo/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` completo;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck con una copia nueva en el scratchpad y Gitleaks en modo `git` sobre un clon, con `.gitleaks.toml`;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #56`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [ ] T027 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Comprobación del autor con SC-004, medida con cronómetro. PR de evidencia con `Closes #56`, y borrar las ramas.

---

## Dependencies & Execution Order

- **T001 y T002:** antes de todo.
- **Fundacional:** T003 a T005 antes del código. T006 y T007 no dependen entre sí. T008 depende de ellas. T010 depende de T009.
- **US1:** depende de la fase 2.
- **US2:** depende de T011.
- **US3:** depende de T012 y T013.
- **US4:** depende de T012 y T013.
- **Cierre:** al final.

## Parallel Example

```text
Después de T005: T006 (Académico) y T007 (Roles) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 3 cargan la configuración.
- **Después:** ver lo vigente, los rechazos y los permisos. Cada fase deja el sistema en verde.
