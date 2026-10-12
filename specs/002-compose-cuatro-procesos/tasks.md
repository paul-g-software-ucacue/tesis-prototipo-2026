---

description: "Tareas de B00-H02: despliegue con Docker Compose en 4 procesos"
---

# Tasks: Despliegue con Docker Compose en 4 procesos

**Input**: documentos de diseño en `specs/002-compose-cuatro-procesos/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas, y los escenarios son pruebas. Las pruebas de Flyway y de esquemas se escriben antes que su configuración. Los escenarios 1 y 2 se demuestran levantando el perfil en local y en el workflow manual.

**Decisiones**: D-34, D-35, D-36 y ADR-013 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**: por historia de usuario de la especificación: US1, todo se levanta con un comando; US2, cabe en la memoria; US3, un esquema por servicio.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Las rutas son relativas a la raíz del repositorio. Gradle se ejecuta en `backend/` con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`.

## Convenciones para todas las tareas

- **Servicios y procesos** (ADR-006, research §11):

  | Proceso | Servicios |
  |---|---|
  | identidad | usuarios, roles, notificaciones |
  | datos | cargas, docentes, academico, normativa |
  | distributivo | generacion, validacionocs, firma |
  | salida | reporting, dashboard |

- **Nombres:**
  - paquete `ec.mdd.<servicio>`;
  - esquema `<servicio>`;
  - clase `<Servicio>FlywayConfig`, con la primera letra en mayúscula: `ValidacionocsFlywayConfig`, `AcademicoFlywayConfig`, etc.
- **JavaDoc** en español, sin `@author` (principio XII).
- **Formato:** `spotlessApply` antes de cada commit. Es google-java-format en estilo AOSP (D-33).
- **Evidencia:** cada comprobación se anota en `specs/002-compose-cuatro-procesos/evidencia.md`.
- **Valores:** ningún secreto real; solo valores ficticios (principio IX).

---

## Phase 1: Setup

**Purpose**: versiones, convenciones y archivos base.

- [X] T001 Agregar `versionSpringCloud=2025.1.3` a `backend/gradle.properties`, con un comentario: «primera línea compatible con Spring Boot 4.1 desde 2025.1.2; research §5».
- [X] T002 [P] En `backend/buildSrc/src/main/groovy/mdd.servicio.gradle`, agregar:
  - en `dependencies`: `implementation 'org.springframework.boot:spring-boot-starter-flyway'` y `runtimeOnly 'org.flywaydb:flyway-database-postgresql'`;
  - en la suite `integrationTest`: `runtimeOnly 'org.postgresql:postgresql'`.

  Todo con las versiones del BOM. Actualizar el comentario de cabecera: «cada servicio tiene su esquema y su Flyway (D-35, ADR-013)».
- [X] T003 [P] Agregar `implementation 'org.springframework.boot:spring-boot-starter-actuator'` a `backend/buildSrc/src/main/groovy/mdd.aplicacion.gradle`, con un comentario: «salud de Actuator para los *healthchecks*; research §6».
- [X] T004 [P] Agregar `.env` a `.gitignore`, con el comentario «# Variables locales; el ejemplo versionado es .env.ejemplo».
- [X] T005 [P] Crear `backend/.dockerignore` que excluya `**/build/`, `.gradle/`, `**/.gradle/` y `buildSrc/build/`.
- [X] T006 [P] Crear `specs/002-compose-cuatro-procesos/evidencia.md` con estas secciones:
  - entorno;
  - construcción y pruebas;
  - escenario 1 (perfil levantado);
  - escenario 2 (mediciones de R-19, con el formato de [contracts/medicion-r19.md](contracts/medicion-r19.md));
  - escenario 3 (esquemas y permisos);
  - notas.
- [X] T007 Comprobar que Docker responde sin `sudo`: `docker version`, y `docker compose version` 2.24 o posterior. El autor lo instala antes con los paquetes de Fedora (quickstart, requisitos previos). Anotar las versiones en la sección «entorno» de `specs/002-compose-cuatro-procesos/evidencia.md`. Después ejecutar `./gradlew build` en `backend/`; tiene que quedar en verde.

