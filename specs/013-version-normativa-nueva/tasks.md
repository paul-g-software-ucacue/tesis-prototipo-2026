---

description: "Tareas de F04-H02: crear una versión nueva de la normativa"
---

# Tasks: Crear una versión nueva de la normativa

**Input**: documentos de diseño en `specs/013-version-normativa-nueva/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/normativa.md](contracts/normativa.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-55, D-62, D-65, D-66, D-67, ADR-009, ADR-015, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); MD-13 y el glosario §7.3 («Asignación normativa») del modelo de dominio.

**Organization**:
- **Fundacional:** el contrato y el catálogo de errores, el dominio con sus operaciones, la persistencia (V3, numeración, bloqueo y registro) y el permiso del Vicerrector (D-67).
- **US1:** crear una versión nueva y cambiar un parámetro en ella, sin tocar la anterior.
- **US2:** una versión asociada a un periodo no cambia; asociar y consultar el periodo.
- **US3:** editar la tabla y los parámetros en la pantalla «Normativa», por el mismo camino que usará la carga 7.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Rutas relativas a la raíz. Abreviaturas: `NOR` = `backend/servicios/normativa/src`, `JNOR` = `NOR/main/java/ec/mdd/normativa`, `PNOR` = `NOR/test/java/ec/mdd/normativa` e `INOR` = `NOR/integrationTest/java/ec/mdd/normativa`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; puertos en `puertos/`; aplicación sin infraestructura; adaptadores e `infraestructura/` con Spring. `ArquitecturaTest` y `ModulosTest` (proceso `datos`) lo comprueban.
- **DDD táctico (principio II):** `VersionNormativa` sigue inmutable; sus operaciones devuelven una versión nueva. Las reglas «no cambia en uso», «no se agregan ni quitan filas ni parámetros» y las invariantes de la fila viven en el dominio (research §1).
- **Principio VI:** ninguna cifra de la política en el código de producción.
- **Principio VII:** una versión con un periodo no cambia; cada creación, edición y asociación queda en `registro_cambio`.
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers y PostgreSQL; contrato con swagger-request-validator; 60 % de líneas por módulo (D-32).
- **Idioma:** identificadores del dominio, mensajes y JavaDoc de puertos y dominio en español (principio XII). «Asignación normativa» siempre completa; en rutas y pantalla, «periodo» (research §10).
- **Formato:** `./gradlew spotlessApply` y `npm run formato` antes de cada commit.
- **Evidencia y commits:** la evidencia va en `specs/013-version-normativa-nueva/evidencia.md`; los commits llevan `Refs #41`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/013-version-normativa-nueva/evidencia.md` con estas secciones: entorno; base (contrato, dominio, persistencia y permiso); escenario «Versión nueva» (US1); escenario «Versión en uso» (US2); escenario «Edición en pantalla» (US3); cierre; equipo de demostración; notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV)

- [X] T002 Cambiar `contratos/normativa/openapi.yaml` según contracts/normativa.md, y actualizar su comentario de cabecera (D-67; crear y editar ya no es «de F04-H02»):
  - `crearVersion` (`POST /api/normativa/versiones`, cuerpo `VersionNueva`: `copiarDe` entero ≥ 1, `codigo`, `origen`, `vigenteDesde` fecha; 201 · 400 · 404 · 409);
  - `editarVersion` (`PUT /api/normativa/versiones/{numero}`, cuerpo `VersionEditada`: `codigo`, `origen`, `vigenteDesde`, `tablaCargaHoraria` y `reglas`; la fila se identifica por `aplicaA` y `perfil`, sin `severidad`, `referencia` ni `observacion` (FR-002); 200 · 400 · 404 · 409);
  - `asociarPeriodo` (`PUT /api/normativa/periodos/{periodo}`, cuerpo `{ "version": n }`; 200 · 400 · 404 · 409) y `consultarPeriodo` (`GET /api/normativa/periodos/{periodo}`; 200 · 404), con `periodo` `^[A-Za-z0-9._-]{1,20}$` y el esquema `PeriodoNormativo` (`periodo`, `version`, `asociadaEn`);
  - `x-mdd-permiso: PARAMETROS:ESCRITURA` en las tres de escritura y `PARAMETROS:LECTURA` en `consultarPeriodo`;
  - `VersionResumen` y `VersionNormativa` ganan `copiadaDe` (opcional) y `periodos` (`[string]`, requerido, vacía si no rige ninguno);
  - todas con 401 `NoAutenticado`, 403 `AccesoNoAutorizado`, 500 y 503.

  Agregar a `contratos/comun/errores.md` las tres filas de contracts/normativa.md (`NORMATIVA_VERSION_EN_USO`, `NORMATIVA_CODIGO_REPETIDO`, `NORMATIVA_PERIODO_CON_VERSION`, todas 409). Regenerar las interfaces (`openApiGenerate`) y los tipos de la SPA (`npm run generar:api`).

