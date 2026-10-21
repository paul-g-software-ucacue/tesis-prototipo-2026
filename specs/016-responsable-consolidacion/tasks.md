---

description: "Tareas de F05-H03: designar al Responsable de consolidación del periodo"
---

# Tasks: Designar al Responsable de consolidación del periodo

**Input**: documentos de diseño en `specs/016-responsable-consolidacion/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/roles.md](contracts/roles.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-13, D-16, D-49, D-62, D-63, D-64, D-65, D-69, ADR-003, ADR-016, ADR-018 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md) y en los ADR del Lean Inception; el glosario §7.3 («Responsable de consolidación», «Periodo académico»).

**Organization**:
- **Fundacional:** el contrato, el dominio de Roles, su migración y sus repositorios, y los eventos de Académico hacia Roles (con la clave del proceso `datos` en cada entorno).
- **US1:** designar al Responsable de un periodo, con sus permisos en el token.
- **US2:** quién designa (`DISTRIBUTIVO:ESCRITURA`).
- **US3:** es un rol, no un cargo.
- **US4:** la pantalla «Periodos».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2, US3 o US4.
- Rutas relativas a la raíz. Abreviaturas: `ROL` = `backend/servicios/roles/src`, `JROL` = `ROL/main/java/ec/mdd/roles`, `PROL` = `ROL/test/java/ec/mdd/roles`, `IROL` = `ROL/integrationTest/java/ec/mdd/roles`, `JACA` = `backend/servicios/academico/src/main/java/ec/mdd/academico` y `PACA` = `backend/servicios/academico/src/test/java/ec/mdd/academico`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura. `ArquitecturaTest` y `ModulosTest` (procesos `identidad` y `datos`) lo comprueban.
- **Límites (principio V, research §1):** Roles no lee tablas de Académico; recibe `academico.PeriodoActualizado` por ADR-018. Ninguna transacción cruza servicios.
- **Principio VII:** cada designación queda en `roles.registro_cambio`; un periodo cerrado no admite designaciones.
- **Secretos:** la clave del proceso `datos` nunca va al repositorio ni al chat; en local y en la CI, un valor ficticio (`cambiar-en-local-…`).
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers; contrato con swagger-request-validator; 60 % de líneas por módulo (D-32).
- **Idioma, formato y commits:** como en las historias anteriores; la evidencia va en `specs/016-responsable-consolidacion/evidencia.md` y los commits llevan `Refs #45`.

---

## Phase 1: Setup

- [ ] T001 Crear `specs/016-responsable-consolidacion/evidencia.md` con estas secciones: entorno; base (contrato, dominio, migración, eventos); «Designación» (US1); «Quién designa» (US2); «Es un rol, no un cargo» (US3); pantalla (US4); cierre; equipo de demostración; notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV, research §4)

- [ ] T002 Cambiar `contratos/roles/openapi.yaml` según contracts/roles.md: esquemas `Responsable` y `Designacion`; `listarResponsables` (`DISTRIBUTIVO:LECTURA`), `designarResponsable` (`DISTRIBUTIVO:ESCRITURA`, 200 · 400 · 404 · 409), `listarDocentes` (`DISTRIBUTIVO:ESCRITURA`) y `recibirEvento` en `/interno/roles/eventos` (sobre de `contratos/comun/eventos.yaml`, 204 · 400), con `example` en el parámetro `periodo`; el comentario de cabecera cita F05-H03, D-13 y ADR-003. Agregar `ROLES_PERIODO_CERRADO` a `contratos/comun/errores.md`. Regenerar (`openApiGenerate`, `generar:api`); `verificar:ruptura` sigue en verde.

### Dominio de Roles (research §1 a §3, data-model.md)

- [ ] T003 Prueba primero, en `PROL/dominio/`, sin dobles:
  - `EstadoConocidoTest`: `masAvanzado` con los 16 pares (gana el posterior); `cerrado()` solo en `CERRADO`;
  - `PeriodoConocidoTest`: `conEstado` nunca retrocede (un evento viejo no cambia la copia);
  - `DesignacionTest`: con el periodo cerrado, `PeriodoCerrado`; un usuario sin `DOCENTE`, `DesignacionInvalida` en `usuario` con «Solo se puede designar a un usuario con el rol Docente»; quien designa, `DesignacionInvalida` con «No puedes designarte a ti mismo: la consolidación y la revisión van en manos distintas»; con el mismo usuario que la anterior, sin cambios; los nombres se normalizan como en F05-H01.