---

## Phase 2: Foundational (bloquea a todas las historias)

**Purpose**: actualizar las plantillas y crear los 12 servicios, los 4 procesos y el gateway (D-35, ADR-013).

**⚠️ CRITICAL**: ninguna historia puede empezar sin esta fase.

### Tests (primero) ⚠️

- [X] T008 [P] Crear `backend/plantillas/servicio/src/test/java/ec/mdd/servicio/ServicioFlywayConfigTest.java`. Sin base de datos, con un `DataSource` que no se conecta (por ejemplo, `DriverManagerDataSource` con una URL que nunca se usa), comprueba que el `Flyway` que devuelve `new ServicioFlywayConfig().flywayServicio(dataSource)` cumple:
  - `getConfiguration().getSchemas()` es exactamente `["servicio"]`;
  - `getConfiguration().getLocations()` es exactamente `classpath:db/migration/servicio`.

  Los nombres de los métodos van en estilo Dado–Cuando–Entonces.
- [X] T009 [P] Crear `backend/plantillas/servicio/src/integrationTest/java/ec/mdd/servicio/ServicioEsquemaIntegrationTest.java`, sin contexto de Spring:
  1. levanta `postgres:18.6-alpine` con `PostgreSQLContainer` de Testcontainers;
  2. arma un `DataSource` con sus datos de conexión;
  3. ejecuta `new ServicioFlywayConfig().flywayServicio(ds).migrate()`;
  4. comprueba en `information_schema.tables` (sin `pg_catalog` ni `information_schema`) que todas las tablas están en el esquema `servicio` y que existe `servicio.flyway_schema_history` ([contracts/persistencia.md](contracts/persistencia.md)).
- [X] T010 [P] Crear `backend/plantillas/aplicacion/src/integrationTest/java/ec/mdd/EsquemasIntegrationTest.java`:
  - `@SpringBootTest(webEnvironment = NONE)` con un `PostgreSQLContainer` (`postgres:18.6-alpine`) anotado `@ServiceConnection`;
  - obtiene los esquemas esperados de todos los beans `org.flywaydb.core.Flyway` del contexto;
  - comprueba en `information_schema.tables` que (1) cada esquema esperado tiene `flyway_schema_history`, (2) no hay tablas en `public` y (3) no hay tablas en esquemas que no estén en el conjunto esperado.

  JavaDoc en español que explique que la atribución por servicio la hace la prueba de cada servicio.

### Plantillas

- [X] T011 Crear `backend/plantillas/servicio/src/main/java/ec/mdd/servicio/infraestructura/ServicioFlywayConfig.java`:
  - un `@Configuration(proxyBeanMethods = false)` con `@Bean(initMethod = "migrate") public Flyway flywayServicio(DataSource dataSource)`;
  - devuelve `Flyway.configure().dataSource(dataSource).schemas("servicio").locations("classpath:db/migration/servicio").load()`;
  - JavaDoc en español: el servicio migra solo en su esquema y el registro de migraciones queda en ese esquema (ADR-013).

  Así T008 pasa.
- [X] T012 Crear `backend/plantillas/servicio/src/main/resources/db/migration/servicio/V1__esquema_servicio.sql`. Solo lleva comentarios SQL: «Esquema del servicio servicio (D-35). Las tablas de negocio llegan con sus historias, como V2 y siguientes.» No crea ninguna tabla.
- [X] T013 Modificar `backend/plantillas/aplicacion/build.gradle`: agregar `implementation 'org.springframework.boot:spring-boot-starter-webmvc'` y `implementation 'org.springframework.boot:spring-boot-starter-jdbc'`, y en tiempo de ejecución `runtimeOnly 'org.postgresql:postgresql'`.
- [X] T014 Crear `backend/plantillas/aplicacion/src/main/resources/application.yaml` con:

  ```yaml
  spring.application.name: proceso
  spring.datasource.username: proceso
  spring.datasource.password: ${DB_CLAVE_PROCESO}
  spring.datasource.url: ${SPRING_DATASOURCE_URL}
  server.port: 8080
  management.endpoints.web.exposure.include: health
  management.endpoint.health.probes.enabled: true
  management.endpoint.health.group.readiness.include: readinessState,db
  ```

  `proceso` y `DB_CLAVE_PROCESO` son los marcadores que se reemplazan al copiar ([contracts/compose.md](contracts/compose.md)).
