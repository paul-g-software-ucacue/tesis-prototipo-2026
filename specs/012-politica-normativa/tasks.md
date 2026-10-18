---

description: "Tareas de F04-H01: la política 2024 como primera versión de normativa"
---

# Tasks: La política 2024 como primera versión de normativa

**Input**: documentos de diseño en `specs/012-politica-normativa/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/normativa.md](contracts/normativa.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-11, D-13, D-55, D-62, D-63, D-65, D-66, ADR-003, ADR-015, ADR-016, ADR-019 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); MD-08 del modelo de dominio.

**Organization**:
- **Fundacional:** el contrato, el dominio de Normativa, la versión 1 en la base con su prueba contra la política y el permiso del Vicerrector (D-66).
- **US1:** el Vicerrector consulta la versión 1 en la pantalla «Normativa».
- **US2:** la versión 1 solo vive en los datos: no se duplica ni cambia al reiniciar o desplegar.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 o US2.
- Rutas relativas a la raíz. Abreviaturas: `NOR` = `backend/servicios/normativa/src`, `JNOR` = `NOR/main/java/ec/mdd/normativa`, `PNOR` = `NOR/test/java/ec/mdd/normativa` e `INOR` = `NOR/integrationTest/java/ec/mdd/normativa`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; puertos en `puertos/`; aplicación sin infraestructura; adaptadores e `infraestructura/` con Spring. `ArquitecturaTest` y `ModulosTest` (proceso `datos`) lo comprueban.
- **DDD táctico (principio II):** las invariantes viven en el agregado y en sus objetos de valor, no en la aplicación ni en la base.
- **Principio VI:** ninguna cifra de la política en el código de producción; solo en `V2__politica_2024.sql` y en la copia de la carga 7 de las pruebas.
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers y PostgreSQL; contrato con swagger-request-validator; 60 % de líneas por módulo (D-32).
- **Idioma:** identificadores del dominio, mensajes y JavaDoc de puertos y dominio en español (principio XII), con la regla RN-xx que aplica cada pieza.
- **Formato:** `./gradlew spotlessApply` y `npm run formato` antes de cada commit.
- **Evidencia y commits:** la evidencia va en `specs/012-politica-normativa/evidencia.md`; los commits llevan `Refs #40`.

---

## Phase 1: Setup

- [ ] T001 Crear `specs/012-politica-normativa/evidencia.md` con estas secciones: entorno; base (dominio, versión 1 y permiso); escenario «Versión inicial»; solo datos (US2); equipo de demostración; notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV)

- [ ] T002 Crear `contratos/normativa/openapi.yaml` según contracts/normativa.md:
  - `listarVersiones` (`GET /api/normativa/versiones`) y `consultarVersion` (`GET /api/normativa/versiones/{numero}`, `numero` entero ≥ 1), las dos con `x-mdd-permiso: PARAMETROS:LECTURA`;
  - los esquemas `VersionResumen`, `VersionNormativa`, `Regla`, `Parametro` y `FilaCargaHoraria`, con `tipoIst` ∈ `T1`…`T5`, `alcanceIst` ∈ `TODOS`, `T1`…`T5` y `severidad` ∈ `DURA`, `BLANDA`; las horas opcionales que la política no da se omiten;
  - respuestas 401 `NoAutenticado`, 403 `AccesoNoAutorizado`, 404, 500 y 503.

  Agregar a `backend/servicios/normativa/build.gradle` `id 'mdd.contrato'`, `spring-boot-starter-jdbc`, `spring-security-core` y `tools.jackson.core:jackson-databind`. Regenerar las interfaces y los tipos de la SPA: `generar:api` gana `src/api/generado/normativa.d.ts` y `apiNormativa` en `frontend/src/api/cliente.ts`.

### Dominio de Normativa (principio II)

