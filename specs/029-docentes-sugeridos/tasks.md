---

description: "Tareas de F06-H02: ver los docentes sugeridos para una asignatura"
---

# Tasks: Ver los docentes sugeridos para una asignatura

**Input**: documentos de diseño en `specs/029-docentes-sugeridos/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-10-31 (en la spec);
- D-75, aprobada por el autor el 2026-10-31 y por escribir en T001;
- ADR-006, ADR-009, ADR-010, ADR-018, ADR-023, D-64, D-68, D-70, D-72 y D-74 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-25, RN-19 y D-01 de la ERS;
- el glosario §7: «Horas disponibles» precisado y «Candidato» agregado, con aprobación del autor, ya aplicados.

Plan aprobado por el autor el 2026-10-31.

**Organization**:
- **Fundacional:** D-75, los contratos, los errores y las tres consultas de los dueños.
- **US1, el orden; US2, el mínimo; US3, las horas:** las reglas de `Sugerencia`, una por historia.
- **La consulta y la pantalla,** que entregan las tres.
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1, US2 o US3.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `GEN` = `backend/servicios/generacion/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `SPA` = `frontend/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** `Sugerencia` y `Candidato` son dominio puro, sin Spring ni HTTP. `ArquitecturaTest` lo comprueba.
- **Límites (principio V):** Generación no importa tipos de Académico, Normativa ni Docentes; recibe sus propios valores. `ModulosTest` lo comprueba.
- **Rutas internas (ADR-023):** solo el rol `PROCESO`; un token de usuario recibe 403 y el API Gateway no las enruta.
- **Reglas (principio VI):** ningún mínimo ni peso en el código de producción. Las pruebas usan valores que no son los de por defecto.
- **Nada se guarda:** la sugerencia se calcula en cada consulta (FR-022). Sin migraciones.
- **Datos (principio IX):** la ruta de docentes activos no devuelve contacto ni contrato, y ninguna ficha va a los logs. Solo datos de prueba ficticios.
- **Decimales:** el puntaje, el mínimo y las horas se comparan con `compareTo`, nunca con `equals`. Solo las horas contratadas son enteras.
- **Mensajes:** en «tú» (D-74); los errores, del catálogo con sus 8 columnas (D-72).
- **Validación (principio XIV):** `^[A-Za-z0-9._-]{1,20}$` para el periodo, `^[0-9A-Z-]{1,30}$` para la asignatura y `^[0-9A-Z_-]{1,20}$` para el paralelo, en el contrato y el servidor. La pantalla no tiene campos: los códigos salen de la fila.
- **Pruebas:**
  - Mockito solo en la aplicación;
  - fuera de ahí, dobles escritos a mano;
  - el cliente entre procesos, contra `ReceptorSimulado`.

---

## Phase 1: Setup

- [X] T001 Escribir D-75 en `docs/decisiones.md`, con su fila en el índice, y pasar «Siguiente número libre» a D-76 · ADR-024: las horas disponibles salen de las horas contratadas (research §9).

---

## Phase 2: Foundational

**Purpose**: los contratos, los errores y lo que publican los dueños. Bloquea las historias.

- [X] T002 Contratos, según [contracts/generacion.md](contracts/generacion.md):
  - `contratos/generacion/openapi.yaml`: la etiqueta `Sugerencia`, para que `consultarSugerencia` tenga su propia interfaz, y los esquemas `Sugerencia`, `Candidato` y `Dedicacion`;
  - `contratos/academico/openapi.yaml`: `consultarParaleloDeLaOferta` y `ParaleloDeLaOferta`;
  - `contratos/docentes/openapi.yaml`: `consultarDocentesActivos` y `DocenteActivo`;
  - `contratos/normativa/openapi.yaml`: `minimoAfinidad`, obligatorio en `PesosDelPeriodo`;
  - `contratos/comun/errores.md`: los 2 códigos, con sus 8 columnas.

  Actualizar el comentario de cabecera de cada contrato. Regenerar las interfaces con Gradle.
- [X] T003 [P] Pruebas de Académico en `ACA/test/java/ec/mdd/academico/adaptadores/entrada/InternoAcademicoContratoTest.java`:
  - una asignatura-paralelo con su tronco común, su carrera y las horas del paralelo;
  - un paralelo con horas indicadas trae esas horas, no las de la asignatura;
  - una retirada responde 200, con su estado;
  - un periodo o un paralelo que no existen, 404;
  - un código mal escrito, 400;
  - un token de usuario, 403;
  - la respuesta no trae palabras clave.
- [X] T004 [P] Implementar `consultarParaleloDeLaOferta` en `ACA/main/java/ec/mdd/academico/adaptadores/entrada/InternoAcademicoControlador.java`, con su puerto y su servicio, y su prueba de aplicación. Hace pasar T003.
- [X] T005 [P] Pruebas de Docentes en `DOC/test/java/ec/mdd/docentes/adaptadores/entrada/InternoDocentesContratoTest.java`:
  - solo docentes activos: ni en licencia ni inactivos;
  - docentes de varias carreras base;
  - su identificación, su nombre completo, su dedicación, sus horas contratadas y su formación;
  - la respuesta no trae correo, teléfono, contrato, institución, registro ni asignaturas afines;
  - sin docentes activos, lista vacía;
  - un token de usuario, 403.
- [X] T006 [P] Implementar `consultarDocentesActivos` en `DOC/main/java/ec/mdd/docentes/adaptadores/entrada/InternoDocentesControlador.java`, con su puerto y su servicio sobre `RepositorioDocentes.todos()`, y su prueba de aplicación. Hace pasar T005.
- [X] T007 [P] Prueba de Normativa en `NOR/test/java/ec/mdd/normativa/adaptadores/entrada/InternoNormativaContratoTest.java`: `minimoAfinidad` es el de la versión del periodo, también con un valor distinto del de por defecto.
- [X] T008 [P] Sumar `minimoAfinidad` a `PesosDeLaVersion`, `ConsultarPesosDelPeriodoServicio` e `InternoNormativaControlador`, y a su prueba de aplicación. Una versión sin mínimo falla igual que una sin pesos. Hace pasar T007.

**Checkpoint**: los tres dueños publican lo necesario; `./gradlew check` en verde en sus módulos.

---

## Phase 3: User Story 1 - Ver los candidatos ordenados (Priority: P1)

**Goal**: el orden de `Sugerencia`, con el desglose de cada candidato.

**Independent Test**: `SugerenciaTest`, con docentes y pesos conocidos, compara el orden y cada puntaje con lo esperado.

- [X] T009 [US1] Primeras pruebas en `GEN/test/java/ec/mdd/generacion/dominio/SugerenciaTest.java`, sin dobles:
  - del mayor puntaje al menor;
  - a igual puntaje, más horas disponibles primero;
  - a igual puntaje y horas, por nombre en español, con tildes y eñes;
  - `2` y `2.00` empatan;
  - quien no tiene horas para el paralelo va después de todos los demás, aunque tenga el mejor puntaje;
  - quien tiene justo las horas del paralelo está en el primer grupo;
  - con 2,5 h libres y un paralelo de 2,5 h, tiene horas; con 2,49 h, no;
  - dos paralelos de la misma asignatura con horas distintas pueden dar órdenes distintos;
  - cada candidato trae su `PuntajeAfinidad`, con su desglose;
  - una lista de docentes vacía da una sugerencia sin candidatos.
- [X] T010 [US1] Implementar en `GEN/main/java/ec/mdd/generacion/dominio/`: `AsignaturaParalelo`, `DocenteActivo`, `Dedicacion`, `HorasDelDocente`, `Candidato` y `Sugerencia`, según [data-model.md](data-model.md). `PesosDelPeriodo` suma `minimoAfinidad`: ajustar sus usos en `DatosDesdeProcesoDatos`, que ya lee ese campo, y en las pruebas de F06-H01. Hace pasar T009.

---

## Phase 4: User Story 2 - Dejar fuera a quien no alcanza el mínimo (Priority: P1)

**Goal**: el mínimo de la versión y la exención del tronco común.

**Independent Test**: `SugerenciaTest`, con un mínimo que no es el de por defecto, una asignatura sin tronco común y otra con él.

- [X] T011 [US2] Sumar a `SugerenciaTest`:
  - bajo el mínimo no aparece, y `fueraPorElMinimo` lo cuenta;
  - igual al mínimo aparece, también con `2` frente a `2.00`;
  - en tronco común aparecen todos, con 0 puntos también, y `fueraPorElMinimo` es 0;
  - nadie alcanza el mínimo: sin candidatos, con `fueraPorElMinimo` mayor que 0;
  - un docente bajo el mínimo y sin horas no aparece y cuenta como fuera;
  - dos mínimos distintos dan listas distintas.
- [X] T012 [US2] Aplicar el mínimo y el tronco común en `Sugerencia.armar`. Hace pasar T011.

---

## Phase 5: User Story 3 - Ver las horas que ya tiene en otras carreras (Priority: P1)

**Goal**: las horas disponibles, con horas simuladas en las pruebas.

**Independent Test**: `SugerenciaTest`, con `HorasDelDocente` simuladas.

- [X] T013 [US3] Sumar a `SugerenciaTest` y a `GEN/test/java/ec/mdd/generacion/dominio/CandidatoTest.java`:
  - horas disponibles: contratadas menos asignadas;
  - más asignadas que contratadas: 0, nunca negativo;
  - unas horas asignadas con decimales dan horas disponibles exactas;
  - las horas en otras carreras se muestran aparte de las asignadas;
  - un docente sin horas registradas tiene 0 y 0;
  - unas horas asignadas cambian su lugar en la lista;
  - `HorasDelDocente` rechaza negativos y `enOtrasCarreras` mayor que `asignadas`.
- [X] T014 [US3] Completar `Candidato.horasDisponibles()` y `HorasDelDocente`. Hace pasar T013.
- [X] T015 [US3] Crear el puerto `GEN/main/java/ec/mdd/generacion/puertos/salida/HorasAsignadas.java` y `GEN/main/java/ec/mdd/generacion/adaptadores/salida/SinAsignacionesTodavia.java`, que responde un mapa vacío, con su JavaDoc diciendo que F06-H03 lo reemplaza (FR-009), y su prueba.

**Checkpoint**: el dominio cubre los tres escenarios.

---

## Phase 6: La consulta (US1, US2, US3)

**Purpose**: la operación de Generación.

- [X] T016 [US1] Sumar a `GEN/test/java/ec/mdd/generacion/adaptadores/salida/DatosDesdeProcesoDatosTest.java`, contra `ReceptorSimulado`:
  - `paralelo` lee su respuesta, y un 404 da vacío;
  - `docentesActivos` lee la lista;
  - `pesos` lee `minimoAfinidad`;
  - un código con caracteres que se escapan en la ruta.
- [X] T017 [US1] Crear `GEN/main/java/ec/mdd/generacion/puertos/salida/DatosParaSugerencia.java`, con `paralelo` y `docentesActivos`, e implementarlo en `DatosDesdeProcesoDatos`. Hace pasar T016.
- [X] T018 [US1] Pruebas de `GEN/test/java/ec/mdd/generacion/aplicacion/ConsultarSugerenciaServicioTest.java`, con Mockito:
  - arma la sugerencia con los datos de los dueños y las horas del puerto, con la carrera del paralelo;
  - un paralelo que no existe: `ParaleloNoEncontrado`, sin pedir nada más;
  - uno retirado: `ParaleloRetirado`, sin pedir nada más;
  - un periodo sin versión: `PeriodoSinAbrir`;
  - un fallo entre procesos se propaga, sin lista parcial.
- [X] T019 [US1] Implementar el puerto `ConsultarSugerencia`, sus excepciones y `ConsultarSugerenciaServicio`, con su bean en `ConfiguracionGeneracion`, junto con `SinAsignacionesTodavia`. Hace pasar T018.
- [X] T020 [US1] Pruebas de contrato y de permisos en `GEN/test/java/ec/mdd/generacion/adaptadores/entrada/SugerenciaContratoTest.java` y `PermisosGeneracionTest.java`:
  - el 200 cumple el contrato, con `ValidadorDeContrato`;
  - 400 por cada parámetro mal escrito, válidos e inválidos;
  - 404, 409 y sus códigos;
  - 502 y 504;
  - un 403 explícito;
  - `ErroresGeneracionTest` con los 2 códigos, y `VerificadorDeCatalogo`.

  Sumar `SugerenciaControlador` y su doble al `@WebMvcTest` de `PermisosGeneracionTest`.
- [X] T021 [US1] Implementar `GEN/main/java/ec/mdd/generacion/adaptadores/entrada/SugerenciaControlador.java` con sus permisos, y los 2 códigos en `ErroresGeneracion`. Hace pasar T020.

**Checkpoint**: los tres escenarios responden por la API, con 0 h asignadas.

---

## Phase 7: La pantalla (US1, US2, US3)

**Purpose**: la acción «Sugerencia» en «Oferta».

- [X] T022 [US1] Sacar el desglose de `SPA/pantallas/docentes/AfinidadDelDocente.tsx` a `SPA/pantallas/comun/DesgloseDeAfinidad.tsx`, sin cambiar lo que muestra la ficha. Las pruebas de `Docentes.test.tsx` siguen en verde.
- [X] T023 [US1] Agregar `useSugerencia` en `SPA/pantallas/oferta/consultas.ts`, con `gcTime` 0, como la afinidad, y sus datos en `SPA/pantallas/oferta/datosDePrueba.ts`.
- [X] T024 [US1] Pruebas en `SPA/pantallas/oferta/Oferta.test.tsx`:
  - la fila ofertada ofrece «Sugerencia» a quien lee docentes;
  - la retirada, no;
  - quien lee la oferta sin leer docentes, no;
  - aparece también si el periodo no se puede modificar;
  - los candidatos, en el orden en que llegan, con su dedicación y sus horas;
  - las horas con decimales se muestran con coma;
  - el desglose desplegado;
  - la marca «Sin horas suficientes»;
  - «0 h en otras carreras y unidades»;
  - la línea de cuántos quedaron fuera por el mínimo;
  - sin candidatos: nadie alcanza el mínimo, y no hay docentes activos;
  - un periodo sin abrir, el 502 y el 504: `AvisoDeError`, sin detalles técnicos;
  - no muestra la identificación ni un botón para proponer.
- [X] T025 [US1] Implementar `SPA/pantallas/oferta/SugerenciaDelParalelo.tsx` y la acción en `SPA/pantallas/oferta/Oferta.tsx`, con `LECTURA_DE_LOS_DOCENTES`, aparte de `puedeCambiar`. Hace pasar T024.
- [X] T026 [US1] `frontend/e2e/oferta.spec.ts`: el recorrido con `simularApi` y axe sobre el diálogo, ya en opacidad 1.

**Checkpoint**: los escenarios del issue pasan por la pantalla; el de las horas, con datos simulados.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T027 `deploy/probar_sugerencia.sh`, según [quickstart.md](quickstart.md), con los dos procesos reales y un oráculo en Python sobre los CSV, y su paso en `.github/workflows/perfil-consolidado.yml`:
  - el orden y los candidatos frente al oráculo, sin tronco común y con él (SC-001);
  - el docente que ya está en licencia en los datos no aparece; uno activo pasa a licencia, desaparece y vuelve;
  - un candidato cuya carrera base no es la de la asignatura;
  - otra versión con otro mínimo en otro periodo (SC-002);
  - un paralelo retirado, y uno que no existe;
  - los errores y permisos de quickstart §5;
  - `/interno/docentes/activos` con token de usuario, y por el API Gateway;
  - la sugerencia en menos de 3 s (SC-003);
  - con `datos` detenido, el mensaje en menos de 10 s (SC-004), y `datos` de vuelta y sano al terminar;
  - una ficha y un correo que no aparecen en los logs de ningún contenedor (SC-005);
  - la base limpia al terminar.
- [X] T028 Crear `specs/029-docentes-sugeridos/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo;
  - Spotless, SpotBugs, ESLint y TypeScript;
  - los scripts de `deploy/`, shellcheck y Gitleaks;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  La evidencia debe decir que el escenario «Horas en otras carreras» solo se probó con datos simulados, y que queda para F06-H03. Commit, push, PR en borrador con `Refs #47` y marcarlo listo cuando sus comprobaciones pasen.