- [X] T015 Actualizar `backend/plantillas/README.md`.
  - **Crear un servicio:** renombrar `ServicioFlywayConfig`, `ServicioFlywayConfigTest` y `ServicioEsquemaIntegrationTest` a `<Servicio>…`, la carpeta `db/migration/servicio` a `db/migration/<servicio>`, el archivo `V1__esquema_servicio.sql` y los literales `"servicio"`.
  - **Crear un proceso:** reemplazar `proceso` y `DB_CLAVE_PROCESO` en `application.yaml`, y agregar el rol en `deploy/postgres/01-esquemas.sh`.
- [X] T016 Validar las plantillas con copias temporales, sin commit:
  1. Copiar `servicios/prueba` y `apps/procesoprueba`, con todos los renombres de T015.
  2. Incluirlas en `settings.gradle` y declarar el servicio como dependencia del proceso.
  3. Ejecutar `./gradlew build integrationTest`; Testcontainers usa el Docker local.
  4. Las tres pruebas nuevas tienen que pasar y la cobertura tiene que ser del 60 % o más. `EsquemasIntegrationTest` tiene que arrancar aunque `DB_CLAVE_PROCESO` no tenga valor, porque `@ServiceConnection` tiene prioridad sobre `spring.datasource.*`.
  5. Revertir `settings.gradle` y borrar las copias.

### Servicios, procesos y gateway

- [X] T017 Crear los 12 servicios en `backend/servicios/<servicio>/`, copiando `backend/plantillas/servicio` con los renombres de T015 para cada nombre de la tabla de convenciones. Agregar los 12 `include 'servicios:<servicio>'` a `backend/settings.gradle`, en lugar del comentario de que se incluyen con sus historias.
- [X] T018 Crear los 4 procesos en `backend/apps/<proceso>/`, copiando `backend/plantillas/aplicacion`:
  - renombrar `ProcesoAplicacion` a `<Proceso>Aplicacion`, también en `ModulosTest`;
  - en `application.yaml`, poner el nombre del proceso, el usuario `<proceso>` y la contraseña `${DB_CLAVE_<PROCESO>}`;
  - en `build.gradle`, agregar `implementation project(':servicios:<servicio>')` por cada servicio del proceso, según la tabla de convenciones.

  Agregar los 4 `include 'apps:<proceso>'` a `backend/settings.gradle`.
- [X] T019 Crear el gateway en `backend/gateway/`:
  - `build.gradle` con `plugins { id 'mdd.aplicacion' }`, `implementation platform("org.springframework.cloud:spring-cloud-dependencies:${versionSpringCloud}")` e `implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'`;
  - `src/main/java/ec/mdd/gateway/GatewayAplicacion.java`, con `@SpringBootApplication` y JavaDoc: «en B00-H02 solo arranca y expone su salud; las rutas y el JWT son de B00-H03»;
  - `src/main/resources/application.yaml`, con `spring.application.name: gateway`, `server.port: 8080`, salud expuesta y *probes* activas;
  - `src/test/java/ec/mdd/gateway/GatewayAplicacionTest.java`, con `@SpringBootTest` que carga el contexto. Así se detecta si Spring Cloud no fuera compatible con Boot 4.1.1.

  Agregar `include 'gateway'` a `backend/settings.gradle`.
- [X] T020 Ejecutar `./gradlew spotlessApply build integrationTest`. Tiene que quedar todo en verde:
  - 12 `<Servicio>FlywayConfigTest` y 12 `<Servicio>EsquemaIntegrationTest`;
  - 4 `ModulosTest` y 4 `EsquemasIntegrationTest`;
  - `GatewayAplicacionTest`;
  - cobertura del 60 % o más en cada módulo.

  Registrar los conteos en `specs/002-compose-cuatro-procesos/evidencia.md`.