- [ ] T003 [P] Prueba primero, en `PNOR/dominio/`, sin dobles:
  - `TipoIst` (T1 a T5) y `Severidad` (`DURA`, `BLANDA`);
  - `FilaCargaHoraria`: horas enteras y no negativas; «sin valor» distinto de cero; mínimo ≤ clase ≤ máximo; la suma de clase, otras de docencia, gestión y el máximo de investigación y vinculación no supera la dedicación total (el cuidador sustituto, 18 + 12 = 30 de 40, es válido);
  - `TablaCargaHoraria`: rechaza un perfil y tipo repetidos y un perfil a la vez en `TODOS` y en un tipo concreto; `filaPara(GESTOR, T2)` da la fila de T2 y `filaPara(DOCENTE_TC, T2)` la de `TODOS`; sin fila, vacío;
  - `Regla`: identificador `RN-NN`, severidad opcional, al menos un parámetro;
  - `VersionNormativa`: número ≥ 1, código, origen, vigencia, tipo de IST, tabla y reglas; una versión nueva registra `VersionNormativaCreada` con su número y su código; reconstruirla desde la base no lo registra.
- [ ] T004 Crear en `JNOR/dominio/` `TipoIst`, `Severidad`, `FilaCargaHoraria`, `TablaCargaHoraria`, `Regla`, `VersionNormativa` y `VersionNormativaCreada`, con JavaDoc en español que cite RF-19, RN-xx, MD-08 y el modelo de dominio §5.1. T003 pasa.

### La versión 1 en la base (FR-001 a FR-004, FR-010)

- [ ] T005 Crear `NOR/main/resources/db/migration/normativa/V2__politica_2024.sql` según data-model.md:
  - las tablas `version_normativa`, `fila_carga_horaria` (con `orden`), `regla`, `parametro_regla` y `registro_cambio`, con sus `check`;
  - la versión 1 (`SENESCYT-2024-03`, «Política SENESCYT 2024 de asignación de carga horaria», vigente desde 2024-03-11, `T2`);
  - las 22 filas de `07_politica_carga_horaria.csv`, en su orden, con «sin valor» (`null`) donde la carga 7 está vacía;
  - las 7 reglas y los 29 parámetros de research §4, con sus severidades;
  - el registro `VERSION_CREADA` por `sistema`.
- [ ] T006 Prueba primero, en `INOR/` (Testcontainers):
  - copiar la carga 7 a `NOR/integrationTest/resources/politica-2024.csv`;
  - `PoliticaDe2024IntegrationTest`: después de migrar hay una sola versión, la 1, y sus 22 filas coinciden con el archivo **valor por valor** (SC-001); el tipo es T2; están las 7 reglas con sus 29 parámetros y severidades; está el registro `VERSION_CREADA`;
  - `RepositorioVersionesJdbcIntegrationTest`: el repositorio lista las versiones y reconstruye la versión 1 con el agregado, así que cumple sus invariantes; una versión que no existe da vacío; las filas vuelven en el orden de la política;
  - `NormativaEsquemaIntegrationTest`: todas las tablas en el esquema `normativa`, y la versión anterior, que solo conoce V1, arranca sobre el esquema nuevo (D-55).

  Crear `JNOR/puertos/salida/RepositorioVersiones.java` y `JNOR/adaptadores/salida/RepositorioVersionesJdbc.java` con `JdbcClient`. T006 pasa.

### Permiso del Vicerrector (D-66)

- [ ] T007 [P] Prueba primero: en `backend/servicios/roles/src/integrationTest/java/ec/mdd/roles/MatrizD16IntegrationTest.java`, la fila esperada de `VICERRECTOR` gana `PARAMETROS:LECTURA`. Después crear `backend/servicios/roles/src/main/resources/db/migration/roles/V4__vicerrector_lee_normativa.sql`. La prueba pasa.

**Checkpoint**: el contrato, el dominio, la versión 1 en la base y el permiso del Vicerrector están listos; `./gradlew check` y las pruebas de integración de Normativa y Roles en verde.

---

