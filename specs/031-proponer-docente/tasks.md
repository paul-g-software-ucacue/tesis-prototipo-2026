---

description: "Tareas de F06-H03: proponer un docente por asignatura-paralelo"
---

# Tasks: Proponer un docente por asignatura-paralelo

**Input**: documentos de diseño en `specs/031-proponer-docente/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**:
- las 6 aclaraciones del autor del 2026-10-31 (en la spec);
- D-77 y D-78, aprobadas por el autor el 2026-10-31 y por escribir en T001;
- D-01, D-16, D-19, D-64, D-65, D-69, D-70, D-72, D-74, D-75, ADR-010 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-24, RF-25, RF-56, RN-19 y RNF-EQU-01 de la ERS;
- el modelo táctico: `Horas` con hasta dos decimales, ya aplicado.

Plan aprobado por el autor el 2026-10-31.

**Organization**:
- **Fundacional:** las decisiones, los contratos, los errores, la migración y el dominio.
- **US1, crear; US2, proponer; US3, justificar; US4, el parcial; US5, las horas reales.**
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US5.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `CAR` = `backend/servicios/cargas/src`;
  - `SPA` = `frontend/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** el agregado y `Justificacion` son dominio puro, sin Spring ni HTTP. `ArquitecturaTest` lo comprueba.
- **Límites (principio V):** sin llaves foráneas a otros esquemas; Generación recibe sus propios valores.
- **Rutas internas (ADR-023):** solo el rol `PROCESO`; un token de usuario recibe 403 y el API Gateway no las enruta.
- **Trazabilidad (principio VII):** cada cambio, en `generacion.registro_cambio`, en la misma transacción. El texto de la justificación no va al registro.
- **Datos (principio IX):** ningún texto de justificación ni ficha en los logs. Solo datos de prueba ficticios.
- **Decimales:** horas y puntajes con `BigDecimal` y `compareTo` (D-77).
- **La justificación la decide el servidor:** la pantalla solo manda el docente y el texto.
- **Mensajes:** en «tú» (D-74); los errores, del catálogo con sus 8 columnas (D-72), y un solo estado por código.
- **Validación (principio XIV):** `^[0-9A-Z]{1,13}$` para el docente y de 10 a 500 caracteres para el texto, en el contrato, el servidor y la pantalla.
- **Pruebas:**
  - Mockito solo en la aplicación;
  - fuera de ahí, dobles escritos a mano;
  - el cliente entre procesos, contra `ReceptorSimulado`;
  - los repositorios, contra PostgreSQL en `integrationTest`;
  - los filtros, con tokens con reclamos reales;
  - tipos de la SPA con `npm run tipos`, no con `tsc -p .`.

---

## Phase 1: Setup

- [X] T001 Escribir D-77 y D-78 en `docs/decisiones.md`, con sus filas en el índice, y pasar «Siguiente número libre» a D-79 · ADR-024 (research §10).

---

## Phase 2: Foundational

**Purpose**: los contratos, los errores, la migración y el dominio. Bloquea las historias.

- [X] T002 Contratos, según [contracts/generacion.md](contracts/generacion.md):
  - `contratos/generacion/openapi.yaml`: las 5 operaciones, sus esquemas y `bajoElMinimo`, `distributivoCreado` y `propuesta` en `Sugerencia`;
  - `contratos/cargas/openapi.yaml`: `consultarPeriodoListo`;
  - `contratos/academico/openapi.yaml`: `consultarOfertaDelPeriodo`, y `periodoCerrado` en `ParaleloDeLaOferta`;
  - `contratos/comun/errores.md`: los 8 códigos, con sus 8 columnas.

  Actualizar el comentario de cabecera de cada contrato. Regenerar las interfaces con Gradle y la SPA con `generar:api`.
- [X] T003 Crear `GEN/main/resources/db/migration/generacion/V3__distributivo.sql`, según [data-model.md](data-model.md), con la restricción que deja `texto` solo en `EXCEPCION`.
- [X] T004 [P] Pruebas sin dobles en `GEN/test/java/ec/mdd/generacion/dominio/JustificacionTest.java`:
  - alcanza el mínimo, también igual a él y con otra escala: `AFINIDAD`, sin texto;
  - bajo el mínimo en tronco común: `DISPONIBILIDAD`, sin texto;
  - bajo el mínimo sin tronco común: `EXCEPCION`, con su texto sin espacios en los extremos;
  - sin texto, solo espacios o con 9 caracteres: `JustificacionObligatoria`;
  - con 10 y con 500 caracteres pasa; con 501, no;
  - un texto en los dos primeros casos no se conserva.
