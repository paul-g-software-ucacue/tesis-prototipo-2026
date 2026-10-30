---

description: "Tareas de F01-H05: configurar las 10 cargas en su orden"
---

# Tasks: Configurar las 10 cargas en su orden

**Input**: documentos de diseño en `specs/026-orden-de-cargas/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cargas.md](contracts/cargas.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: las 4 aclaraciones del autor del 2026-10-27 (en la spec); ADR-022 (el lenguaje publicado, que se amplía sin cambiar la decisión), D-70, D-71, D-72, D-74, MD-13 y ADR-009 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-56, RF-18 y RF-57 de la ERS; el glosario §7 («Carga obligatoria», «Historial de cargas», «Puerto de fuente de datos»), sin cambios. Plan aprobado por el autor el 2026-10-29.

**Organization**:
- **Fundacional:** el contrato, el catálogo, `tieneDatos` en el lenguaje publicado y `OrdenDeLasCargas`.
- **US1, el orden; US2, el estado:** los dueños y Cargas.
- **US3:** «sin proyectos».
- **US4:** las 10 de prueba en orden.
- **US5:** la fuente de datos.
- **Pantalla y cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US5.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `PLC` = `backend/plataforma/lenguaje-cargas/src`;
  - `CAR` = `backend/servicios/cargas/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `SPA` = `frontend/src/pantallas/plantillas`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** `OrdenDeLasCargas` es dominio puro, sin Spring. Los casos de uso van en `aplicacion`; la consulta a Docentes, en `adaptadores/salida`. `ArquitecturaTest` lo comprueba.
- **Límites (principio V):** Cargas usa solo `ec.mdd.plataforma.cargas` y los `puertos.entrada` de los demás; `ModulosTest` lo comprueba.
- **Las dependencias** salen solo del `dependeDe` del diccionario (FR-002); ninguna lista de dependencias se escribe en el código.
- **Datos (principio IX):** el estado solo dice si hay datos; no van datos personales a los mensajes ni a los logs; solo datos de prueba ficticios.
- **Mensajes:** en «tú» (D-74). Cada código nuevo lleva sus 8 columnas (D-72).
- **Pruebas:**
  - Mockito solo en la aplicación;
  - los repositorios con Testcontainers (postgres:18.6-alpine);
  - el contrato con swagger-request-validator;
  - los dobles de los repositorios que ya existen suman el método nuevo;
  - 60 % de líneas por módulo.
- **Evidencia y commits:** como en las historias anteriores. La evidencia va en `specs/026-orden-de-cargas/evidencia.md` y los commits llevan `Refs #31`.

---

## Phase 1: Setup

- [ ] T001 Crear `specs/026-orden-de-cargas/evidencia.md` con las secciones de la historia anterior (base, dueños, orden y estado, «sin proyectos», fuente de datos, pantalla, cierre, notas) y la línea de base: `./gradlew build integrationTest` en verde en `main` (`78362d3`) y los 15 scripts de `deploy/probar_*.sh`.

---

## Phase 2: Foundational (bloquea a todas las historias)

- [ ] T002 Ampliar el contrato `contratos/cargas/openapi.yaml` según [contracts/cargas.md](contracts/cargas.md):
  - `consultarEstadoDeLasCargas` (`GET /api/cargas/estado`, `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`), con `EstadoDeLasCargas`, `EstadoDeCarga`, `EstadoDeLaCarga` y `CargaQueFalta`;
  - `confirmarSinProyectos` (`POST /api/cargas/sin-proyectos`, `CARGAS:ESCRITURA`), con `ConfirmacionSinProyectos`: `periodo` obligatorio, `maxLength: 10`, `pattern: '^[0-9]{4}-(I|II)$'`, `additionalProperties: false`; 201 y 200 con `EntradaDeHistorial`, 400 y 409;
  - el 409 `CARGAS_CARGAS_ANTERIORES_PENDIENTES` en `validarArchivo` y `confirmarCarga`;
  - `EntradaDeHistorial`: `archivo` y `hash` dejan de ser obligatorios, suma `periodo` opcional; `ResultadoDeHistorial` suma `SIN_PROYECTOS`.

  Regenerar los modelos y comprobar que el contrato pasa su validación.
