---

description: "Tareas de F02-H01: abrir el periodo académico con su versión de normativa"
---

# Tasks: Abrir el periodo académico con su versión de normativa

**Input**: documentos de diseño en `specs/015-abrir-periodo/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/academico.md](contracts/academico.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-35, D-65, D-67, D-69, ADR-012, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); MD-10, la pregunta 4 de §9.2 y el glosario §7.3 («Periodo académico», «Asignación normativa») del modelo de dominio.

**Organization**:
- **Fundacional:** el contrato y los errores, el dominio del periodo, la migración V2, el repositorio, el registro y la configuración del servicio.
- **US1:** abrir el periodo con su versión de normativa.
- **US2:** avanzar el estado del periodo.
- **US3:** un periodo cerrado no cambia.
- **US4:** la pantalla «Periodos».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2, US3 o US4.
- Rutas relativas a la raíz. Abreviaturas: `ACA` = `backend/servicios/academico/src`, `JACA` = `ACA/main/java/ec/mdd/academico`, `PACA` = `ACA/test/java/ec/mdd/academico`, `IACA` = `ACA/integrationTest/java/ec/mdd/academico` y `DATOS` = `backend/apps/datos/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura; adaptadores e `infraestructura/` con Spring. `ArquitecturaTest` de Académico y `ModulosTest` del proceso `datos` lo comprueban.
- **Soporte (principio II, research §1):** `PeriodoAcademico` (record) y `EstadoPeriodo` (enum) con sus reglas; sin agregado elaborado ni eventos.
- **Límites (principio V, research §3 y §4):** Académico solo usa `ec.mdd.normativa.puertos.entrada`, desde un adaptador de salida. Nunca llama a Normativa dentro de una transacción propia.
- **Principio VII:** un periodo cerrado no cambia; cada registro, corrección, apertura y cambio de estado deja su `registro_cambio`.
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers y PostgreSQL; contrato con swagger-request-validator; 60 % de líneas por módulo (D-32).
- **Idioma:** identificadores del dominio, mensajes y JavaDoc en español (principio XII). Se dice «periodo académico», nunca «ciclo» (MD-10).
- **Formato:** `./gradlew spotlessApply` y `npm run formato` antes de cada commit.
- **Evidencia y commits:** la evidencia va en `specs/015-abrir-periodo/evidencia.md`; los commits llevan `Refs #32`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/015-abrir-periodo/evidencia.md` con estas secciones: entorno; base (contrato, dominio, migración y repositorio); escenario «Abrir el periodo» (US1); «Estados del periodo» (US2); «Periodo cerrado» (US3); pantalla «Periodos» (US4); cierre; equipo de demostración; notas.
- [X] T002 Cambiar `backend/servicios/academico/build.gradle`: plugin `mdd.contrato`; `spring-boot-starter-jdbc`, `tools.jackson.core:jackson-databind` y `spring-security-core`, como Normativa; `implementation project(':servicios:normativa')`, con un comentario que diga que solo usa su interfaz `api` (research §4, como Usuarios con Roles).

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato y errores (principio IV, research §5 y §8)

- [X] T003 Crear `contratos/academico/openapi.yaml` según contracts/academico.md, con el estilo de `contratos/normativa/openapi.yaml`:
  - comentario de cabecera que cite F02-H01, RF-13, RF-19, D-69 y la pregunta 4;
  - esquemas `EstadoPeriodo`, `PeriodoAcademico` (`version` opcional, entero ≥ 1), `PeriodoNuevo`, `DatosDelPeriodo`, `VersionDelPeriodo` y `EstadoDeDestino`; `codigo` con `pattern: '^[A-Za-z0-9._-]{1,20}$'`; `nombre` con `minLength: 1` y `maxLength: 120`; `inicio` y `fin` con `format: date`; sin `allOf` en los cuerpos;
  - las seis operaciones con su `x-mdd-permiso` (`DISTRIBUTIVO:LECTURA` o `DISTRIBUTIVO:ESCRITURA`) y sus respuestas, incluidas 401, 403, 500 y 503 de `contratos/comun/`;
  - la respuesta `Conflicto` que nombra los cinco códigos `ACADEMICO_*`.

  Agregar los cinco códigos a `contratos/comun/errores.md` con su estado, título, descripción y cuándo. Agregar `academico` a `generar:api` en `frontend/package.json` (`src/api/generado/academico.d.ts`). Regenerar las interfaces (`./gradlew :servicios:academico:openApiGenerate`) y los tipos (`npm run generar:api`); `npm run verificar:ruptura` sigue en verde. En `contratos/normativa/openapi.yaml`, agregar a la descripción de `asociarPeriodo`: «La SPA abre el periodo desde Académico (`abrirPeriodo`, D-69); esta operación no comprueba que el periodo exista».

### Dominio (research §1, data-model.md)

- [X] T004 Prueba primero, en `PACA/dominio/`, sin dobles:
  - `EstadoPeriodoTest`: `siguiente()` de cada estado; vacío en `CERRADO`;
  - `PeriodoAcademicoTest`: se crea en `PLANIFICACION`; un código con otro formato, un nombre vacío o de más de 120 caracteres, y un fin igual o anterior al inicio lanzan `DatoInvalido` con su campo (`codigo`, `nombre`, `fin`) y el motivo en español (por ejemplo «La fecha de fin (2026-01-01) debe ser posterior a la de inicio (2026-03-01)»); `corregido` devuelve el periodo nuevo y la lista de `CambioDeValor` (vacía si no cambia nada) y no cambia el código.
- [X] T005 Crear en `JACA/dominio/`: `EstadoPeriodo`, `PeriodoAcademico`, `CambioDeValor`, `DatoInvalido`, y las excepciones de las reglas `PeriodoCerrado`, `TransicionNoPermitida` (con el estado actual y el siguiente permitido) y `PeriodoSinVersion`. JavaDoc en español que cite RF-13, el glosario §7.3 y la pregunta 4. T004 pasa.

### Persistencia y registro (research §6, data-model.md)

- [X] T006a Prueba primero, en `IACA/AcademicoEsquemaIntegrationTest.java` (ampliar): después de las migraciones existen `periodo_academico` y `registro_cambio`; sus `check` rechazan un código con otro formato, un nombre vacío, `fin <= inicio`, un estado y una acción que no son de la lista. Tiene que fallar sin V2.
- [X] T006 Crear `ACA/main/resources/db/migration/academico/V2__periodos_academicos.sql` según data-model.md: `periodo_academico` y `registro_cambio`, con sus `check` (código, nombre, `fin > inicio`, estado y acciones `PERIODO_REGISTRADO`, `PERIODO_CORREGIDO`, `PERIODO_ABIERTO`, `PERIODO_ESTADO_CAMBIADO`). Comentario de cabecera que cite D-35, RF-13 y F02-H01. Sin datos precargados. T006a pasa.
- [X] T007 Crear los puertos de salida en `JACA/puertos/salida/`: `RepositorioPeriodos` (`todos()`, `porCodigo`, `bloquear(codigo)`, `registrar`, `guardar`), `RegistroDeCambios` y su `CambioDePeriodo` (usuario, momento, acción y detalle), `NormativaDelPeriodo` (`versionDe(codigo): OptionalInt`, `asociar(codigo, version, quien)`) y `Transaccion`.
- [X] T008 Prueba primero, en `IACA/` (Testcontainers, como en Normativa):
  - `RepositorioPeriodosJdbcIntegrationTest`: registra y lee un periodo igual, campo por campo; un código repetido no se guarda (`registrar` devuelve falso y la aplicación lanza `CodigoRepetido`); `todos()` ordena por inicio descendente; `guardar` cambia el estado y los datos; `bloquear` hace `SELECT … FOR UPDATE`;
  - `RegistroDeCambiosJdbcIntegrationTest`: guarda las cuatro acciones con su detalle en `jsonb`.

  Crear `JACA/adaptadores/salida/RepositorioPeriodosJdbc.java`, `RegistroDeCambiosJdbc.java` y `TransaccionSpring.java` con `JdbcClient`, como Normativa. T008 pasa.

### Puertos de entrada y configuración

- [X] T009 Crear los puertos de entrada en `JACA/puertos/entrada/` (interfaz `api` de Spring Modulith, con su `package-info.java` como el de Normativa): `ConsultarPeriodos` (`todos()`, `porCodigo`), `PeriodoConsultado` (el periodo y su versión, `OptionalInt`), `RegistrarPeriodo`, `CorregirPeriodo`, `AbrirPeriodo` y `CambiarEstadoPeriodo`, y las excepciones `PeriodoNoEncontrado`, `CodigoRepetido`, `PeriodoConOtraVersion` y `VersionInexistente`. JavaDoc que diga qué operación del contrato refleja cada uno.
- [X] T010 Prueba primero, en `PACA/adaptadores/salida/NormativaDesdePuertosTest.java`, con dobles de los puertos de Normativa: `PeriodoSinVersion` da vacío; `PeriodoConVersion` se traduce a `PeriodoConOtraVersion`; `VersionNoEncontrada`, a `VersionInexistente`. Crear `JACA/adaptadores/salida/NormativaDesdePuertos.java`, que solo importa `ec.mdd.normativa.puertos.entrada`. Las excepciones de Normativa se nombran con el paquete completo, porque Académico tiene `PeriodoSinVersion` y `CodigoRepetido` propios. T010 pasa.
- [X] T011 Crear `JACA/infraestructura/ConfiguracionAcademico.java` (los servicios, los adaptadores, el reloj y la transacción, como `ConfiguracionNormativa`). Prueba en `DATOS/test/java/ec/mdd/ModulosTest.java`: sigue en verde con la dependencia nueva, y un uso de una clase de `ec.mdd.normativa.dominio` desde Académico la haría fallar (comprobarlo una vez y anotarlo).

### Consulta (FR-009)

- [X] T012 Prueba primero, en `PACA/aplicacion/ConsultarPeriodosServicioTest.java` (Mockito): `todos()` trae cada periodo con la versión que da `NormativaDelPeriodo` (o vacía); `porCodigo` de uno que no existe lanza `PeriodoNoEncontrado`. Crear `JACA/aplicacion/ConsultarPeriodosServicio.java`. T012 pasa.
- [X] T013 Prueba primero, en `PACA/adaptadores/entrada/AcademicoContratoTest.java` (MockMvc y swagger-request-validator, con un `AcademicoEnMemoria` como `NormativaEnMemoria`): `listarPeriodos` y `consultarPeriodo` validan contra el contrato, con y sin `version`; un código que no existe da 404 `COMUN_RECURSO_NO_ENCONTRADO`. `PermisosAcademicoTest` (con `VerificadorDePermisos`): las seis operaciones exigen su `x-mdd-permiso`. `ErroresAcademicoTest`: los cinco códigos de `ErroresAcademico` están en `errores.md` con el mismo estado. Crear `JACA/adaptadores/entrada/AcademicoControlador.java` (implementa la interfaz generada, `@PreAuthorize` con `PERMISO_DISTRIBUTIVO:LECTURA` y `:ESCRITURA`) y `ErroresAcademico.java`. Las operaciones de escritura responden 501 hasta su historia (la interfaz generada no tiene métodos por defecto). T013 pasa.

**Checkpoint**: el contrato, el dominio, V2, los repositorios, la consulta y el adaptador hacia Normativa están listos; `./gradlew :servicios:academico:check :servicios:academico:integrationTest :apps:datos:test` en verde.

---

## Phase 3: User Story 1 - Abrir el periodo con su versión de normativa (Priority: P1) 🎯 MVP

**Goal**: registrar un periodo y abrirlo con su versión de normativa, que desde entonces no se puede editar (FR-001, FR-002, FR-003, FR-006, FR-008; SC-002).

**Independent Test**: registrar un periodo, abrirlo con una versión sin periodo, y comprobar que Académico y Normativa dicen la misma versión y que editarla da 409.

- [X] T014 [US1] Prueba primero, en `PACA/aplicacion/` (Mockito):
  - `RegistrarPeriodoServicioTest`: registra en `PLANIFICACION`, con el usuario y un `PERIODO_REGISTRADO` con código, nombre y fechas; un código repetido lanza `CodigoRepetido` sin registro;
  - `AbrirPeriodoServicioTest`: un periodo que no existe lanza `PeriodoNoEncontrado`; uno cerrado, `PeriodoCerrado`, sin llamar a Normativa; con la misma versión que ya tiene, termina sin llamar a `asociar` ni registrar; con otra, `PeriodoConOtraVersion`; un periodo en `EN_APROBACION` con otra versión, `PeriodoConOtraVersion`, y con la misma, sin cambios; si `asociar` lanza, no registra; con una versión inexistente, `VersionInexistente`; si abre, registra `PERIODO_ABIERTO` con la versión; y con `InOrder`, `asociar` se llama **fuera** de `Transaccion.ejecutar` y el registro, dentro (research §3).

  Crear `JACA/aplicacion/RegistrarPeriodoServicio.java` y `AbrirPeriodoServicio.java`. T014 pasa.
- [X] T015 [US1] Prueba primero, en `PACA/adaptadores/entrada/AcademicoContratoTest.java`: `registrarPeriodo` responde 201 con el periodo en `PLANIFICACION` y sin `version`; un código repetido, 409 `ACADEMICO_CODIGO_REPETIDO`; fin antes del inicio, 400 `COMUN_SOLICITUD_INVALIDA` con el campo `fin` y su motivo; `abrirPeriodo` responde 200 con la `version`; con otra versión, 409 `ACADEMICO_PERIODO_CON_VERSION`; con una versión inexistente, 400 con el campo `version`. Implementar las dos operaciones en `AcademicoControlador` (el usuario es el `sub` del token) y traducir `DatoInvalido` y `VersionInexistente` a `SolicitudInvalida` con su detalle. T015 pasa.
- [X] T016 [US1] Prueba de integración con la Normativa real, en `DATOS/integrationTest/java/ec/mdd/AbrirPeriodoIntegrationTest.java` (Testcontainers, el proceso `datos` completo):
  - abrir un periodo con una versión copiada de la 1 deja la asignación en Normativa (`ConsultarNormativa.periodo`), y editar esa versión lanza `VersionEnUso` (escenario 1, SC-002);
  - el registro de Normativa tiene `VERSION_ASOCIADA` y el de Académico `PERIODO_ABIERTO`, con el mismo usuario;
  - si el registro de Académico falla después de asociar (un `RegistroDeCambios` que lanza), la asignación de Normativa queda confirmada: cada servicio con su transacción (research §3);
  - dos aperturas simultáneas del mismo periodo con versiones distintas dejan una sola, y la otra lanza `PeriodoConOtraVersion`;
  - dos periodos abiertos con la misma versión quedan los dos con ella (escenario 3 de US1).

  Limpia lo que crea al terminar. T016 pasa.

**Checkpoint**: un periodo se registra y se abre por la API, y su versión queda fija.

---

## Phase 4: User Story 2 - Avanzar el estado del periodo (Priority: P1)

**Goal**: el estado avanza solo un paso a la vez, y salir de Planificación exige versión (FR-004, FR-008; SC-003).

**Independent Test**: avanzar un periodo abierto hasta Cerrado; saltar o retroceder da 409; pasar a En aprobación sin versión da 409.

- [X] T017 [US2] Prueba primero, en `PACA/dominio/PeriodoAcademicoTest.java`: `conEstado` con los 16 pares de estados de origen y destino: el mismo estado no cambia; el siguiente cambia; los demás lanzan `TransicionNoPermitida` con el actual y el siguiente; `PLANIFICACION` → `EN_APROBACION` sin versión lanza `PeriodoSinVersion`, y con versión cambia; desde `CERRADO`, cualquier destino distinto lanza `PeriodoCerrado`. Completar `conEstado` en `JACA/dominio/PeriodoAcademico.java`. T017 pasa.
- [X] T018 [US2] Prueba primero, en `PACA/aplicacion/CambiarEstadoPeriodoServicioTest.java` (Mockito): bloquea el periodo, pide la versión a `NormativaDelPeriodo` solo si está en `PLANIFICACION`, guarda y registra `PERIODO_ESTADO_CAMBIADO` con el estado anterior y el nuevo; el mismo estado no guarda ni registra. Crear `JACA/aplicacion/CambiarEstadoPeriodoServicio.java` (la lectura de la versión, antes de abrir su transacción). T018 pasa.
- [X] T019 [US2] Prueba primero, en `AcademicoContratoTest`: `cambiarEstadoPeriodo` a `EN_APROBACION` con versión responde 200; sin versión, 409 `ACADEMICO_PERIODO_SIN_VERSION`; de `PLANIFICACION` a `VIGENTE`, 409 `ACADEMICO_TRANSICION_NO_PERMITIDA` con la descripción del catálogo; un estado que no es del enum, 400. Implementar la operación en `AcademicoControlador`. T019 pasa.

**Checkpoint**: los estados avanzan en orden por la API.

---

## Phase 5: User Story 3 - Un periodo cerrado no cambia (Priority: P1)

**Goal**: corregir los datos de un periodo no cerrado y rechazar todo cambio de uno cerrado (FR-005, FR-006, FR-008; SC-004).

**Independent Test**: cerrar un periodo e intentar corregirlo, cambiar su estado y abrirlo con otra versión; todo da 409 y el periodo sigue igual.

- [X] T020 [US3] Prueba primero, en `PACA/aplicacion/CorregirPeriodoServicioTest.java` (Mockito): corrige nombre y fechas y registra `PERIODO_CORREGIDO` con cada cambio; sin cambios, no guarda ni registra; cerrado, `PeriodoCerrado` sin guardar. Crear `JACA/aplicacion/CorregirPeriodoServicio.java`. T020 pasa.
- [X] T021 [US3] Prueba primero, en `AcademicoContratoTest`: `corregirPeriodo` responde 200 con los datos nuevos; con un periodo cerrado, `corregirPeriodo`, `cambiarEstadoPeriodo` y `abrirPeriodo` responden 409 `ACADEMICO_PERIODO_CERRADO`; `consultarPeriodo` de un periodo cerrado responde 200 con todos sus datos. Implementar `corregirPeriodo` en `AcademicoControlador`. T021 pasa.
- [X] T022 [US3] Crear `deploy/probar_periodos.sh` con los 9 pasos de research §11, con el estilo de `probar_normativa.sh` (`iniciar`, `verificar`, la limpieza en un `trap` que borra en la base solo el periodo de prueba `prueba-<aleatorio>`, sus registros en Académico, su asignación y su `VERSION_ASOCIADA` en Normativa, y la versión que creó con sus filas, reglas, parámetros y registros). Agregar su paso a `.github/workflows/perfil-consolidado.yml`, después de `probar_normativa.sh`. Correrlo dos veces seguidas con el perfil reconstruido, `shellcheck` sin observaciones, y anotarlo.

**Checkpoint**: las tres historias del servidor pasan de punta a punta.

---

## Phase 6: User Story 4 - Ver y registrar los periodos en pantalla (Priority: P1)

**Goal**: la pantalla «Periodos» y «Normativa» sin el diálogo de asociar (FR-010, D-69; SC-001).

**Independent Test**: como Vicerrector, registrar, abrir y avanzar un periodo en pantalla; como Rector, verlo sin acciones.

- [X] T023 [US4] Prueba primero, en `frontend/src/pantallas/periodos/Periodos.test.tsx` (Vitest con `instalarApiSimulada`; datos en `datosDePrueba.ts`):
  - la tabla con código, nombre, inicio, fin, estado en español y versión o «Sin versión»;
  - «Registrar periodo» envía código, nombre y fechas; un 400 marca el campo con su motivo; un 409 se muestra con su título y su descripción;
  - «Corregir» abre el diálogo sin el código, y no aparece en un periodo cerrado;
  - «Abrir» solo en Planificación sin versión, con la lista de versiones de `listarVersiones`; al confirmar, la fila muestra la versión;
  - «Pasar a En aprobación» deshabilitado sin versión, con el motivo; pasar a Cerrado pide confirmación;
  - con solo `DISTRIBUTIVO:LECTURA`, la tabla sin acciones.

  Crear `frontend/src/pantallas/periodos/` (`Periodos.tsx`, `DialogosDePeriodo.tsx`, `consultas.ts`, `estados.ts` con los nombres en español), la ruta en `frontend/src/rutas.tsx` y la entrada «Periodos» con `DISTRIBUTIVO:LECTURA` en `frontend/src/navegacion/Marco.tsx`. T023 pasa.
- [X] T024 [US4] Prueba primero, en `frontend/src/pantallas/normativa/`: «Normativa» ya no ofrece «Asociar a un periodo» y sigue mostrando «Rige: <periodo>». Quitar el diálogo de `DialogosDeVersion.tsx`, su uso en `Normativa.tsx` y en `consultas.ts`, y sus pruebas (D-69). T024 pasa; las demás pruebas de «Normativa» siguen en verde.
- [X] T025 [US4] Prueba con Playwright, en `frontend/e2e/periodos.spec.ts`, con la API simulada: `vicerrectorado.demo` registra un periodo, lo abre eligiendo una versión y lo pasa a En aprobación; axe-core sin fallas WCAG 2.1 AA en la lista y en los diálogos de registrar, abrir y cerrar (solo Chromium). Quitar de `frontend/e2e/normativa.spec.ts` lo del diálogo de asociar, si lo usa.

**Checkpoint**: las cuatro historias pasan de punta a punta.

---

## Phase 7: Cierre

- [X] T026 Correr:
  - en el backend, `./gradlew check integrationTest`;
  - en el frontend, `npm run lint`, `formato:verificar`, `tipos`, `test -- --run --coverage`, `test:e2e` y `verificar:ruptura`;
  - `shellcheck` sobre `probar_periodos.sh`, y Gitleaks sobre un clon en el scratchpad, montado con `:ro,Z` (nunca el repositorio);
  - con el perfil local reconstruido con la rama: `probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh`, `probar_spa.sh`, `probar_normativa.sh`, `probar_periodos.sh` dos veces seguidas y `deploy/equipo/probar_equipo.sh`.

  Anotar el tiempo de `listarPeriodos` y `abrirPeriodo` (meta < 200 ms). Todo en verde, con el 60 % de líneas en Académico. Hacer commit y push, abrir el PR en borrador con `Refs #32` y marcarlo listo **después** de que terminen sus runs. Comprobar los checks, `perfil-consolidado` y `5 · imágenes`.