- [X] T005 [P] Implementar `Justificacion`, `TipoDeJustificacion` y `JustificacionObligatoria` en `GEN/main/java/ec/mdd/generacion/dominio/`. Hace pasar T004.
- [X] T006 [P] Pruebas sin dobles en `GEN/test/java/ec/mdd/generacion/dominio/VersionDistributivoTest.java`:
  - nace con un parcial en elaboración por carrera;
  - proponer deja una sola propuesta, y otra la reemplaza;
  - una carrera nueva recibe su parcial con la primera propuesta;
  - una versión que no está editable, o un parcial que no está en elaboración, rechaza;
  - quitar deja el paralelo sin propuesta, y quitar sin propuesta rechaza;
  - el avance: ofertadas, con propuesta y completo;
  - una retirada no cuenta para el avance, y vuelve a contar si se oferta.
- [X] T007 [P] Implementar `VersionDistributivo`, `ParcialCarrera`, `Propuesta` y `AvanceDelParcial` en `GEN/main/java/ec/mdd/generacion/dominio/`, con `periodoCerrado` en `AsignaturaParalelo`. Hace pasar T006.

- [X] T008 Pruebas en `backend/plataforma/errores-nucleo/src/test/java/ec/mdd/plataforma/errores/nucleo/AlcanceDeCarreraTest.java`, con un solo permiso: sobre todo, ve todas; solo lo propio con carrera, esa; sin carrera, `CarreraSinIndicar`; sin el permiso, ve todas. Implementar `AlcanceDeCarrera.propia(autoridades, permiso)`.

**Checkpoint**: el dominio de Generación en verde.

---

## Phase 3: User Story 1 - Crear el distributivo (Priority: P1)

**Goal**: el Vicerrector crea el distributivo de un periodo listo.

**Independent Test**: pulsar «Crear distributivo» con un periodo listo y ver que el tablero dice que ya existe.

- [X] T009 [P] [US1] Pruebas de `consultarPeriodoListo` en `CAR/test/java/ec/mdd/cargas/adaptadores/entrada/InternoCargasContratoTest.java`: los tres valores, 404, token de usuario 403, y que no trae cargas ni listas. Implementarlo en un controlador interno, sobre `ConsultarPreparacion`.
- [X] T010 [P] [US1] Pruebas de `consultarOfertaDelPeriodo` en `ACA/test/java/ec/mdd/academico/adaptadores/entrada/InternoAcademicoContratoTest.java`: todos los paralelos, también retirados, con horas efectivas; 404; token de usuario 403. Sumar `periodoCerrado` a `consultarParaleloDeLaOferta`. Implementarlo en `ConsultarParaleloDeLaOfertaServicio` e `InternoAcademicoControlador`.
- [X] T011 [US1] Sumar a `DatosDesdeProcesoDatosTest`: `periodoListo`, `ofertaDelPeriodo` y `periodoCerrado`. Implementarlos en `DatosParaSugerencia` y `DatosDesdeProcesoDatos`.
- [X] T012 [US1] Pruebas de `GEN/test/java/ec/mdd/generacion/aplicacion/CrearDistributivoServicioTest.java`, con Mockito:
  - crea la versión 1 y un parcial por carrera con oferta, y lo registra;
  - un periodo que no está listo, o cerrado: `PeriodoNoListo`, sin guardar;
  - un periodo que no existe: `PeriodoNoEncontrado`;
  - con distributivo: `DistributivoRepetido`, sin consultar nada más;
  - un fallo entre procesos no guarda nada.
- [X] T013 [US1] Implementar `CrearDistributivo`, `ConsultarDistributivo`, sus excepciones, `CrearDistributivoServicio`, los puertos `RepositorioDistributivos`, `RegistroDeCambios` y `Transaccion`, y sus adaptadores, con sus beans. `RepositorioDistributivosJdbc` traduce el choque de la llave única a `DistributivoRepetido`. Hace pasar T012.
- [X] T014 [US1] Prueba de `RepositorioDistributivosJdbc` en `GEN/integrationTest/java/ec/mdd/generacion/`, contra PostgreSQL: guardar y leer; un segundo distributivo del periodo lanza `DistributivoRepetido`; `texto` solo con excepción.
- [X] T015 [US1] Pruebas de contrato y permisos en `GEN/test/java/ec/mdd/generacion/adaptadores/entrada/DistributivoContratoTest.java` y `PermisosGeneracionTest`, con los errores en `ErroresGeneracionTest`. Implementar `GEN/main/java/ec/mdd/generacion/adaptadores/entrada/DistributivoControlador.java` y los códigos en `ErroresGeneracion`.
- [X] T016 [US1] Pruebas en `SPA/pantallas/preparacion/Preparacion.test.tsx`: crea y dice que ya existe; deja de ofrecer crear; un error del catálogo; quien no escribe no lo ve. Implementarlo en `SPA/pantallas/preparacion/Preparacion.tsx`, con `apiGeneracion`, quitando el aviso de que aún no se crea.