- [ ] T003 [P] Agregar a `contratos/comun/errores.md` `CARGAS_CARGAS_ANTERIORES_PENDIENTES` (409, Negocio), `CARGAS_PERIODO_NO_ENCONTRADO` (400, Validación) y `CARGAS_PERIODO_CON_PROYECTOS` (409, Negocio), con las 8 columnas de [contracts/cargas.md](contracts/cargas.md), y sus entradas en el catálogo de Cargas (`EntradaCatalogo`). `VerificadorDeCatalogo` los comprueba.
- [ ] T004 [P] Ampliar el lenguaje publicado (`PLC/main/java/ec/mdd/plataforma/cargas/`). Primero las pruebas, en `PLC/test/java/ec/mdd/plataforma/cargas/AplicadorPorTraductoresTest.java`, sin dobles:
  - `tieneDatos(carga)` devuelve lo del traductor de esa carga;
  - una carga que no atiende lanza `IllegalArgumentException`.

  Después, `boolean tieneDatos()` abstracto en `TraductorDeCarga`, con su JavaDoc («al menos un registro vigente, entrado por archivo o por pantalla»), `tieneDatos(String carga)` en `AplicadorDeCargas` y su implementación en `AplicadorPorTraductores`. El traductor de prueba de `MotorDelLoteTest` lo implementa.
- [ ] T005 [P] Antes de crear los tipos, comprobar que las 4 entradas de research §12 están en el glosario (§7 de `sdd/modelo_dominio.md`). Crear el dominio del orden en `CAR/main/java/ec/mdd/cargas/dominio/`: `OrdenDeLasCargas`, `EstadoDeCarga`, `EstadoDeLaCarga` (`PENDIENTE`, `CARGADA`, `SIN_PROYECTOS`) y `CargaQueFalta`. Primero las pruebas, en `CAR/test/java/ec/mdd/cargas/dominio/OrdenDeLasCargasTest.java`, sin dobles y con el diccionario real:
  - sin datos, la 04 tiene que faltar la 02, y la 01, la 03 y la 07 no les falta nada;
  - la 08 con la 01 y la 02 pero sin la 06: falta solo la 06;
  - el estado `SIN_PROYECTOS` de la 9 con periodos válidos y sin datos;
  - la 9 con datos es `CARGADA` aunque haya periodos «sin proyectos»;
  - `SIN_PROYECTOS` no habilita a ninguna carga;
  - el orden de `faltan` es el de la ERS.

**Checkpoint**: el contrato, el catálogo, el lenguaje publicado y el orden están listos; todavía nadie los usa.

---

## Phase 3: US1 y US2 · Los dueños dicen si tienen datos (Priority: P1) 🎯 MVP

**Goal**: cada uno de los 10 traductores implementa `tieneDatos()` con lo vigente de [data-model.md](data-model.md).

**Independent Test**: en cada dueño, con Testcontainers, `tieneDatos()` es falso sin registros, falso solo con inactivos y verdadero con uno vigente.

- [ ] T006 [P] [US1] Académico: `tieneDatos()` en `TraductorEstructura` (una unidad activa), `TraductorCarreras` (una carrera `VIGENTE`), `TraductorPeriodos` (un periodo), `TraductorAsignaturas` (una asignatura activa), `TraductorOferta` (una asignatura-paralelo `OFERTADA`) y `TraductorMatriculas` (un registro), en `ACA/main/java/ec/mdd/academico/aplicacion/cargas/`. Cada uno usa un método nuevo de existencia en su repositorio de `ACA/main/java/ec/mdd/academico/puertos/salida/`, con su `select exists (…)` en el adaptador JDBC. Primero las pruebas, en `ACA/integrationTest/java/ec/mdd/academico/AplicadorAcademicoIntegrationTest.java`: sin registros, solo inactivos y con uno vigente, para las 6 cargas.
- [ ] T007 [P] [US1] Docentes: `tieneDatos()` en `TraductorDocentesContratados` (un docente `ACTIVO` o con `LICENCIA`), `TraductorCargos` (un cargo `VIGENTE`) y `TraductorProyectos` (un proyecto), en `DOC/main/java/ec/mdd/docentes/aplicacion/cargas/`, con métodos de existencia en `RepositorioDocentes`, `RepositorioCargos` y `RepositorioProyectos` y sus adaptadores JDBC. Primero las pruebas, en `DOC/integrationTest/java/ec/mdd/docentes/AplicadorDocentesIntegrationTest.java`: sin registros, solo inactivos o finalizados y con uno vigente.
- [ ] T008 [P] [US1] Normativa: `tieneDatos()` en `TraductorPolitica` (una versión), en `NOR/main/java/ec/mdd/normativa/aplicacion/cargas/`, con un método de existencia en `RepositorioVersiones`. Primero las pruebas: en `NOR/integrationTest/java/ec/mdd/normativa/AplicadorNormativaIntegrationTest.java`, verdadero con la política 2024 precargada (F04-H01); en `TraductorPoliticaTest`, con Mockito.