- [ ] T004 Crear en `JROL/dominio/`: `EstadoConocido`, `PeriodoConocido`, `Designacion`, `DesignacionInvalida` y `PeriodoCerrado`, con JavaDoc en español que cite D-13, ADR-003 y research §1. T003 pasa.

### Persistencia de Roles (data-model.md)

- [ ] T005 Prueba primero, en `IROL/RolesEsquemaIntegrationTest.java` (ampliar): después de las migraciones existen `periodo_conocido`, `designacion_responsable` y `evento_recibido`; sus `check` rechazan un código o un estado inválidos; `registro_cambio` acepta `DESIGNAR_RESPONSABLE`; una designación de un periodo desconocido viola la clave foránea. Tiene que fallar sin la migración.
- [ ] T006 Crear `ROL/main/resources/db/migration/roles/V6__responsable_de_consolidacion.sql` según data-model.md, con el comentario de cabecera que cite F05-H03, D-13, ADR-003 y ADR-018. Sin datos precargados. T005 pasa.
- [ ] T007 Prueba primero, en `IROL/RepositoriosJdbcIntegrationTest.java` (ampliar): la copia de un periodo se guarda y se actualiza; una designación se guarda, se reemplaza y se lee; `bloquearPeriodo` toma el candado; `periodosConDesignacionAbierta(usuario)` solo trae los no cerrados; `registrarSiEsNuevo` devuelve falso con un `idEvento` repetido. Crear los puertos de salida (`RepositorioDesignaciones`, `RepositorioPeriodosConocidos`, `RepositorioEventosRecibidos`) y sus adaptadores JDBC en `JROL/adaptadores/salida/`. T007 pasa.

### Eventos de Académico hacia Roles (research §1 y §6)

- [ ] T008 Prueba primero, en `PACA/aplicacion/` (Mockito): `RegistrarPeriodoServicioTest` y `CambiarEstadoPeriodoServicioTest` llaman a `AvisosDelPeriodo.actualizado` **dentro** de la transacción con el periodo nuevo; el mismo estado y un registro rechazado no avisan. Crear el puerto `JACA/puertos/salida/AvisosDelPeriodo.java`, su adaptador `PublicadorDeAvisosSpring` (un `EventoEntreProcesos` de tipo `academico.PeriodoActualizado`, origen `academico`, destino `roles`, contenido `{codigo, estado}`, como `PublicadorDeEventosSpring` de Generación) y llamarlo en los dos servicios. T008 pasa; `AbrirPeriodoIntegrationTest` sigue en verde.
- [ ] T009 Agregar `plataforma:eventos` a `backend/apps/datos/build.gradle` y su configuración a `backend/apps/datos/src/main/resources/application.yaml`, como `distributivo`: `eventos_datos`, plazo, reenvío, destino `roles` (`${MDD_URI_IDENTIDAD:http://identidad:8080}/interno/roles/eventos`), token de proceso, `proceso: datos` y `MDD_CLAVE_PROCESO_DATOS`. En `backend/apps/identidad/src/main/resources/application.yaml`, `mdd.usuarios.procesos.datos.clave`. Ampliar `backend/apps/datos/src/integrationTest/java/ec/mdd/EsquemasIntegrationTest.java` con `eventos_datos.event_publication` (prueba primero). Agregar la clave a `.env.ejemplo` y a `compose.yaml` (si la pasa explícita) y a `.github/workflows/perfil-consolidado.yml` con un valor ficticio.
- [ ] T010 Prueba primero, en `PROL/aplicacion/RegistrarPeriodoConocidoServicioTest.java` (Mockito): un evento nuevo guarda la copia; uno repetido no cambia nada; uno viejo no la hace retroceder; un tipo que no es `academico.PeriodoActualizado` lanza `EventoNoReconocido`. Y en `PROL/adaptadores/entrada/EventosRolesContratoTest.java`: `POST /interno/roles/eventos` responde 204 con un token de proceso, también con un evento repetido; 400 `COMUN_EVENTO_NO_RECONOCIDO` con otro tipo; 403 con un token de usuario. Crear el servicio, el puerto de entrada y `EventosRolesControlador`, como el receptor de Usuarios (registro idempotente y copia en la misma transacción). T010 pasa.
- [ ] T011 Equipo (research §6): en `deploy/equipo/compose.demo.yml`, `MDD_CLAVE_PROCESO_DATOS` para `datos` (obligatoria) y para `identidad`; en `deploy/equipo/preparar.sh`, generarla en un `.env` nuevo y, si el `.env` ya existe y no la tiene, agregarla al final sin tocar el resto. Prueba primero en `deploy/equipo/probar_equipo.sh`, paso 11: con un `.env` sin la clave, `preparar.sh` la agrega y las demás líneas siguen iguales. `shellcheck` sin observaciones.