## Phase 3: User Story 1 - El MDD trae la política 2024 como versión 1 (Priority: P1) 🎯 MVP

**Goal**: el Vicerrector académico y el Administrador consultan la versión 1 completa; un docente recibe «Acceso no autorizado» (FR-001 a FR-004, FR-007 a FR-009; SC-001, SC-002 y SC-005).

**Independent Test**: con `vicerrectorado.demo`, abrir «Normativa» y ver la versión 1 con sus 22 filas, el tipo de IST T2 y los parámetros de RN-01 a RN-15.

- [ ] T008 [US1] Prueba primero, en `PNOR/aplicacion/ConsultarNormativaServicioTest.java` (Mockito): listar las versiones por número; consultar la 1; una versión que no existe lanza `VersionNoEncontrada`. Crear el puerto `JNOR/puertos/entrada/ConsultarNormativa.java`, `ConsultarNormativaServicio`, `VersionNoEncontrada` y `JNOR/infraestructura/ConfiguracionNormativa.java`. T008 pasa.
- [ ] T009 [US1] Prueba primero, en `PNOR/adaptadores/entrada/`:
  - `NormativaContratoTest`: las dos operaciones contra el contrato, con la versión completa (las horas sin valor se omiten), y el 404 con `COMUN_RECURSO_NO_ENCONTRADO`;
  - `PermisosNormativaTest`: `VerificadorDePermisos` sobre el contrato de Normativa (SC-005).

  Después crear `JNOR/adaptadores/entrada/NormativaControlador.java`, con `@PreAuthorize("hasAuthority('PERMISO_PARAMETROS:LECTURA')")` en cada método, y la clase raíz de pruebas `PNOR/AplicacionDePruebaNormativa.java`. T009 pasa. `ModulosTest` de `datos` sigue en verde.
- [ ] T010 [P] [US1] Prueba primero, en `frontend/src/pantallas/normativa/Normativa.test.tsx`, con la API simulada:
  - con `PARAMETROS:LECTURA`: la lista de versiones; la cabecera (Política SENESCYT 2024, `SENESCYT-2024-03`, vigente desde el 11/03/2024, Tipo 2); los parámetros agrupados por regla, con la severidad en español («Dura», «Blanda», «—» si no tiene); la tabla con los perfiles en español, «Todos» para `TODOS` y «—» para un valor que la política no da;
  - sin el permiso: «Acceso no autorizado», sin pedir nada a `/api/normativa`, y sin «Normativa» en el menú.

  Crear `frontend/src/pantallas/normativa/` (`Normativa.tsx`, `consultas.ts`, `perfiles.ts`, con un perfil desconocido legible), la ruta `/normativa` con `RutaConPermiso` en `rutas.tsx` y la entrada «Normativa» con su permiso en `navegacion/Marco.tsx`. T010 pasa.
- [ ] T011 [US1] Prueba con Playwright, en `frontend/e2e/normativa.spec.ts`, con la API simulada:
  - `vicerrectorado.demo` entra, abre «Normativa» y ve 22 filas y Tipo 2;
  - `docente.demo` en `/normativa` ve «Acceso no autorizado» sin texto técnico;
  - axe-core sin fallas WCAG 2.1 AA en la pantalla.
- [ ] T012 [US1] Crear `deploy/probar_normativa.sh`, con `shellcheck` limpio y el estilo de `probar_autenticacion.sh`:
  - paso 1: `vicerrectorado.demo` lista una sola versión, la 1, con `SENESCYT-2024-03` y T2;
  - paso 2: la versión 1 tiene 22 filas y 7 reglas, y la fila `DOCENTE_TC` dice 22 h de clase entre 18 y 24 y 18 de otras de docencia;
  - paso 3: `admin.demo` la ve igual; `docente.demo` recibe el 403 común; una versión que no existe da 404.

  Agregarlo a `.github/workflows/perfil-consolidado.yml`, después de `probar_spa.sh`. Correrlo en el perfil y anotarlo.