- [ ] T027 Completar `evidencia.md` y la «Definición de terminado» del PR. Fusionarlo con *squash* **con la aprobación del autor**, con la rama al día con `main`. Después:
  - esperar el despliegue automático en el PC («Sano en N s»), con `academico` en V2;
  - el autor entra con `vicerrectorado.demo` en `https://mdd.localhost`, registra un periodo, lo abre con la versión que elija y lo pasa a En aprobación en menos de 2 minutos (SC-001); en «Normativa», esa versión dice que rige el periodo y ya no se edita;
  - en la base del equipo, los registros de Académico y Normativa con su usuario;
  - abrir el PR de evidencia con `Closes #32` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup (fase 1)**: T001 y T002 son libres.
- **Foundational (fase 2)**: T003 después de T002; T004 antes que T005; T006a antes que T006; T006 y T007 después de T005; T008 después de T006 y T007; T009 después de T005; T010 después de T007 y T009; T011 después de T008 y T010; T012 después de T011; T013 después de T003 y T012.
- **US1 (fase 3)**: depende de la fase 2. T014 antes que T015; T016 después de T015.
- **US2 (fase 4)**: depende de la fase 2. T017 antes que T018; T019 después de T018.
- **US3 (fase 5)**: T020 depende de la fase 2; T021 después de T020; T022 después de T016, T019 y T021 (el script recorre las tres).
- **US4 (fase 6)**: T023 y T024 dependen de T003 (la pantalla se prueba con la API simulada); T025 después de T023 y T024.
- **Cierre (fase 7)**: T026 y T027 dependen de todo.

### Parallel Opportunities

- T006 (migración) y T009 (puertos de entrada) tocan archivos distintos: su orden es libre, después de T005.
- La SPA (T023 a T025) puede empezar después de T003, en paralelo con el servidor.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el dominio, V2, los repositorios y el adaptador hacia Normativa.
2. Registrar y abrir el periodo por la API.
3. **Validar:** Académico y Normativa dicen la misma versión, y la versión ya no se edita.

### Incremental

1. **US2:** los estados en orden.
2. **US3:** el periodo cerrado, y el script de punta a punta con las tres.
3. **US4:** la pantalla, y «Normativa» sin el diálogo de asociar.
4. **Cierre:** evidencia, PR, fusión y despliegue.

---

## Notes

- La versión del periodo vive solo en Normativa: Académico la consulta, no la copia (research §2).
- Los plazos (RN-23), el tablero de preparación, el Responsable de consolidación y la carga 3 por archivo son de otras historias (F02-H02, F02-H03, F01).
- `probar_normativa.sh` no cambia: sigue usando la operación HTTP de Normativa (D-69).
- Hacer commit por tarea o por grupo lógico en `015-abrir-periodo`, con `Refs #32`.