**Checkpoint**: el distributivo se crea desde la pantalla.

---

## Phase 4: User Story 2 - Proponer un docente (Priority: P1)

**Goal**: el coordinador elige un docente desde la sugerencia.

**Independent Test**: con un distributivo, proponer un candidato y verlo en la oferta.

- [X] T017 [US2] Pruebas de `GEN/test/java/ec/mdd/generacion/aplicacion/ProponerDocenteServicioTest.java`, con Mockito:
  - guarda con el puntaje recalculado en el servidor, y lo registra;
  - reemplazar registra al docente anterior;
  - quitar, y quitar sin propuesta;
  - sin distributivo, con el periodo cerrado, otra carrera, un paralelo retirado o inexistente, y un docente no activo: su excepción, sin guardar;
  - sin horas suficientes se guarda, con `horasSuficientes` en falso;
  - un fallo entre procesos no guarda nada.
- [X] T018 [US2] Implementar `ProponerDocente`, sus excepciones y `ProponerDocenteServicio`, con su bean. Hace pasar T017.
- [X] T019 [US2] Sumar a `RepositorioDistributivosJdbc` y a su prueba: guardar y reemplazar una propuesta con la misma llave, quitarla, y las del periodo.
- [X] T020 [US2] Pruebas en `GEN/test/java/ec/mdd/generacion/adaptadores/entrada/PropuestaContratoTest.java` y `PermisosGeneracionTest`, con tokens con reclamos reales: 200, 204, 400 por cada campo, válidos e inválidos, 403 de otra carrera y sin carrera, 404, cada 409, y 502 y 504. También un rol con `OFERTA:ESCRITURA:PROPIO` y sin permisos de lectura de la oferta: 403 con otra carrera. Implementar `proponerDocente` y `quitarPropuesta` en `DistributivoControlador` y sus códigos.

---

## Phase 5: User Story 3 - Justificar la propuesta (Priority: P1)

**Goal**: los docentes bajo el mínimo, con su justificación.

**Independent Test**: proponer bajo el mínimo con y sin texto, y en tronco común.

- [X] T021 [US3] Sumar a `SugerenciaTest`: `bajoElMinimo` trae a los que no lo alcanzan, ordenados igual, con sus horas; `fueraPorElMinimo` es su tamaño; en tronco común, vacía. Implementarlo en `Sugerencia`.
- [X] T022 [US3] Sumar a `ConsultarSugerenciaServicioTest` y `SugerenciaContratoTest`: `bajoElMinimo`, `distributivoCreado` y la propuesta actual. Implementarlo en `ConsultarSugerenciaServicio` y `SugerenciaControlador`.
- [X] T023 [US3] Sumar a `ProponerDocenteServicioTest`: los tres casos de la justificación, también con un texto que no se conserva; sin texto, el 400 con el campo `texto`.

---

## Phase 6: User Story 4 - Ver el parcial (Priority: P1)

**Goal**: el avance del parcial y la propuesta de cada fila.

**Independent Test**: proponer en todos los paralelos de una carrera y ver «completo».

- [X] T024 [US4] Pruebas de `GEN/test/java/ec/mdd/generacion/aplicacion/ConsultarParcialesServicioTest.java`, con Mockito: el avance por carrera; sin distributivo, `creado` en falso; una retirada marcada que no cuenta; un paralelo nuevo pendiente; solo su carrera. Implementar `ConsultarParciales`, su servicio y su bean.
- [X] T025 [US4] Pruebas de `consultarParciales` en `PropuestaContratoTest` y `PermisosGeneracionTest`: 200, sin distributivo, un coordinador solo ve su carrera, y sin carrera 403. Implementarlo en `DistributivoControlador`.

---

## Phase 7: User Story 5 - Ver las horas reales (Priority: P1)

**Goal**: las horas salen de las propuestas.

**Independent Test**: proponer a un docente en dos carreras y ver las horas de la otra.

- [X] T026 [US5] Pruebas de `GEN/test/java/ec/mdd/generacion/dominio/HorasDelPeriodoTest.java`, sin dobles: suma por docente, con decimales; otras carreras aparte; sin el paralelo consultado; sin retirados; con las horas actuales de la oferta; sin distributivo, vacío.
- [X] T027 [US5] Implementar `HorasDelPeriodo`, borrar `HorasAsignadas`, `SinAsignacionesTodavia` y su prueba, y ajustar `ConsultarSugerenciaServicio`, `ProponerDocenteServicio` y sus pruebas. Hace pasar T026.