**Checkpoint**: la versión 1 se consulta por la API y por la pantalla, con el mínimo privilegio.

---

## Phase 4: User Story 2 - La versión 1 rige solo con datos (Priority: P1)

**Goal**: la versión 1 no se duplica ni cambia al reiniciar o desplegar, y ninguna cifra de la política queda en el código (FR-005, FR-006; SC-003 y SC-004).

**Independent Test**: reiniciar `datos` tres veces y comprobar que sigue habiendo una sola versión, igual; revisar que ninguna cifra de la política está en el código de Normativa.

- [ ] T013 [US2] Agregar a `deploy/probar_normativa.sh` el paso 4: guardar la versión 1, reiniciar `datos` tres veces (`docker compose restart datos` y `up --wait`) y comprobar que sigue habiendo exactamente una versión, idéntica (SC-003). Correrlo y anotarlo.
- [ ] T014 [US2] Revisión del principio VI (SC-004): buscar en `JNOR/` y en `frontend/src/pantallas/normativa/` las cifras de la política (por ejemplo `22`, `18`, `40`, `300`, `3500`) y anotar en la evidencia que solo aparecen en `V2__politica_2024.sql` y en las pruebas.

**Checkpoint**: las dos historias pasan de punta a punta.

---

## Phase 5: Cierre

- [ ] T015 Correr:
  - en el backend, `./gradlew check integrationTest`;
  - en el frontend, `npm run lint`, `formato:verificar`, `tipos`, `test -- --run --coverage`, `test:e2e` y `verificar:ruptura`;
  - `shellcheck` sobre `probar_normativa.sh`, y Gitleaks sobre un clon en el scratchpad;
  - `probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh`, `probar_spa.sh` y `deploy/equipo/probar_equipo.sh`.

  Todo en verde, con el 60 % de líneas. Hacer commit y push, abrir el PR en borrador con `Refs #40` y marcarlo listo **después** de que terminen sus runs. Si la concurrencia cancela el run de `ready_for_review`, relanzarlo. Comprobar los checks, `perfil-consolidado` y `5 · imágenes`.
- [ ] T016 Completar `evidencia.md` y la «Definición de terminado» del PR. Fusionarlo con *squash* **con la aprobación del autor**, con la rama al día con `main`. Después:
  - esperar el despliegue automático en el PC («Sano en N s»);
  - el autor entra con `vicerrectorado.demo` y ve la versión 1 en «Normativa» (SC-002);
  - abrir el PR de evidencia con `Closes #40` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup (fase 1)**: T001 es libre.
- **Foundational (fase 2)**:
  - T002 antes que todo lo demás;
  - T003 antes que T004;
  - T005 y T006 después de T004;
  - T007 es libre.
- **US1 (fase 3)**: depende de la fase 2. T008 antes que T009; T010 depende de T002; T011 depende de T010; T012 depende de T009 y T007.
- **US2 (fase 4)**: T013 depende de T012; T014 es libre una vez hecho T009.
- **Cierre (fase 5)**: T015 y T016 dependen de todo.

### Parallel Opportunities

- Fase 2: T003 y T007.
- US1: T010, una vez hechos T002 y T009 en el backend.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el dominio, la versión 1 en la base y el permiso del Vicerrector.
2. La consulta por la API y la pantalla «Normativa».
3. **Validar:** `vicerrectorado.demo` ve la versión 1 con sus 22 filas.

### Incremental

1. **US2:** la versión 1 sobrevive a los reinicios, y el principio VI se revisa.
2. **Cierre:** evidencia, PR, fusión y despliegue.

---

## Notes

- Crear y editar versiones es F04-H02; asociar una versión a un periodo, F02-H01; cargar la política por archivo, F01.
- El evento `VersionNormativaCreada` queda en `registro_cambio`; se publicará entre procesos cuando tenga su primer consumidor (research §5).
- Hacer commit por tarea o por grupo lógico en `012-politica-normativa`, con `Refs #40`.