**Checkpoint**: Cargas ya puede preguntarle a cada dueño si tiene datos.

---

## Phase 4: US1 y US2 · El orden y el estado en Cargas (Priority: P1)

**Goal**: validar y confirmar respetan el orden, y `consultarEstadoDeLasCargas` da el estado de las 10.

**Independent Test**: con una base sin carreras, `validarArchivo` de la 04 responde 409 con `carreras` en sus detalles, sin leer el archivo y sin entrada en el historial; `GET /api/cargas/estado` da las 10 con su estado.

- [ ] T009 [US1] Imponer el orden en `CAR/main/java/ec/mdd/cargas/aplicacion/ValidarArchivoServicio.java` y `ConfirmarCargaServicio.java`, con la excepción nueva `CAR/main/java/ec/mdd/cargas/puertos/entrada/CargasAnterioresPendientes.java` (lleva las cargas que faltan) y su traducción en `ErroresCargas`, que pone un detalle por carga (`campo`: el código; `motivo`: el número y el nombre). Primero las pruebas, con Mockito, en `ValidarArchivoServicioTest` y `ConfirmarCargaServicioTest`:
  - con una carga anterior sin datos, lanzan la excepción antes de llamar a la fuente de datos;
  - no escriben en el historial;
  - con todo cargado, siguen igual que antes.

  También en `ErroresCargasTest` y en las pruebas de contrato.

  Los dobles de los dueños en las pruebas de Cargas (`AplicadorDePrueba` y los contextos WebMvc) responden «tiene datos» por defecto; solo las pruebas del orden lo cambian. Si la consulta del orden falla, validar y confirmar responden `COMUN_ERROR_INTERNO` sin leer el archivo (prueba con Mockito).
- [ ] T010 [US2] Crear `ConsultarEstadoDeLasCargas` (puerto de entrada), `ConsultarEstadoDeLasCargasServicio` (aplicación) y `EstadoControlador` (`adaptadores/entrada`, operación `consultarEstadoDeLasCargas`) en `CAR/main/java/ec/mdd/cargas/`. El servicio arma `OrdenDeLasCargas` con `DuenosDeLasCargas` y, para la 9, con las confirmaciones «sin proyectos» que valen (en esta fase, ninguna; T014 las suma). Primero las pruebas:
  - `ConsultarEstadoDeLasCargasServicioTest` con Mockito;
  - la prueba de contrato del 200;
  - `PermisosCargasTest`: lectura sí, sin permiso 403.
- [ ] T011 [US1] [US2] Ampliar `backend/apps/datos/src/integrationTest/java/…/ConfirmarLasDiezCargasIntegrationTest.java`:
  - desde la base vacía, la 04 antes que la 02 responde 409 con `carreras`, sin entrada en el historial;
  - tras las 10 en orden, el estado da las 10 `CARGADA`;
  - una carrera registrada por pantalla sin carga habilita la 04;
  - el tiempo de `consultarEstadoDeLasCargas` queda por debajo de 2 s (SC-003, con holgura).

  Anotar las mediciones en la evidencia.