- [X] T029 Agregar en el issue #48 (F06-H03) los tres pendientes de esta historia:
  - demostrar el escenario «Horas en otras carreras» con datos reales;
  - aplicar el filtro de «lo propio» con el usuario ligado a su carrera;
  - el glosario dice que `Horas` son enteras, pero la oferta admite decimales: precisarlo cuando nazcan las asignaciones.
- [X] T030 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Después: la comprobación del autor, con SC-006, y el PR de evidencia con `Closes #47`.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:**
  - T002 antes de todo el código;
  - T003-T004, T005-T006 y T007-T008 son [P] después de T002, cada implementación después de su prueba.
- **Dominio:** T009 a T015 van en orden; dependen de T002, y T010 también de T008, por el contrato de Normativa.
- **Consulta:**
  - T016 y T017 dependen de T008 y T010;
  - T018 y T019 dependen de T015 y T017;
  - T020 y T021 dependen de T019.
- **Pantalla:** depende de T021, por el contrato real; T022 va primero.
- **Cierre:** al final; T030 depende de todo lo anterior.

## Parallel Example

```text
Después de T002: T003-T004 (Académico), T005-T006 (Docentes) y T007-T008 (Normativa) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 6 dan los tres escenarios por la API.
- **Después:** la pantalla (fase 7) y el cierre (fase 8). Cada fase deja el sistema en verde.