### Dominio (principio II, research §1)

- [X] T003 Prueba primero, en `PNOR/dominio/`, sin dobles:
  - `VersionNormativaTest`, `copiaDe(origen, numero, datos)`: copia la tabla, las reglas y el tipo de IST valor por valor; lleva el número, el código, el origen y la vigencia nuevos y `copiadaDe` = número de origen; no tiene asignaciones; registra un `VersionNormativaCreada` con `numero`, `codigo`, `copiadaDe` y `momento`;
  - `editada(cambios)`: cambia la cabecera, las horas de una fila (las horas de clase de `DOCENTE_MT`) y el valor de un parámetro; devuelve los `CambioDeValor` (`campo`, `anterior`, `nuevo`), uno por valor que cambió y ninguno si nada cambió; una fila que rompe sus invariantes lanza `FilaInvalida` con su perfil, su tipo y el motivo; una fila, una regla o un parámetro de más o de menos lanza `CambioNoPermitido` (FR-002); la versión original no cambia;
  - `asociadaA(periodo)`: agrega una `AsignacionNormativa` (`periodo`, `asociadaEn`); `enUso()` pasa a verdadero; asociar el mismo periodo dos veces no duplica; con un periodo, `editada` lanza `VersionEnUso` (FR-004);
  - `FilaCargaHorariaTest`: las pruebas de F04-H01 siguen iguales, ahora con `FilaInvalida` (que hereda de `IllegalArgumentException`).
- [X] T004 Crear en `JNOR/dominio/` `AsignacionNormativa`, `DatosDeVersion`, `CambioDeValor`, `VersionNormativaCreada`, `VersionEnUso`, `CambioNoPermitido` y `FilaInvalida`, y las operaciones de `VersionNormativa` (con `copiadaDe` y `asignaciones`), con JavaDoc en español que cite RF-19, el modelo de dominio §5.1 y el glosario §7.3. `FilaCargaHoraria` lanza `FilaInvalida`. T003 pasa, y las pruebas de dominio de F04-H01 siguen en verde.

### Catálogo de errores (ADR-016)

- [X] T005 Prueba primero, en `PNOR/adaptadores/entrada/ErroresNormativaTest.java`, como `ErroresRolesTest`: los tres códigos de `ErroresNormativa` coinciden con `errores.md` en estado, título y descripción. Crear `JNOR/adaptadores/entrada/ErroresNormativa.java` con `VersionEnUso` (de T004); cada otra excepción se suma en la tarea que la crea: `CodigoRepetido` en T009 y `PeriodoConVersion` en T012. T005 pasa.

### Persistencia (research §3, §6)

- [X] T006 Crear `NOR/main/resources/db/migration/normativa/V3__versiones_y_periodos.sql` según data-model.md:
  - `version_normativa.copiada_de` int nulo, FK a `version_normativa(numero)`, y el `codigo` único (si no lo es ya);
  - la tabla `asignacion_normativa` (`version_id` FK, `periodo` varchar(20) **único**, `asociada_en` timestamptz, `asociada_por` varchar(64));
  - el `check` de `registro_cambio.accion` con `VERSION_CREADA`, `VERSION_EDITADA` y `VERSION_ASOCIADA`.