**Checkpoint**: el código de los 6 componentes construye y sus pruebas pasan.

---

## Phase 3: User Story 1 - Todo el perfil consolidado se levanta con un comando (Priority: P1) 🎯 MVP

**Goal**: un `docker compose up` desde un clon limpio deja los 6 contenedores sanos (escenario 1; FR-001 a FR-006 y FR-012).

**Independent Test**: `git clone` en un directorio temporal → `docker compose up -d --wait` → los 6 contenedores en estado `healthy` (quickstart §2).

### Implementation for User Story 1

- [X] T021 [US1] Reescribir `deploy/docker/java.Dockerfile` en tres etapas:
  1. `FROM eclipse-temurin:25-jdk AS compilacion`, con `ARG MODULO` y `ARG NOMBRE`, `WORKDIR /src`, `COPY . .` y `RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon :${MODULO}:bootJar`.
  2. `extraccion`: `COPY --from=compilacion /app.jar app.jar` y extrae las capas, como hoy.
  3. La imagen final queda igual: `eclipse-temurin:25-jre`, usuario 10001 y `JarLauncher`.

  Un `COPY` no puede transformar `MODULO`, así que la etapa 1 termina el mismo `RUN` con `cp "$(echo "${MODULO}" | tr : /)/build/libs/${NOMBRE}.jar" /app.jar`. Comentario de cabecera: «compila desde el código (ADR-013); B00-H08 adapta cd.yml».
- [X] T022 [P] [US1] Crear `deploy/postgres/01-esquemas.sh`, ejecutable, con `set -euo pipefail` y `psql -v ON_ERROR_STOP=1`:
  - crea los 4 roles con `LOGIN` y su contraseña tomada de `DB_CLAVE_IDENTIDAD`, `DB_CLAVE_DATOS`, `DB_CLAVE_DISTRIBUTIVO` y `DB_CLAVE_SALIDA`;
  - crea los 12 esquemas con `AUTHORIZATION <rol de su proceso>`;
  - hace `REVOKE CREATE ON SCHEMA public FROM PUBLIC`.

  Valores de la tabla de convenciones y de [contracts/persistencia.md](contracts/persistencia.md).
- [X] T023 [P] [US1] Crear `.env.ejemplo` en la raíz. Lleva `POSTGRES_PASSWORD`, `POSTGRES_DB=mdd`, los cuatro `DB_CLAVE_*`, `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mdd` y `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=60`. Los valores son ficticios y obvios (`cambiar-en-local-…`), con un comentario de cabecera: «valores de ejemplo para local; no usar en el equipo de despliegue».
- [X] T024 [US1] Crear `compose.yaml` en la raíz según [contracts/compose.md](contracts/compose.md):
  - los 6 servicios, con `env_file: [{path: .env.ejemplo, required: true}, {path: .env, required: false}]`;
  - `build` con contexto `backend`, `dockerfile: ../deploy/docker/java.Dockerfile` y los `args` `MODULO` y `NOMBRE`;
  - `deploy.resources.limits.memory`: 512m para `postgres` y cada proceso, y 384m para `gateway`, que suman 2 944 MiB;
  - en `postgres`, el *healthcheck* `pg_isready -h 127.0.0.1 -U postgres -d mdd`, por TCP, para que no quede sano con el servidor temporal de `01-esquemas.sh`;
  - los *healthchecks* del contrato, con `interval: 10s`, `timeout: 5s`, `retries: 12` y `start_period: 60s`;
  - en los procesos, `depends_on: postgres: {condition: service_healthy}`;
  - en `postgres`, el volumen `datos-postgres` y el script montado en `/docker-entrypoint-initdb.d/01-esquemas.sh:ro`;
  - solo el gateway publica `8080:8080`.