- [ ] T012 [US1] Ajustar `deploy/probar_subida.sh` al orden: si no hay una unidad orgánica activa, crear una temporal antes de subir las carreras y borrarla al terminar, como `probar_reglas_carga.sh`. Correrlo en una base vacía y en una con datos; pasar shellcheck v0.11.0.

**Checkpoint**: el primer escenario del issue funciona por la API.

---

## Phase 5: US3 · «Sin proyectos» (Priority: P1)

**Goal**: confirmar «sin proyectos» para un periodo, con su validez calculada.

**Independent Test**: con los periodos 2026-I y 2026-II cargados y sin proyectos, confirmar 2026-II da 201 y la 9 `SIN_PROYECTOS (2026-II)`; tras cargar la 09 de prueba, la 9 es `CARGADA` y otro «sin proyectos» para 2026-II da 409.

- [ ] T013 [US3] Crear la migración `CAR/main/resources/db/migration/cargas/V3__sin_proyectos.sql` según [data-model.md](data-model.md):
  - la columna `periodo varchar(10)` con su patrón;
  - `SIN_PROYECTOS` en el `check` de `resultado`;
  - `archivo` y `hash` nulos solo en `SIN_PROYECTOS`, y `periodo` solo en `SIN_PROYECTOS`;
  - el índice único parcial `(carga, periodo) where resultado = 'SIN_PROYECTOS'`.

  Ampliar `HistorialDeCargas` (puerto) con `sinProyectos()` y `confirmarSinProyectos(periodo, quien)`, que devuelve la entrada nueva o la que ya estaba, y `HistorialDeCargasJdbc`, `HistorialEnMemoria` y `EntradaDeHistorial` (dominio, con `periodo` opcional y `archivo` y `hash` opcionales). Primero las pruebas, en `CAR/integrationTest/…/HistorialDeCargasJdbcIntegrationTest.java`:
  - una confirmación nueva y la repetida, que no duplica;
  - las entradas viejas siguen válidas;
  - la base rechaza un `SIN_PROYECTOS` sin periodo o un `APLICADA` con periodo.
- [ ] T014 [US3] Crear el puerto de salida `CAR/main/java/ec/mdd/cargas/puertos/salida/ProyectosDelPeriodo.java` (`hayAsignables(periodo)`, lanza `PeriodoNoEncontrado`) y su adaptador `CAR/main/java/ec/mdd/cargas/adaptadores/salida/ProyectosDesdePuertos.java`, que llama a `ConsultarProyectos.listar(periodo, null, null)` de Docentes y traduce su `DatoInvalido` en `periodo`. Sumar al estado de T010 las confirmaciones que valen (`HistorialDeCargas.sinProyectos()` filtradas por `hayAsignables`). Primero las pruebas:
  - `ProyectosDesdePuertosTest`, con un doble escrito a mano de `ConsultarProyectos`;
  - en `ConsultarEstadoDeLasCargasServicioTest`, una confirmación que vale y otra que ya no.
- [ ] T015 [US3] Crear `ConfirmarSinProyectos` (puerto de entrada), las excepciones `PeriodoNoEncontrado` y `PeriodoConProyectos` (`CAR/main/java/ec/mdd/cargas/puertos/entrada/`), `ConfirmarSinProyectosServicio` (aplicación) y la operación `confirmarSinProyectos` en `EstadoControlador` (201 con la nueva, 200 con la que ya estaba). Las excepciones se traducen en `ErroresCargas`. Primero las pruebas:
  - **`ConfirmarSinProyectosServicioTest`, con Mockito:** periodo sin proyectos, con proyectos (409), inexistente (400) y repetido (200);
  - **pruebas de contrato, válidos e inválidos (principio XIV):** `2026-I` y `2026-II` válidos; vacío, `2026-III`, `26-I`, de 11 caracteres y `2026-i` dan 400 `COMUN_SOLICITUD_INVALIDA`;
  - **`PermisosCargasTest`:** escritura sí; solo lectura o sin permiso, 403.