- [X] T007 Prueba primero, en `INOR/` (Testcontainers):
  - `RepositorioVersionesJdbcIntegrationTest`: guardar una versión nueva copiada de la 1 y reconstruirla igual, valor por valor; guardar una versión editada reemplaza sus filas y parámetros en una sola transacción (si falla a mitad, no queda nada cambiado); `siguienteNumero` con el candado de la transacción da números distintos a dos creaciones simultáneas (dos hilos); `bloquear(numero)` con `SELECT … FOR UPDATE` hace esperar a la segunda de dos transacciones sobre la misma versión; la versión reconstruida trae sus asignaciones y `copiadaDe`; un periodo repetido viola la restricción única;
  - `RegistroDeCambiosJdbcIntegrationTest`: guarda `VERSION_CREADA` (con `copiadaDe`), `VERSION_EDITADA` (con la lista de cambios) y `VERSION_ASOCIADA` (con el periodo), con el usuario y el momento;
  - `NormativaEsquemaIntegrationTest`: la versión anterior, que solo conoce V1 y V2, arranca sobre el esquema con V3 (D-55);
  - `PoliticaDe2024IntegrationTest` sigue en verde: la versión 1 queda igual después de V3.

  Ampliar `JNOR/puertos/salida/RepositorioVersiones.java` (`guardar`, `siguienteNumero`, `bloquear`, `existeCodigo`, `porPeriodo`) y `JNOR/adaptadores/salida/RepositorioVersionesJdbc.java`; crear `JNOR/puertos/salida/RegistroDeCambios.java` y `JNOR/adaptadores/salida/RegistroDeCambiosJdbc.java`, con el detalle en `jsonb` escrito con Jackson. T007 pasa.

### Permiso del Vicerrector (D-67)

- [X] T008 [P] Prueba primero: en `backend/servicios/roles/src/integrationTest/java/ec/mdd/roles/MatrizD16IntegrationTest.java`, la fila esperada de `VICERRECTOR` gana `PARAMETROS:ESCRITURA`. Después crear `backend/servicios/roles/src/main/resources/db/migration/roles/V5__vicerrector_escribe_normativa.sql`, con alcance `TODO`. La prueba pasa.

**Checkpoint**: el contrato, el catálogo, el dominio, la persistencia y el permiso están listos; `./gradlew check` y las pruebas de integración de Normativa y Roles en verde.

---

## Phase 3: User Story 1 - Cambiar un parámetro en una versión nueva (Priority: P1) 🎯 MVP

**Goal**: crear la versión 2 a partir de la 1 y cambiar en ella las horas de clase del docente de medio tiempo, sin que la 1 cambie (FR-001 a FR-003, FR-005, FR-008, FR-009; SC-002, SC-004 y SC-005).

**Independent Test**: con la versión 1 asociada a un periodo, crear la versión 2, cambiar las horas de clase de `DOCENTE_MT` y comprobar que la 2 tiene el cambio y la 1 sigue idéntica.

- [X] T009 [US1] Prueba primero, en `PNOR/aplicacion/` (Mockito):
  - `CrearVersionServicioTest`: crea con `siguienteNumero` dentro de la transacción y registra `VERSION_CREADA` con el usuario de la sesión; una versión de origen que no existe lanza `VersionNoEncontrada`; un código repetido lanza `CodigoRepetido` y no guarda nada;
  - `EditarVersionServicioTest`: bloquea la versión, aplica `editada` y guarda; registra `VERSION_EDITADA` con los cambios, y no registra nada si no cambió nada; una versión en uso lanza `VersionEnUso` sin guardar; un código que ya usa otra versión lanza `CodigoRepetido`; `FilaInvalida` y `CambioNoPermitido` pasan sin guardar.

  Crear los puertos `JNOR/puertos/entrada/CrearVersion.java` y `EditarVersion.java`, con JavaDoc que nombra las excepciones que lanzan; `CodigoRepetido` en `JNOR/puertos/entrada/`, junto a `EditarVersion`, como `VersionNoEncontrada`, y sumado a `ErroresNormativa`; los servicios `CrearVersionServicio` y `EditarVersionServicio` con `TransactionOperations`, y sus beans en `JNOR/infraestructura/ConfiguracionNormativa.java`. T009 pasa.