- [X] T025 [US1] Construir las 5 imágenes con `time docker compose build` y anotar en `specs/002-compose-cuatro-procesos/evidencia.md` el tiempo y el tamaño de cada imagen (`docker image ls`). Así la medición de T026 (SC-001) parte de imágenes ya construidas.
- [X] T026 [US1] Validar el escenario 1 en un clon limpio, según el quickstart §2:
  1. Hacer `git clone` de la rama en el directorio temporal de la sesión y ejecutar `docker compose up -d --wait`.
  2. Los 6 contenedores tienen que estar `healthy`.
  3. `curl localhost:8080/actuator/health/readiness` tiene que dar `UP`.
  4. `docker compose port postgres 5432` no tiene que devolver nada.
  5. Con las imágenes construidas, `docker compose down && time docker compose up -d --wait` tiene que tardar 180 s o menos (SC-001).

  Registrar los resultados en `specs/002-compose-cuatro-procesos/evidencia.md`. Anotar también que el caso límite «un contenedor supera su límite de memoria» quedaría visible en `docker compose ps` como detenido o no sano; no se fuerza. Si un componente no queda sano, revisar `docker compose logs <componente>` y corregir antes de seguir.
- [X] T027 [US1] Comprobar el caso límite «falta el archivo de variables»: con `.env.ejemplo` renombrado temporalmente, `docker compose up` tiene que fallar con un mensaje que nombra el archivo. Restaurarlo y anotar el resultado.

**Checkpoint**: el perfil consolidado se levanta con un comando.

---

## Phase 4: User Story 2 - El perfil consolidado cabe en la memoria del equipo (Priority: P1)

**Goal**: medir la memoria después del calentamiento, comprobar que queda por debajo de 8 GiB (D-36) y registrarlo como evidencia de R-19 (escenario 2; FR-007 y FR-008; D-34).

**Independent Test**: `deploy/medir_memoria.sh` termina con código 0 e imprime la tabla de [contracts/medicion-r19.md](contracts/medicion-r19.md).

### Implementation for User Story 2

- [X] T028 [US2] Crear `deploy/medir_memoria.sh`, ejecutable, con `set -euo pipefail`, según [contracts/medicion-r19.md](contracts/medicion-r19.md):
  1. `docker compose up -d --wait`; si falla, `exit 2`. Con `set -e`, ese paso va dentro de un `if !` para no salir con 1.
  2. Calentamiento: 20 llamadas a `/actuator/health/readiness` en `identidad`, `datos`, `distributivo`, `salida` y `gateway`, con `docker compose exec -T <c> bash -c '<comando del healthcheck>'`.
  3. `sleep 30`.
  4. Una sola lectura de `docker stats --no-stream --format '{{.Name}};{{.MemUsage}}' $(docker compose ps -q)`, solo de los contenedores del proyecto. Traducir cada nombre a su servicio con `docker compose ps --format '{{.Name}};{{.Service}}'`. Normalizar KiB, MiB y GiB a MiB.
  5. La tabla Markdown con fecha ISO 8601, `git rev-parse --short HEAD`, el entorno (`docker version --format '{{.Server.Version}}'`), los límites, el total en MiB y GiB, el resultado y la alerta de R-19 si pasa de 7 GiB.

  Códigos de salida: 0 si el total es menor que 8 GiB, 1 si llega a 8 GiB o más, 2 si un componente no está sano.
- [X] T029 [US2] Crear `.github/workflows/perfil-consolidado.yml`:
  - `on`: `workflow_dispatch` y `pull_request` (tipos `opened`, `synchronize`, `reopened` y `ready_for_review`) con `paths: [compose.yaml, .env.ejemplo, 'deploy/**', '.github/workflows/perfil-consolidado.yml']`;
  - el job con `if: github.event_name == 'workflow_dispatch' || github.event.pull_request.draft == false`, `permissions: contents: read`, `runs-on: ubuntu-latest` y `timeout-minutes: 30`, como los jobs de la CI; no es un check obligatorio;
  - pasos: `actions/checkout` → `time docker compose build` (tiempo de construcción) → `time docker compose up -d --wait`, ya sin construir, que es la referencia de SC-001 → `deploy/medir_memoria.sh | tee medicion.md`, en un paso con `shell: bash` para que `pipefail` conserve el código del script → `cat medicion.md >> "$GITHUB_STEP_SUMMARY"` → `actions/upload-artifact` con el nombre `medicion-r19` y 90 días de retención (`if: always()`) → `docker compose down -v` (`if: always()`).

  Fijar las acciones con `pinact run` y validar con actionlint, como en B00-H01.