- [ ] T016 [US3] Ampliar `ConfirmarLasDiezCargasIntegrationTest`, con el proceso `datos` real:
  - con la 01 a la 08 confirmadas y sin proyectos, «sin proyectos» para 2026-II da 201 y el estado de la 9 es `SIN_PROYECTOS` con `2026-II`;
  - después de confirmar la 09 de prueba, la 9 es `CARGADA` y «sin proyectos» para 2026-II da 409;
  - `2027-I` da 400 `CARGAS_PERIODO_NO_ENCONTRADO`;
  - el historial de la 9 conserva la confirmación.
  - un proyecto `SUSPENDIDO` en un periodo confirmado «sin proyectos» no anula la confirmación.

**Checkpoint**: el tercer escenario del issue funciona por la API.

---

## Phase 6: US5 · La fuente de datos (Priority: P1)

**Goal**: las reglas no dependen del formato ni del origen (RF-57).

**Independent Test**: `ArquitecturaTest` falla si `dominio` o `aplicacion` importan Apache POI o un lector; la misma carga desde una fuente de prueba da el mismo informe y la misma vista previa que el CSV.

- [ ] T017 [P] [US5] Renombrar `CAR/main/java/ec/mdd/cargas/puertos/salida/LectorDeArchivo.java` a `FuenteDeDatos.java`, con su JavaDoc: el puerto de fuente de datos del glosario; el archivo es su adaptador inicial; Moodle o SIGA serían otro (RF-57). Actualizar sus usos, `LectorPorFormato` (el adaptador de archivos), las pruebas y los comentarios.
- [ ] T018 [US5] Agregar a `CAR/test/java/ec/mdd/cargas/ArquitecturaTest.java` la regla «ni `..cargas.dominio..` ni `..cargas.aplicacion..` dependen de `org.apache.poi..` ni de `..cargas.adaptadores..`». Comprobar que falla si se agrega a propósito una importación de POI en `aplicacion` y se quita. Crear `CAR/test/java/ec/mdd/cargas/aplicacion/FuenteDeDatosIntercambiableTest.java`: una `FuenteDeDatos` escrita a mano que entrega una `TablaDeDatos` sin leer bytes, con los mismos registros que un CSV de prueba. El informe de validación y la vista previa de `ValidarArchivoServicio` tienen que ser iguales a los del CSV leído por `LectorPorFormato`.

**Checkpoint**: el cuarto escenario del issue queda probado en cada build.

---

## Phase 7: Pantalla (US1, US2, US3)

**Goal**: «Plantillas de carga» muestra el estado, deshabilita lo que no se puede subir y permite «Sin proyectos…».

**Independent Test**: con Vitest y la API simulada:
- con estado «Pendiente» y `faltan: [carreras]`, la 04 muestra el chip «Pendiente», el botón deshabilitado y «Falta la carga de Carreras» asociado con `aria-describedby`;
- el diálogo «Sin proyectos…» confirma un periodo y actualiza el estado.

- [ ] T019 [US2] Agregar a `SPA/consultas.ts` `useEstadoDeLasCargas` (React Query) y `confirmarSinProyectos`, con sus tipos del contrato; al confirmar una carga o «sin proyectos», invalidar el estado (FR-008). Agregar el estado a `SPA/datosDePrueba.ts`.
- [ ] T020 [US1] [US2] En `SPA/Plantillas.tsx`:
  - la columna «Estado», con un chip «Cargada», «Pendiente» o «Sin proyectos» y sus periodos;
  - «Subir archivo» deshabilitado si `faltan` no está vacío, con «Falta la carga de …» (varias, en una frase) asociado con `aria-describedby`.

  Un error al consultar el estado se muestra con `AvisoDeError`, sin inventar un estado. Primero las pruebas, en `SPA/Plantillas.test.tsx`:
  - cada estado;
  - el botón deshabilitado con su descripción;
  - el botón se habilita tras confirmar la carga anterior, sin recargar;
  - el error del estado;
  - un usuario de solo lectura ve el estado pero no los botones.