- [X] T010 [US1] Prueba primero, en `PNOR/adaptadores/entrada/`:
  - `NormativaContratoTest`: `crearVersion` responde 201 con la versión nueva contra el contrato; `editarVersion` responde 200 con la versión editada; un `FilaInvalida` responde 400 `COMUN_SOLICITUD_INVALIDA` con un detalle cuyo campo es `tablaCargaHoraria[DOCENTE_MT/TODOS]` y su descripción en español (por ejemplo, «Las horas de clase (13) no están entre el mínimo y el máximo (8 a 12)»); un `CambioNoPermitido`, 400 con su detalle; un código repetido, 409 `NORMATIVA_CODIGO_REPETIDO`; una versión de origen que no existe, 404; `listarVersiones` y `consultarVersion` traen `copiadaDe` y `periodos`. La prueba de F04-H01 que esperaba 405 en `POST` y `PUT` se reemplaza por estas.
  - `PermisosNormativaTest`: `VerificadorDePermisos` sobre las seis operaciones del contrato (SC-005 de F04-H01 y FR-008).

  Agregar `crearVersion` y `editarVersion` a `JNOR/adaptadores/entrada/NormativaControlador.java`, con `@PreAuthorize("hasAuthority('PERMISO_PARAMETROS:ESCRITURA')")` y el usuario del token (`sub`). El controlador captura `FilaInvalida` y `CambioNoPermitido` y lanza `SolicitudInvalida`, con un `DetalleRespuesta` por campo, como `RolesControlador`; el campo es `tablaCargaHoraria[<perfil>/<aplicaA>]` o `reglas[<RN-xx>].<parametro>`. T010 pasa. `ModulosTest` de `datos` sigue en verde.
- [X] T011 [US1] Agregar a `deploy/probar_normativa.sh` el paso 5, con un periodo de prueba `prueba-<aleatorio>`: `vicerrectorado.demo` asocia la versión 1 a ese periodo (`asociarPeriodo`), crea una versión a partir de la 1 (su número queda en `versiones_creadas`), cambia en ella las horas de clase de `DOCENTE_MT` a un valor válido distinto y comprueba que la versión creada tiene el cambio y que la 1 es idéntica, byte a byte, a la de antes de crearla, comparando sin el campo `periodos` (`jq 'del(.periodos)'`); aparte, `periodos` de la 1 contiene el periodo de prueba (SC-002). Agregar la limpieza por `psql` de research §12, en un `trap … EXIT`: la asignación del periodo de prueba con su registro `VERSION_ASOCIADA`, y **solo** las versiones cuyo número anotó el script (`versiones_creadas`), con sus filas, reglas, parámetros y registros. Los pasos 1 y 4 dejan de exigir «una sola versión»: comprueban que la versión 1 existe, es la primera de la lista y es idéntica a la migración sin el campo `periodos`, así el script pasa aunque haya otras versiones o la 1 rija un periodo real. Correrlo y anotarlo.

**Checkpoint**: una versión nueva se crea y se cambia por la API, y la anterior no cambia.

---

## Phase 4: User Story 2 - Una versión en uso no se cambia (Priority: P1)

**Goal**: una versión asociada a un periodo no se modifica; un periodo tiene una sola versión (FR-004, FR-006; SC-003).

**Independent Test**: con una versión asociada a un periodo, intentar cambiarla por la API: 409 `NORMATIVA_VERSION_EN_USO`, y la versión no cambia.

