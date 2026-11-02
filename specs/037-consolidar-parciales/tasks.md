---

description: "Tareas de F10-H02: consolidar los parciales con el informe sustentado"
---

# Tasks: Consolidar los parciales con el informe sustentado

**Input**: documentos de diseño en `specs/037-consolidar-parciales/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-11-02 (en la spec);
- D-88, aprobada por el autor el 2026-11-02 y por escribir en T001;
- el término «Consolidado desactualizado», aprobado por el autor el 2026-11-02 y por escribir en T001;
- D-13, D-72, D-74, D-83, D-85, D-87 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-32, D-16, RNF-POR-01 y MD-04.

Plan aprobado por el autor el 2026-11-02.

**Organization**:
- **Fundacional:** las decisiones, el contrato, los errores, el almacén y la tabla.
- **US1, consolidar; US2, desactualizado; US3, el informe; US4, los permisos.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`, con `main` y `test` en `java/ec/mdd/generacion`;
  - `SPA` = `frontend/src/pantallas`.

---

## Phase 1: Setup

- [X] T001 Escribir D-88 en `docs/decisiones.md`, con su fila en la tabla: los archivos del negocio van en un volumen con nombre, detrás de un puerto en Generación, con la clave generada por el sistema y la huella en la base; subirlo a la plataforma se decide con Firma. Agregar «Consolidado desactualizado» a `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §7.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: el contrato, los errores, la tabla y el almacén.

**⚠️ CRITICAL**: el dominio se escribe con sus pruebas primero, y se ve fallar antes de su código.

- [X] T002 Actualizar `contratos/generacion/openapi.yaml` según `contracts/generacion.md`: las 4 operaciones con `x-mdd-permiso`, los esquemas y la nota de la cabecera. Generar los modelos.
- [X] T003 Agregar los 5 códigos a `contratos/comun/errores.md`, con sus 8 columnas, y a `GEN/main/adaptadores/entrada/ErroresGeneracion.java`, con sus excepciones (FR-025). Actualizar `ErroresGeneracionTest`.
- [X] T004 Crear `backend/servicios/generacion/src/main/resources/db/migration/generacion/V6__consolidacion.sql`, según `data-model.md`, con `DISTRIBUTIVO_CONSOLIDADO` e `INFORME_ADJUNTADO` en `registro_cambio`.
- [X] T005 [P] Crear `GEN/test/dominio/ArchivoPdfTest.java`, con esqueleto, y verlo fallar: un PDF válido con su huella; texto, imagen y vacío rechazados aunque se llamen `.pdf` (FR-015); 10 MB justos pasan y uno más no; el nombre sin ruta, sin caracteres de control y hasta 150. Después crear `GEN/main/dominio/ArchivoPdf.java`, en un comando aparte.
- [X] T006 [P] Crear el puerto `GEN/main/puertos/salida/AlmacenDeArchivos.java`. Escribir primero su prueba de integración con una carpeta temporal: guarda y lee lo mismo; la clave no depende del nombre (FR-017); no queda un archivo a medias; una clave con `../` se rechaza; una que no existe falla; la carpeta se crea al guardar, no al arrancar. Después crear `GEN/main/adaptadores/salida/AlmacenEnDisco.java`.
- [X] T007 [P] Crear `GEN/main/puertos/salida/RepositorioConsolidacion.java` y su adaptador `RepositorioConsolidacionJdbc`, con prueba de integración primero: la última consolidación y el último informe de la versión; se conservan los anteriores (FR-018); dos consolidaciones a la vez dejan las dos filas, y vale la última. Agregar al mismo puerto el último cambio de asignaciones de un periodo, con las 4 acciones de `research.md` §6, para no ampliar `RegistroDeCambios`, que es de escritura.

**Checkpoint**: el archivo se valida y se guarda.

---

## Phase 3: User Story 1 - Consolidar (Priority: P1) 🎯 MVP

**Goal**: consolidar y ver todas las carreras.

**Independent Test**: consolidar y ver las carreras con su estado, avance y horas.

- [X] T008 [US1] Crear `GEN/test/dominio/ConsolidadoDelPeriodoTest.java`, con esqueleto, y verlo fallar: todas las carreras con estado, avance y horas (FR-007); los totales (FR-008), sin contar retirados; una carrera sin propuestas sale con 0; el estado del parcial no cambia (FR-004). Después crear `GEN/main/dominio/ConsolidadoDelPeriodo.java`.
- [X] T009 [US1] Crear `GEN/test/aplicacion/ConsultarConsolidadoServicioTest.java` y `ConsolidarServicioTest.java`, con esqueleto, y verlos fallar: calcula al abrir, sin guardar (FR-011); toma las abiertas del panel (FR-010); sin distributivo responde vacío; consolidar la versión vigente (FR-001) guarda y registra (FR-002), se repite (FR-003) y admite abiertas (FR-005); sin distributivo o con el periodo cerrado se rechaza (FR-006). Después crear los casos de uso y sus servicios.
- [X] T010 [US1] Crear `GEN/main/adaptadores/entrada/ConsolidadoControlador.java` con el GET y el POST, con pruebas primero en `ConsolidadoContratoTest`. Actualizar `PermisosGeneracionTest`, de 14 a 18.
- [X] T011 [US1] Crear `SPA/consolidado/`, con la ruta en `frontend/src/rutas.tsx` y la entrada en `frontend/src/navegacion/Marco.tsx`. Escribir primero `Consolidado.test.tsx`: las carreras con su nombre, los totales, la nota de las unidades (FR-009), las abiertas con el enlace al panel, consolidar, sin distributivo, periodo cerrado y un error que no se presenta como vacío (FR-026).

**Checkpoint**: se consolida.

---

## Phase 4: User Story 2 - Saber si sigue al día (Priority: P1)

**Goal**: el aviso de desactualizado.

**Independent Test**: consolidar, cambiar una propuesta y ver el aviso.

- [X] T012 [US2] Agregar a `ConsolidadoDelPeriodoTest` y a `ConsultarConsolidadoServicioTest`, y verlos fallar: un cambio posterior lo deja desactualizado (FR-012); sin cambios, no; sin consolidar, no; al repetir, deja de estarlo; adjuntar y justificar no lo dejan desactualizado (FR-013). Después implementarlo.
- [X] T013 [US2] Agregar a `Consolidado.test.tsx`, y verlo fallar: el aviso de desactualizado, que pide repetir. Después implementarlo.

**Checkpoint**: el consolidado no miente.

---

## Phase 5: User Story 3 - Adjuntar el informe sustentado (Priority: P1)

**Goal**: el PDF asociado a la versión.

**Independent Test**: adjuntar un PDF y descargarlo idéntico.

- [X] T014 [US3] Crear `GEN/test/aplicacion/InformeSustentadoServicioTest.java`, con esqueleto, y verlo fallar: adjuntar a una versión consolidada (FR-014) guarda el archivo fuera de la base (FR-016), la fila y el registro; sin consolidar se rechaza; si falla el almacén no queda fila (FR-022); reemplazar conserva el anterior (FR-018); descargar devuelve el mismo archivo (FR-019) y comprueba la huella (FR-020); sin informe, 404; un archivo que falta o cambió, el error del catálogo. Después crear los casos de uso y el servicio.
- [X] T015 [US3] Agregar a `ConsolidadoControlador` el PUT y el GET del informe, con pruebas primero en `ConsolidadoContratoTest`: el multipart, los errores, `application/pdf` y un `Content-Disposition` que no use el nombre recibido.
- [X] T016 [US3] Subir a 11 MB `frontend/nginx.conf`, con su comentario al día (por el informe sustentado y su envoltura; Cargas y `datos` siguen con sus límites), y el multipart de `backend/apps/distributivo/src/main/resources/application.yaml`. Declarar en ese archivo `mdd.archivos.raiz: ${MDD_ARCHIVOS_RAIZ:/var/mdd/archivos}`, y agregar el bean de `AlmacenEnDisco` en `GEN/main/infraestructura/ConfiguracionGeneracion.java`.
- [X] T017 [US3] Crear la carpeta con dueño 10001 en `deploy/docker/java.Dockerfile`, y el volumen `archivos` en `compose.yaml` y `deploy/equipo/compose.demo.yml`, solo en `distributivo`. Documentarlo en `deploy/equipo/README.md`.
- [X] T018 [US3] Agregar a `Consolidado.test.tsx`, y verlo fallar: adjuntar, con nombre, tamaño, quién y cuándo (FR-021); más de 10 MB se avisa antes de subir; un error del servidor se muestra; reemplazar; descargar. Después implementarlo, con el `bodySerializer` y el `blob` de las plantillas.

**Checkpoint**: el informe queda asociado a la versión.

---

## Phase 6: User Story 4 - Quién puede hacerlo (Priority: P2)

**Goal**: los permisos.

**Independent Test**: un coordinador recibe 403.

- [X] T019 [US4] Agregar a `ConsolidadoContratoTest` los permisos (FR-023, FR-024): Consolidación consolida y adjunta; el Vicerrectorado y el Administrador leen y descargan; un coordinador recibe 403 en las cuatro.
- [X] T020 [US4] Agregar a `Consolidado.test.tsx`: sin permiso de escritura no salen «Consolidar» ni «Adjuntar», sí «Descargar»; un coordinador no ve la entrada del menú.

**Checkpoint**: la historia está completa.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T021 [P] Agregar `frontend/e2e/consolidado.spec.ts`, con la API simulada: consolidar, adjuntar y axe en cada estado.
- [X] T022 Crear `deploy/probar_consolidado.sh` y agregarlo a `.github/workflows/perfil-consolidado.yml`, según `quickstart.md`, con un PDF de 10 MB, subido también por el puerto de la SPA para pasar por nginx, la base y el volumen limpios al terminar, y sin que el archivo pase por los registros (FR-027).
- [X] T023 Agregar a `deploy/equipo/probar_equipo.sh` que `distributivo` puede escribir en el volumen.
- [X] T024 Crear `specs/037-consolidar-parciales/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` completo;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck con una copia nueva en el scratchpad y Gitleaks en modo `git` sobre un clon, con `.gitleaks.toml`;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #54`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [ ] T025 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion`, los 8 contenedores sanos con la versión nueva y el volumen creado). Comprobación del autor con SC-004, medida con cronómetro. PR de evidencia con `Closes #54`, y borrar las ramas.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 a T004 antes del código. T005 a T007 no dependen entre sí.
- **US1:** depende de la fase 2.
- **US2:** depende de T009.
- **US3:** depende de T005, T006 y T010.
- **US4:** depende de T010 y T015.
- **Cierre:** al final.

## Parallel Example

```text
Después de T004: T005 (PDF), T006 (almacén) y T007 (repositorio) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 3 dan el primer escenario del issue.
- **Después:** el aviso, el informe y los permisos. Cada fase deja el sistema en verde.