**Checkpoint**: el contrato, el dominio, la migración y los eventos están listos; un periodo registrado en Académico aparece en la copia de Roles. `./gradlew check integrationTest` en verde.

---

## Phase 3: User Story 1 - Designar al Responsable de un periodo (Priority: P1) 🎯 MVP

**Goal**: designar, reemplazar y registrar; el designado recibe el rol en su token mientras su periodo no esté cerrado (FR-001, FR-002, FR-004 a FR-006, FR-008, FR-009).

**Independent Test**: designar a un docente en un periodo en Planificación; su autorización incluye `RESPONSABLE_CONSOLIDACION`; al cerrarse el periodo, ya no.

- [ ] T012 [US1] Prueba primero, en `PROL/aplicacion/DesignarResponsableServicioTest.java` (Mockito): designa y registra `DESIGNAR_RESPONSABLE` con el anterior y el nuevo; reemplaza; el mismo usuario no registra; periodo desconocido lanza `PeriodoDesconocido`; cerrado, `PeriodoCerrado`; no docente o quien designa, `DesignacionInvalida`; bloquea el periodo antes de leer la designación. Crear `DesignarResponsable` (puerto de entrada) y su servicio. T012 pasa.
- [ ] T013 [US1] Prueba primero, en `PROL/aplicacion/AutorizacionServiciosTest.java` (ampliar): un docente con una designación en un periodo no cerrado recibe `RESPONSABLE_CONSOLIDACION` y sus permisos (`CONSOLIDACION:ESCRITURA`, `DISTRIBUTIVO:FIRMA`…); con su único periodo cerrado, no; sin designaciones, igual que antes; con el rol asignado a mano, lo conserva. Cambiar `ConsultarAutorizacionServicio`. T013 pasa; `MatrizD16IntegrationTest` sigue en verde.
- [ ] T014 [US1] Prueba primero, en `PROL/adaptadores/entrada/RolesContratoTest.java` (ampliar): `designarResponsable` responde 200 con el `Responsable`; 400 con el detalle en `usuario`; 404 con un periodo desconocido; 409 `ROLES_PERIODO_CERRADO`; `listarResponsables` y `listarDocentes` validan contra el contrato. `ErroresRolesTest` con el código nuevo. Implementar las tres operaciones (en `RolesControlador` o en uno nuevo de designaciones) y la entrada del catálogo. T014 pasa.
- [ ] T015 [US1] Prueba de integración en `backend/apps/identidad/src/integrationTest/java/ec/mdd/` (Testcontainers, el proceso `identidad` completo): recibir `academico.PeriodoActualizado` por HTTP con un token de proceso, designar a un docente, y que la autorización interna (`/interno/roles/autorizaciones/{usuario}`) tenga el rol; recibir el cierre y que ya no lo tenga.

**Checkpoint**: la designación funciona por la API y llega al token.

---

## Phase 4: User Story 2 - Quién designa (Priority: P1)

**Goal**: solo con `DISTRIBUTIVO:ESCRITURA` (FR-003).

- [ ] T016 [US2] Ampliar `PROL/adaptadores/entrada/PermisosRolesTest.java`: las tres operaciones nuevas de `/api/` exigen su `x-mdd-permiso` (`VerificadorDePermisos`), y `RolesContratoTest`: con solo `USUARIOS_ROLES:ESCRITURA` (el Administrador), designar responde 403. T016 pasa.

---

## Phase 5: User Story 3 - Es un rol, no un cargo (Priority: P1)

**Goal**: la designación no es un cargo ni una actividad (FR-007).

- [ ] T017 [US3] Anotar en la evidencia que la designación solo vive en `roles.designacion_responsable`: no hay cargos ni actividades en el sistema todavía (F03-H05, F07), y ninguna tabla de horas la referencia. Agregar a la sección «Notas» de la evidencia que F07 comprueba con su cálculo que no suma horas, y dejarlo en el issue de F07 como dependencia.

---

## Phase 6: User Story 4 - Ver y designar en pantalla (Priority: P1)

**Goal**: la columna y el diálogo en «Periodos» (FR-010; SC-001).