- [X] T012 [US2] Prueba primero, en `PNOR/aplicacion/AsociarVersionAPeriodoServicioTest.java` (Mockito): bloquea la versión, la asocia y registra `VERSION_ASOCIADA`; asociar otra vez la misma versión al mismo periodo no cambia nada ni registra; un periodo con otra versión lanza `PeriodoConVersion`; una versión que no existe lanza `VersionNoEncontrada`. `ConsultarNormativaServicioTest` gana `porPeriodo`: un periodo sin versión lanza `PeriodoSinVersion`. Crear el puerto publicado `JNOR/puertos/entrada/AsociarVersionAPeriodo.java` (JavaDoc: lo usará Académico en F02-H01), `PeriodoConVersion` en `JNOR/puertos/entrada/`, junto a `AsociarVersionAPeriodo` y sumado a `ErroresNormativa`, `PeriodoSinVersion` junto a `ConsultarNormativa`, y el servicio con su bean. T012 pasa.
- [X] T013 [US2] Prueba primero, en `PNOR/adaptadores/entrada/NormativaContratoTest.java`: `asociarPeriodo` responde 200 `PeriodoNormativo` contra el contrato; un periodo con otra versión, 409 `NORMATIVA_PERIODO_CON_VERSION`; un periodo con un código inválido (`2025 1`), 400; `consultarPeriodo` responde 200 o 404; `editarVersion` sobre una versión en uso responde 409 `NORMATIVA_VERSION_EN_USO` con la descripción que pide crear una versión nueva. Agregar las dos operaciones al controlador, con `PARAMETROS:ESCRITURA` y `PARAMETROS:LECTURA`. T013 pasa.
- [X] T014 [US2] Prueba en `INOR/VersionEnUsoIntegrationTest.java` (Testcontainers, con los servicios reales): una edición y una asociación de la misma versión lanzadas a la vez (dos hilos) terminan con la versión asociada y, o bien la edición guardada antes de asociar, o bien rechazada con `VersionEnUso`; nunca editada después de asociada (research §3). Y dos ediciones de la misma versión sin periodo, lanzadas a la vez, terminan las dos guardadas en orden, con dos registros `VERSION_EDITADA`; la versión queda con la última (caso límite).
- [X] T015 [US2] Agregar a `deploy/probar_normativa.sh` los pasos 6 y 7: editar la versión 1, que rige el periodo de prueba, da 409 `NORMATIVA_VERSION_EN_USO` y la versión no cambia (SC-003); asociar la versión creada en el paso 5 al mismo periodo da 409 `NORMATIVA_PERIODO_CON_VERSION`, y `consultarPeriodo` sigue dando la 1. Con `docente.demo`, `crearVersion` da el 403 común. Correrlo y anotarlo.

**Checkpoint**: la regla de inmutabilidad se cumple de punta a punta, también con concurrencia.

---

## Phase 5: User Story 3 - Editar la tabla en pantalla (Priority: P1)

**Goal**: en «Normativa», crear una versión nueva, editar en pantalla su tabla y sus parámetros, y asociarla a un periodo; una versión con periodo se ve en solo lectura (FR-007, FR-010; SC-001).

**Independent Test**: con una versión sin periodo, editar una fila en la pantalla y comprobar que el cambio queda en esa versión, con las mismas validaciones que la API.

- [X] T016 [US3] Prueba primero, en `frontend/src/pantallas/normativa/Normativa.test.tsx`, con la API simulada (ampliar `datosDePrueba.ts` con una versión 2 sin periodo y la 1 con el periodo `2025-1`):
  - al abrir, queda elegida la versión de número más alto;
  - con `PARAMETROS:ESCRITURA`: «Crear versión nueva» abre un diálogo con código, origen y vigencia y llama a `crearVersion` con `copiarDe`; la versión creada queda elegida;
  - en una versión sin periodo, «Editar» pasa la tabla y los parámetros a campos numéricos y la cabecera a campos de texto y fecha; «Guardar» envía la versión completa a `editarVersion`; «Cancelar» descarta;
  - un 400 marca la fila o el parámetro de cada detalle con su descripción, junto al campo, sin códigos; un 409 se muestra con su título y su descripción;
  - «Asociar a un periodo» pide el código y llama a `asociarPeriodo`;
  - una versión con periodos muestra «Rige: 2025-1», sin «Editar», con «Crear versión nueva»;
  - con solo `PARAMETROS:LECTURA` no aparece ninguna acción, y las pruebas de F04-H01 siguen en verde.

  Ampliar `Normativa.tsx` y `consultas.ts` (mutaciones con React Query que invalidan la lista y la versión). T016 pasa.