- [X] T030 [P] [US2] Agregar a `.github/dependabot.yml` el ecosistema `docker-compose` con `directory: /` y `schedule: { interval: weekly }`, para que proponga las actualizaciones de `postgres:18.6-alpine`.
- [X] T031 [US2] Medir en local con el perfil de T026: `deploy/medir_memoria.sh | tee` a un archivo en el directorio temporal de la sesión, y pegar la tabla en `specs/002-compose-cuatro-procesos/evidencia.md` (entorno: Docker local). Anotar que en local cada JVM ve 12 núcleos y en el equipo de despliegue verá 4: con más hilos, la medición local sale algo más alta, así que la diferencia deja margen a favor. Según el total:
  - si es de 8 GiB o más, ajustar los límites o `JAVA_TOOL_OPTIONS` (research §8) y repetir;
  - si sigue sin caber, detenerse y proponer al autor una decisión nueva (alternativa de R-19: Nginx en lugar de Spring Cloud Gateway).
- [ ] T032 [US2] Cuando el pull request quede listo para revisión (T038), esperar el run de `perfil-consolidado` (`gh run list --workflow perfil-consolidado.yml --branch 002-compose-cuatro-procesos`). `gh workflow run` no sirve antes de fusionar, porque GitHub solo ejecuta a mano los workflows que están en la rama principal. Pegar en `specs/002-compose-cuatro-procesos/evidencia.md` la tabla del resumen (entorno: Docker en `ubuntu-latest`), el enlace al run, el tiempo de construcción y el tiempo de `up --wait`. Este último es la referencia de SC-001 en un equipo más parecido al de despliegue que el local. Si la construcción en paralelo resulta lenta, construir una imagen a la vez. Es la medición de referencia de R-19 y, además, la prueba del escenario 1 en un clon limpio con Docker.

**Checkpoint**: R-19 está medido y registrado.

---

## Phase 5: User Story 3 - Un esquema por servicio (Priority: P2)

**Goal**: cada servicio crea sus tablas solo en su esquema, y un proceso no puede tocar los esquemas de otro (escenario 3; FR-009 a FR-011; ADR-013).

**Independent Test**: `./gradlew integrationTest` (12 pruebas de servicio y 4 de proceso) y la consulta de esquemas sobre el perfil levantado (quickstart §4).

### Implementation for User Story 3

- [X] T033 [US3] Comprobar el caso límite «una migración apunta al esquema de otro servicio»:
  1. Agregar temporalmente `backend/servicios/normativa/src/main/resources/db/migration/normativa/V2__intruso.sql` con `create table docentes.intruso(id int);`.
  2. `./gradlew :servicios:normativa:integrationTest` tiene que fallar en `NormativaEsquemaIntegrationTest`.
  3. Borrar el archivo, comprobar que la prueba vuelve a pasar y anotar el mensaje de la falla en `specs/002-compose-cuatro-procesos/evidencia.md`.
- [X] T034 [US3] Sobre el perfil levantado, ejecutar la consulta de esquemas del quickstart §4. Tiene que haber 12 filas `<servicio> | flyway_schema_history` y ninguna en `public`. Pegar la salida en `specs/002-compose-cuatro-procesos/evidencia.md`.
- [X] T035 [US3] Comprobar los permisos entre procesos, según el quickstart §4: como rol `identidad`, `create table normativa.intruso(id int);` tiene que responder `permission denied for schema normativa`. Repetirlo como rol `salida` contra `firma`. Anotar los resultados en `specs/002-compose-cuatro-procesos/evidencia.md`.
- [X] T036 [US3] Comprobar la segunda ejecución (SC-004): `docker compose down && docker compose up -d --wait`. Todo tiene que quedar sano, y en `docker compose logs <proceso>` Flyway tiene que decir que cada esquema está al día, sin migraciones nuevas. Anotar el resultado en `specs/002-compose-cuatro-procesos/evidencia.md`.