- [ ] T018 [US4] Prueba primero, en `frontend/src/pantallas/periodos/Periodos.test.tsx` (Vitest con la API simulada): la columna «Responsable de consolidación» con el usuario o «Sin designar»; con escritura, «Designar responsable <código>» en un periodo no cerrado (no en uno cerrado), con la lista de `listarDocentes`; al confirmar, la fila muestra al elegido; un 400 muestra su motivo y un 409 su título; un 404 muestra «El periodo todavía no está disponible para designar. Inténtalo en unos segundos.»; sin escritura, la columna sin acción. Agregar `apiRoles` a las consultas de la pantalla (`listarResponsables`, `listarDocentes`, `designarResponsable`) y el diálogo en `DialogosDePeriodo.tsx`. T018 pasa.
- [ ] T019 [US4] Prueba con Playwright, en `frontend/e2e/periodos.spec.ts`: el Vicerrector designa un responsable; axe-core sin fallas en el diálogo «Designar al Responsable de consolidación de <código>».

**Checkpoint**: las cuatro historias pasan.

---

## Phase 7: Punta a punta y cierre

- [ ] T020 Ampliar `deploy/probar_periodos.sh` con un paso 10 (escenario 1, SC-002 y SC-003): con el periodo de prueba registrado, esperar (hasta 30 s) a que Roles lo conozca; designar a un no docente (`rectorado.demo`) da 400 y al propio Vicerrector, 400; designar a `docente.demo` da 200; `docente.demo` inicia sesión y su sesión trae `CONSOLIDACION:ESCRITURA`; `listarResponsables` lo muestra; después de cerrar el periodo (paso 6), `docente.demo` inicia sesión otra vez y su sesión ya no trae ese permiso; designar en el periodo cerrado da 409 `ROLES_PERIODO_CERRADO`. El registro de Roles tiene `DESIGNAR_RESPONSABLE`. La limpieza borra la designación, la copia del periodo, los eventos recibidos y las publicaciones de `eventos_datos` del periodo de prueba. Correrlo dos veces seguidas y anotarlo.
- [ ] T021 Correr todo, como T026 de F02-H01: backend, SPA, los 6 scripts con el perfil reconstruido, `probar_equipo.sh`, `shellcheck` y Gitleaks sobre un clon. Hacer commit y push, abrir el PR en borrador con `Refs #45` y marcarlo listo después de sus runs.
- [ ] T022 Completar la evidencia y el PR. **Antes de fusionar**, el autor corre en su terminal `sudo deploy/equipo/preparar.sh` desde la rama y comprueba que `/opt/mdd/.env` tiene `MDD_CLAVE_PROCESO_DATOS` (sin mostrar su valor). Fusionar con *squash* con la aprobación del autor. Después:
  - esperar el despliegue («Sano en N s»), con `roles` en V6;
  - el autor registra un periodo nuevo y designa a `docente.demo` en menos de 1 minuto (SC-001); entra con `docente.demo` y ve «Periodos» en el menú;
  - en la base del equipo, la copia del periodo, la designación y su registro;
  - abrir el PR de evidencia con `Closes #45` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 primero; T003 antes que T004; T005 antes que T006; T007 después de T004 y T006; T008 después de T002; T009 después de T008; T010 después de T007; T011 después de T009.
- **US1:** depende de la fase 2. T012 antes que T014; T013 después de T007; T015 después de T010, T012 y T013.
- **US2:** T016 después de T014. **US3:** T017 en cualquier momento.
- **US4:** T018 después de T002; T019 después de T018.
- **Cierre:** T020 después de T015 y T018; T021 y T022 al final.

### Parallel Opportunities

- La SPA (T018, T019) puede empezar después de T002, con la API simulada.
- Los eventos de Académico (T008, T009) y el dominio de Roles (T003 a T007) tocan servicios distintos.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el dominio y la migración de Roles.
2. Los eventos de Académico y la copia en Roles.
3. Designar y el rol en el token. **Validar:** un docente designado recibe los permisos y los pierde al cerrar.

### Incremental

1. **US2 y US3:** el permiso y la nota del cargo.
2. **US4:** la pantalla.
3. **Cierre:** punta a punta, `preparar.sh` en el equipo, PR, fusión y despliegue.

---

## Notes

- La consolidación y la firma F1 (F09, F10) usarán `listarResponsables` para limitar sus operaciones a los periodos del Responsable (D-64).
- El periodo `001` del equipo, anterior al despliegue, no llega a Roles: designar en él responde «no encontrado» (research §1).
- Hacer commit por tarea o por grupo lógico en `016-responsable-consolidacion`, con `Refs #45`.