- [X] T017 [US3] Prueba con Playwright, en `frontend/e2e/normativa.spec.ts`, con la API simulada: `vicerrectorado.demo` crea la versión 2, cambia las horas de clase de `DOCENTE_MT`, guarda y ve el valor nuevo; un valor fuera de rango muestra el motivo junto al campo; axe-core sin fallas WCAG 2.1 AA en la edición y en los dos diálogos (esperar a que `.MuiDialog-container` tenga opacidad 1, como en «Rol nuevo»).
- [X] T018 [US3] FR-010: anotar en la evidencia que `editarVersion` es la única entrada para cambiar una versión (la pantalla la usa y la carga 7 de F01 la usará), y que su JavaDoc en `EditarVersion` lo dice. Revisión del principio VI: buscar en `JNOR/` y en `frontend/src/pantallas/normativa/` cifras de la política y anotar que no aparecen.

**Checkpoint**: las tres historias pasan de punta a punta.

---

## Phase 6: Cierre

- [ ] T019 Correr:
  - en el backend, `./gradlew check integrationTest`;
  - en el frontend, `npm run lint`, `formato:verificar`, `tipos`, `test -- --run --coverage`, `test:e2e` y `verificar:ruptura`;
  - `shellcheck` sobre `probar_normativa.sh`, y Gitleaks sobre un clon en el scratchpad, montado con `:ro,Z` (nunca el repositorio);
  - `probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh`, `probar_spa.sh`, `probar_normativa.sh` dos veces seguidas (la limpieza permite repetirlo) y `deploy/equipo/probar_equipo.sh`.

  Anotar en la evidencia el tiempo de `crearVersion` y `editarVersion` en el paso 5 de `probar_normativa.sh` (meta < 200 ms, plan.md). Todo en verde, con el 60 % de líneas. Hacer commit y push, abrir el PR en borrador con `Refs #41` y marcarlo listo **después** de que terminen sus runs. Comprobar los checks, `perfil-consolidado` y `5 · imágenes`.
- [ ] T020 Completar `evidencia.md` y la «Definición de terminado» del PR. Fusionarlo con *squash* **con la aprobación del autor**, con la rama al día con `main`. Después:
  - esperar el despliegue automático en el PC («Sano en N s»), con `normativa` en V3 y `roles` en V5;
  - el autor entra de nuevo con `vicerrectorado.demo` (para traer el permiso nuevo, D-62), crea la versión 2 y cambia las horas de clase del docente de medio tiempo en menos de 2 minutos, y comprueba que la 1 sigue igual (SC-001);
  - abrir el PR de evidencia con `Closes #41` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup (fase 1)**: T001 es libre.
- **Foundational (fase 2)**:
  - T002 antes que todo lo demás;
  - T003 antes que T004;
  - T005 después de T002 (el catálogo) y de T004 (`VersionEnUso`);
  - T006 y T007 después de T004;
  - T008 es libre.
- **US1 (fase 3)**: depende de la fase 2. T009 antes que T010; T011 depende de T010, T013 (usa `asociarPeriodo`) y T008.
- **US2 (fase 4)**: T012 antes que T013; T014 depende de T012 y T009; T015 depende de T011 y T013.
- **US3 (fase 5)**: T016 depende de T002; T017 de T016; T018 de T010.
- **Cierre (fase 6)**: T019 y T020 dependen de todo.

### Parallel Opportunities

- Fase 2: T008.
- US3: T016 puede empezar después de T002, porque la pantalla se prueba con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el dominio, la persistencia y el permiso.
2. Crear y editar por la API.
3. **Validar:** la versión 2 tiene el cambio y la 1 sigue idéntica.

### Incremental

1. **US2:** asociar a un periodo y la regla de inmutabilidad, con concurrencia.
2. **US3:** la edición en pantalla.
3. **Cierre:** evidencia, PR, fusión y despliegue.

---

## Notes

- Cambiar severidades y los pesos de afinidad es F04-H03; la carga 7 por archivo, F01 (usará `EditarVersion`); abrir el periodo y elegir su versión, F02-H01 (usará `AsociarVersionAPeriodo`).
- El evento `VersionNormativaCreada` se guarda como `VERSION_CREADA`; su envío entre procesos llega con su primer consumidor (research §6).
- Hacer commit por tarea o por grupo lógico en `013-version-normativa-nueva`, con `Refs #41`.