**Checkpoint**: el aislamiento de esquemas está demostrado de forma automática y sobre el perfil.

---

## Phase 6: Polish & cierre

- [X] T037 Revisar el JavaDoc (principio XII) en las 12 `<Servicio>FlywayConfig`, en `GatewayAplicacion`, en los `<Proceso>Aplicacion` y en `EsquemasIntegrationTest`. Ejecutar `spotlessApply`. Comprobar con `git status` que no hay archivos temporales.
- [ ] T038 Hacer commit y push, y abrir el pull request de `002-compose-cuatro-procesos` hacia `main` **en borrador**, con la plantilla y `Refs #19`. Cuando termine el push, marcarlo como listo. Comprobar que `2 · test-integracion`, `4 · sbom-backend` y `4 · sca` están en éxito y no *skipped* (nota de la carrera de B00-H01), y que `3 · secretos` pasa con `.env.ejemplo`. Si Gitleaks marca `.env.ejemplo`, agregar una excepción acotada en `.gitleaks.toml`, con su motivo, y anotarla en las notas de `specs/002-compose-cuatro-procesos/evidencia.md`.
- [ ] T039 Completar `specs/002-compose-cuatro-procesos/evidencia.md` y marcar la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**, en un pull request de evidencia aparte si hace falta, como en B00-H01. Cerrar el issue #19 con un comentario que enlace la evidencia (`Closes #19`) y borrar la rama fusionada.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: sin dependencias.
- **Foundational (fase 2)**: depende de la fase 1. Las pruebas (T008 a T010) van antes de su configuración (T011 a T014). La validación T016 va antes de crear los servicios (T017 a T019), y T020 cierra la fase.
- **US1 (fase 3)**: depende de la fase 2. T021 a T024 se pueden hacer en cualquier orden entre sí, salvo T024, que usa T021 a T023. T025 va antes de T026 y T027.
- **US2 (fase 4)**: depende de US1, porque necesita el perfil levantado. T032 depende de T038, porque el workflow corre en el pull request listo para revisión.
- **US3 (fase 5)**: T033 depende solo de la fase 2. T034 a T036 dependen de US1.
- **Cierre (fase 6)**: depende de US1, US2 (salvo T032) y US3. T032 se completa entre T038 y T039.

### Parallel Opportunities

- Fase 1: T002 a T006.
- Fase 2: T008, T009 y T010 (pruebas en archivos distintos).
- US1: T022 y T023 mientras se reescribe el Dockerfile (T021).
- US2: T030 en cualquier momento.
- Como trabajas solo (límite de 1 en «Implementar»), [P] indica orden libre, no trabajo simultáneo.

---

## Parallel Example: Phase 2

```bash
Task: "ServicioFlywayConfigTest en backend/plantillas/servicio/src/test/java/ec/mdd/servicio/"
Task: "ServicioEsquemaIntegrationTest en backend/plantillas/servicio/src/integrationTest/java/ec/mdd/servicio/"
Task: "EsquemasIntegrationTest en backend/plantillas/aplicacion/src/integrationTest/java/ec/mdd/"
```

---

## Implementation Strategy

### MVP (US1)

1. Fases 1 y 2: el código de los 6 componentes construye y se prueba.
2. Fase 3: `docker compose up` deja todo sano desde un clon limpio.
3. **Validar**: quickstart §2.

### Incremental

1. **US2:** medición en local y de referencia en el workflow. Si R-19 no se cumple, conviene saberlo aquí, antes de seguir.
2. **US3:** caso límite de atribución, consulta de esquemas, permisos y segunda ejecución.
3. **Cierre:** pull request, evidencia y fusión.

---

## Notes

- Docker Engine lo instala el autor con `sudo` antes de T007 (quickstart, requisitos previos).
- La segunda medición de R-19, después de iniciar sesión, es de B00-H03 (D-34). Esta historia no la hace.
- Los archivos temporales de T016, T027 y T033 nunca entran en un commit.
- Hacer commit por tarea o por grupo lógico en `002-compose-cuatro-procesos`, con `Refs #19`.