- [ ] T021 [US3] Crear `SPA/SinProyectos.tsx`: el diálogo que abre «Sin proyectos…» en la fila de la 9, solo con `CARGAS:ESCRITURA`. Tiene una lista con los periodos de los ciclos cargados y la validación del periodo con el patrón del contrato antes de enviar (principio XIV). Muestra el 409 y el 400 con `traducirError`. Mostrar las confirmaciones `SIN_PROYECTOS` en `SPA/HistorialDeLaCarga.tsx`, con el periodo y sin archivo ni huella. Primero las pruebas, en `SPA/Plantillas.test.tsx`:
  - confirmar, y que el estado cambie;
  - un periodo con proyectos (409);
  - un periodo inválido no se envía;
  - la entrada en el historial.
- [ ] T022 [US1] [US3] Ampliar `frontend/e2e/plantillas.spec.ts` con rutas simuladas: el botón deshabilitado con su texto y el diálogo «Sin proyectos…», con axe sobre la tabla y sobre el diálogo ya visible (`opacity: 1`). Correr `lint`, `tipos`, `formato`, Vitest y Playwright.

**Checkpoint**: la pantalla cubre los escenarios 1 y 3 del issue.

---

## Phase 8: US4 · Punta a punta y cierre

- [ ] T023 [US4] Crear `deploy/probar_orden_cargas.sh`, siguiendo `deploy/probar_confirmar_carga.sh`:
  0. comprobar que la base no tiene los datos de prueba;
  1. la 04 antes de la 02: 409 con `carreras` y sin entrada en el historial;
  2. el estado inicial;
  3. las cargas 01 a 08 en orden;
  4. «sin proyectos» para 2026-II: 201, y la repetida, 200;
  5. el estado de la 9 `SIN_PROYECTOS`;
  6. la 09 de prueba y el estado `CARGADA`;
  7. «sin proyectos» para 2026-II: 409;
  8. `2027-I`: 400;
  9. `docente.demo`: 403;
  10. limpiar por códigos y por `momento >= inicio`.

  Agregarlo a `.github/workflows/perfil-consolidado.yml`. Pasar shellcheck v0.11.0.
- [ ] T024 Verificación completa:
  - `./gradlew build integrationTest`;
  - los 16 scripts, con `probar_orden_cargas.sh` dos veces seguidas en un perfil reconstruido;
  - `probar_equipo.sh`;
  - shellcheck;
  - Gitleaks sobre un clon;
  - ningún valor de prueba en los logs;
  - la cobertura por módulo;
  - los criterios de terminado de la constitución (§6).

  Completar la evidencia. Commit, push, PR en borrador con `Refs #31` y marcarlo listo cuando sus comprobaciones pasen.
- [ ] T025 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion`, los 8 contenedores sanos con la versión nueva, la migración V3 en el registro de `datos`). Después: la comprobación del autor, el PR de evidencia con `Closes #31` y borrar las ramas.

---

## Dependencies & Execution Order

- **Fundacional (T002 a T005)** antes de todo. T003, T004 y T005 son [P] entre sí, después de T002.
- **Fase 3 (T006 a T008)** depende de T004. Sus tres tareas son [P].
- **Fase 4:** T009 y T010 dependen de T005 y de la fase 3; T011, de T009 y T010; T012, de T009, y va en el mismo PR para que la CI no falle.
- **Fase 5:**
  - T013 depende de T002;
  - T014 depende de T010 y T013;
  - T015 depende de T014;
  - T016 depende de T011 y T015.
- **Fase 6 (T017, T018)** solo depende de la fundacional y puede ir en paralelo con las fases 3 a 5. T018 va después de T017.
- **Fase 7** depende de T010 y T015 (el contrato real); T019 primero.
- **Fase 8** al final.

## Parallel Example

```text
Después de T002: T003, T004 y T005 a la vez.
Después de T004: T006 (Académico), T007 (Docentes) y T008 (Normativa) a la vez, y T017 en Cargas.
```

## Implementation Strategy

- **MVP:** las fases 2 a 4 dan el primer escenario del issue (el orden) y el estado por la API.
- **Después, una por una:**
  1. «sin proyectos» (fase 5);
  2. la fuente de datos (fase 6);
  3. la pantalla (fase 7);
  4. el cierre (fase 8).

  Cada fase deja el sistema funcionando y en verde.