**Checkpoint**: los escenarios del issue y las horas reales responden por la API.

---

## Phase 8: La pantalla

**Purpose**: proponer y ver el parcial.

- [X] T028 [US2] Agregar `useParciales`, `proponerDocente` y `quitarPropuesta` en `SPA/pantallas/oferta/consultas.ts`, sus datos en `SPA/pantallas/oferta/datosDePrueba.ts`, y `ESCRITURA_DE_LA_PROPUESTA` en `SPA/sesion/funcionalidades.ts`: `OFERTA:ESCRITURA`.
- [X] T029 [US2] Pruebas en `SPA/pantallas/oferta/Oferta.test.tsx`:
  - «Proponer» en cada candidato, para quien escribe la oferta;
  - sin distributivo, lo dice y no lo ofrece;
  - el Vicerrector ve la lista sin proponer;
  - proponer recarga la oferta;
  - el aviso de horas insuficientes;
  - un error del catálogo no cierra el diálogo.
- [X] T030 [US3] Pruebas en el mismo archivo:
  - «Docentes bajo el mínimo» cerrada, y no aparece en tronco común;
  - proponer bajo el mínimo pide el texto, con su contador y su largo;
  - guardar sin texto no deja, y un 400 lo muestra en el campo.
- [X] T031 [US2] Implementar los puntos anteriores en `SPA/pantallas/oferta/SugerenciaDelParalelo.tsx`. Hace pasar T029 y T030.
- [X] T032 [US4] Pruebas e implementación en `SPA/pantallas/oferta/Oferta.tsx` y `Oferta.test.tsx`:
  - cada fila muestra el docente propuesto y su justificación;
  - la marca «Paralelo retirado» y «Quitar propuesta»;
  - «N de M propuestas» por carrera, y «Parcial completo»;
  - «Horas a cubrir», sin «del periodo»;
  - `horas()` con hasta dos decimales en `SPA/pantallas/carreras/nombres.ts`, con sus pruebas ajustadas.
- [X] T033 [US2] `frontend/e2e/oferta.spec.ts` y `frontend/e2e/preparacion.spec.ts`: crear, proponer con y sin justificación, con `simularApi` y axe sobre los diálogos. Reemplazar en `preparacion.spec.ts` la prueba del texto «llega con F06».

---

## Phase 9: Polish & Cross-Cutting Concerns

- [X] T034 `deploy/probar_propuestas.sh`, según [quickstart.md](quickstart.md), con los procesos reales, y su paso en `.github/workflows/perfil-consolidado.yml`:
  - crear, repetido, sin preparar y sin permiso;
  - proponer, reemplazar y quitar: una sola propuesta (SC-001);
  - las tres justificaciones y su registro (SC-002);
  - el parcial completo de una carrera (SC-003);
  - retirar y volver a ofertar;
  - las horas en otras carreras con dos coordinadores, frente a una suma aparte (SC-004);
  - otra carrera, 403;
  - proponer en menos de 3 s y crear en menos de 5 s (SC-006);
  - con `datos` detenido, el mensaje en menos de 10 s, sin guardar nada (SC-007);
  - ningún texto de justificación en los logs;
  - la base limpia al terminar.

  Ajustar `deploy/probar_sugerencia.sh` y `deploy/probar_carrera_coordinador.sh` si cambian sus respuestas.
- [X] T035 Crear `specs/031-proponer-docente/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - `integrationTest` de Generación;
  - Spotless, SpotBugs, ESLint, `npm run tipos` y Prettier;
  - los scripts de `deploy/`, shellcheck y Gitleaks;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #48`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [X] T036 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Antes de la comprobación, indicarle al autor qué registrar para ver varios candidatos y horas en otras carreras, si quiere. Después: la comprobación del autor, con SC-005, y el PR de evidencia con `Closes #48`, que dice qué no se vio en la demo.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 antes de todo el código; T003 antes de las pruebas de repositorio; T004-T005 y T006-T007 son [P]; T008 no depende de ellas.
- **US1:** T009 y T010 son [P]; T011 depende de los dos; T012 a T016, en orden.
- **US2:** depende de T013. T017 a T020, en orden.
- **US3:** depende de T018.
- **US4:** depende de T019.
- **US5:** depende de T019 y T011.
- **Pantalla:** depende de T020, T022 y T025, por el contrato real.
- **Cierre:** al final.

## Parallel Example

```text
Después de T002: T004-T005 (justificación) y T006-T007 (agregado) a la vez.
Dentro de US1: T009 (Cargas) y T010 (Académico) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 7 dan los escenarios del issue y las horas reales por la API.
- **Después:** la pantalla (fase 8) y el cierre (fase 9). Cada fase deja el sistema en verde.
