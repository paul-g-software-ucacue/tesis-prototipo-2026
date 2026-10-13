# Investigación (fase 0): B00-H02 · Despliegue con Docker Compose en 4 procesos

**Fecha:** 2026-10-12 · **Especificación:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Fuentes leídas: constitución 2.0.1, `docs/decisiones.md` (D-28 a D-35, ADR-012), ADR-006 y ADR-008, la ERS (RNF-POR-01 y R-19), `deploy/` (`java.Dockerfile`, `desplegar.sh`, `Caddyfile`), el `cd.yml` pendiente y lo que dejó B00-H01 (plantillas, convenciones y CI). También el BOM de Spring Boot 4.1.1, Maven Central, Docker Hub y las imágenes de Temurin. Las decisiones que afectan a más de una feature están en **ADR-013**, aprobado (§13).

---

## 1. Un solo `docker compose up` desde un clon limpio (FR-001)

- **Decisión:**
  - `compose.yaml` en la raíz del repositorio, para que `docker compose up` funcione sin `-f`.
  - Las imágenes se **construyen desde el código** con un Dockerfile de varias etapas. Una etapa de compilación con `eclipse-temurin:25-jdk` ejecuta `./gradlew :<módulo>:bootJar` y usa una caché de Gradle compartida (`RUN --mount=type=cache`). Después se extraen las capas y la imagen final usa `eclipse-temurin:25-jre`, igual que el `java.Dockerfile` actual.
- **Cambio de `deploy/docker/java.Dockerfile`:** hoy espera un JAR compilado antes (`ARG JAR`), que es lo que hace el `cd.yml` pendiente. Se agrega la etapa de compilación, con `ARG MODULO` (por ejemplo `apps:identidad` o `gateway`) y `ARG NOMBRE` para la ruta del JAR. B00-H08 adapta `cd.yml` para construir con la misma imagen, o pasa a la etapa de extracción con su JAR; queda anotado en ADR-013.
- **Contexto de construcción:** `backend/`, con un `backend/.dockerignore` que excluye `**/build/` y `.gradle/`.
- **Por qué:** el escenario pide que el clon recién hecho se levante «con un solo comando», sin compilar antes. Con la caché de Gradle, las cinco imágenes comparten las dependencias descargadas.
- **Alternativas consideradas:**
  - pedir `./gradlew bootJar` antes de `docker compose up`, que contradice FR-001;
  - un Dockerfile de desarrollo y otro de producción, que terminarían divergiendo;
  - imágenes publicadas, que son de B00-H08.

## 2. Variables de entorno (FR-005)

- **Decisión:** `.env.ejemplo` versionado en la raíz, con valores ficticios. `compose.yaml` lo carga con `env_file` y, si existe, agrega encima un `.env` opcional (`required: false`, disponible desde Compose 2.24). `.env` se agrega a `.gitignore`.
- **Por qué:** así el clon funciona sin editar ni copiar nada (FR-001) y quien quiera sus propios valores los pone en `.env`, que nunca entra al repositorio. Si falta `.env.ejemplo`, Compose falla con un mensaje claro (caso límite «falta el archivo»).
- Las variables llegan a los contenedores por `env_file`; el `compose.yaml` no usa interpolación `${…}`, que solo leería `.env`.
- **Riesgo aceptado en el perfil local:** todos los contenedores reciben las mismas variables, así que cada proceso ve también la clave del superusuario y las de los otros roles. Darle a cada contenedor solo las suyas, sin interpolación, obligaría a repartir `.env.ejemplo` en unos 6 archivos con claves repetidas y rompería el `.env` opcional (principio XI). Se acepta porque los valores son ficticios y los procesos no publican puertos. El `compose.vps.yml` de B00-H08 entrega a cada contenedor solo sus secretos.
- **Gitleaks:** las contraseñas de ejemplo son ficticias y obvias (por ejemplo `cambiar-en-local-identidad`). Si Gitleaks las marca, se agrega una excepción acotada a `.env.ejemplo` en `.gitleaks.toml`, con su motivo.

## 3. PostgreSQL, esquemas y roles

- **Decisión:**
  - Imagen `postgres:18.6-alpine`: la última estable de la 18; la 19 todavía está en beta.
  - Una base `mdd`. Un script `deploy/postgres/01-esquemas.sh` en `docker-entrypoint-initdb.d` crea un **rol por proceso** (identidad, datos, distributivo y salida) y los **12 esquemas**. Cada esquema queda a nombre del rol de su proceso, según ADR-006.
  - Cada proceso se conecta con su rol, así que un proceso no puede crear ni leer tablas de los esquemas de otro proceso.
- **Por qué:** es el mínimo privilegio de la base (principio IX) y hace cumplir el principio V entre procesos sin un `DataSource` por servicio. Dentro de un proceso, el aislamiento entre servicios lo verifica la prueba de §7.
- **Alternativas consideradas:**
  - un solo rol para todo, que es más simple pero no impide nada entre procesos;
  - un rol y un `DataSource` por servicio: 12 *pools* de conexiones, más memoria (R-19) y más configuración, sin un beneficio proporcional en un prototipo (principio XI).
- **Nota:** el script solo corre la primera vez que se crea el volumen de datos; para reiniciar desde cero se usa `docker compose down -v`.

## 4. Flyway por servicio (FR-009)

- **Decisión:** cada servicio tiene:
  - `src/main/resources/db/migration/<servicio>/V1__esquema_<servicio>.sql`, una primera migración que no crea tablas de negocio (D-35);
  - `infraestructura/<Servicio>FlywayConfig`, con un bean `Flyway` configurado con `schemas=<servicio>`, `locations=classpath:db/migration/<servicio>` y el registro de migraciones en su propio esquema. El bean usa `initMethod = "migrate"`.
- **Autoconfiguración:** con un bean `Flyway` propio, la autoconfiguración de Flyway de Spring Boot se retira. Cada proceso migra los esquemas de sus servicios al arrancar.
- **Nombres únicos:** `NormativaFlywayConfig`, `FirmaFlywayConfig`, etc. Si las 12 clases se llamaran igual, los nombres de bean chocarían, porque las clases de un mismo proceso se escanean juntas.
- **Dependencias:** `mdd.servicio` agrega `spring-boot-starter-flyway` y `flyway-database-postgresql`, que gestiona el BOM (Flyway 12.4.0), porque los 12 servicios tienen esquema (D-35, FR-002 de B00-H01).
- **Prueba unitaria por servicio:** comprueba que su `Flyway` apunta solo a su esquema y a su carpeta de migraciones. No necesita base de datos y mantiene la cobertura del 60 %.
- **Plantilla:** la configuración, la migración V1 y la prueba se agregan al esqueleto `backend/plantillas/servicio`, con el marcador `servicio`, para que los servicios creados después ya las traigan.
- **Alternativas consideradas:**
  - un solo Flyway por proceso con varios esquemas: la primera migración de cada servicio quedaría mezclada en un solo historial;
  - una clase genérica compartida: una abstracción para una configuración de tres líneas (principio XI).

## 5. Procesos y gateway

- **Procesos:** `apps/identidad`, `apps/datos`, `apps/distributivo` y `apps/salida`, creados desde `backend/plantillas/aplicacion` y con los servicios de ADR-006 como dependencias.
- **Dependencias de un proceso:** en el esqueleto de aplicación, que se copia a cada proceso, se agregan `spring-boot-starter-webmvc`, `spring-boot-starter-jdbc` y el controlador `org.postgresql:postgresql` (en tiempo de ejecución).
- **`mdd.aplicacion`:** agrega `spring-boot-starter-actuator`, porque los 5 componentes necesitan salud.
- **JPA:** no se agrega hasta que una historia tenga entidades (principio XI). La medición de R-19 de esta historia no incluye Hibernate; B00-H03 y las historias siguientes la repiten.
- **API Gateway:** `backend/gateway`, creado a mano. No usa el esqueleto de proceso porque no ensambla servicios ni necesita `ModulosTest`.
  - Aplica `mdd.aplicacion`, el BOM `spring-cloud-dependencies` **2025.1.3** y `spring-cloud-starter-gateway-server-webflux`.
  - La 2025.1.2 es la primera que agrega compatibilidad con Spring Boot 4.1.0 ([anuncio de Spring Cloud 2025.1.2](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released/)). La 2025.1.3 trae Gateway 5.0.3.
  - En esta historia solo arranca y queda sano; las rutas y el JWT son de B00-H03.
  - La versión queda en `gradle.properties` (`versionSpringCloud`).
- **Nombres:** `apps:identidad`, `apps:datos`, `apps:distributivo`, `apps:salida` y `gateway` no se repiten con ningún servicio (regla del README de plantillas) y coinciden con `cd.yml`.

## 6. Salud de los contenedores (FR-003, FR-004)

- **Procesos y gateway:** Actuator expone solo `health`, con sus grupos `liveness` y `readiness`. En los procesos, `readiness` incluye la base (`db`), así que un proceso no queda sano si no se conecta a PostgreSQL.
- **Comando del *healthcheck*:** las imágenes de Temurin 25 (Ubuntu 26.04) **no traen curl ni wget**. El *healthcheck* usa bash con `/dev/tcp` para hacer un `GET /actuator/health/readiness` y busca `"status":"UP"`. No agrega paquetes a la imagen, que es una superficie de ataque menor, ni arranca una JVM por cada comprobación, lo que falsearía la medición de memoria.
- **PostgreSQL:** `pg_isready -h 127.0.0.1 -U postgres -d mdd`. Con `-h`, la comprobación va por TCP. Mientras corre `01-esquemas.sh`, la imagen levanta un servidor temporal que solo escucha por el socket; sin `-h`, `postgres` quedaría sano antes de tiempo y los procesos fallarían al migrar cuando el servidor se reinicia.
- **Orden de arranque:** los procesos tienen `depends_on` de PostgreSQL con `condition: service_healthy`. El gateway no depende de los procesos, porque en esta historia no enruta.
- **Puertos:** solo el gateway publica el `8080` en la máquina anfitriona. Los procesos y PostgreSQL quedan en la red interna de Compose.
- **Alternativas consideradas:**
  - instalar curl en la imagen, que agrega un paquete solo para la salud;
  - un *healthcheck* en Java, que arranca una JVM cada pocos segundos.

## 7. Comprobación automática de esquemas (FR-011)

- **Decisión:** dos pruebas de integración en la suite `integrationTest`, que corren en el job `2 · test-integracion` de la CI: `<Servicio>EsquemaIntegrationTest` por servicio (atribución, ver abajo) y `EsquemasIntegrationTest` por proceso. La del proceso:
  1. Levanta el contexto del proceso contra un PostgreSQL de Testcontainers con la misma imagen (`postgres:18.6-alpine`) y `@ServiceConnection`.
  2. Obtiene los esquemas esperados de los beans `Flyway` del contexto.
  3. Consulta `information_schema.tables`: cada tabla de servicio está en el esquema de su servicio, cada esquema tiene su propio registro de migraciones y no hay tablas en `public` ni en otro esquema.
- **Plantilla:** la prueba se agrega al esqueleto de aplicación, que es genérico porque toma los esquemas de los beans.
- **Atribución por servicio:** dentro de un proceso, un mismo rol es dueño de los esquemas de todos sus servicios, así que la prueba del proceso no puede saber qué servicio creó una tabla. Por eso cada servicio tiene además `<Servicio>EsquemaIntegrationTest`: migra solo ese servicio contra una base vacía de Testcontainers, sin Spring, y comprueba que todas las tablas creadas están en su esquema. Agrega 12 contenedores efímeros al job de integración, aproximadamente un minuto.
- **Qué no cubre:** los permisos de los roles por proceso (§3). Esos se comprueban con el perfil levantado (quickstart §4).

## 8. Límites de memoria (FR-006)

- **Límites iniciales** (`deploy.resources.limits.memory`, que Docker Compose v2 aplica sin Swarm):

  | Contenedor | Límite |
  |---|---|
  | `postgres` | 512 MiB |
  | `identidad`, `datos`, `distributivo` y `salida` | 512 MiB cada uno |
  | `gateway` | 384 MiB |
  | **Total** | **2 944 MiB** (2,9 GiB) |

  Quedan unos 5,1 GiB de los 8 GiB del equipo para el sistema operativo y para lo que llegue en las olas siguientes (D-36). Si una medición lo pide, los límites se suben mientras el total quede por debajo de 8 GiB.
- **JVM:** `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=60`, como ya anticipaba `java.Dockerfile`. Con menos de 1 792 MiB por contenedor, la JVM elige SerialGC aunque el equipo tenga 4 núcleos; es la opción de menor consumo. No se agregan más ajustes hasta medir.
- **Si la medición no cabe:** primero se ajustan los límites y las opciones de la JVM. Si aun así queda en 8 GiB o más, se aplica la alternativa de R-19 (Nginx en lugar de Spring Cloud Gateway), con una decisión nueva.

## 9. Medición de R-19 (FR-007, FR-008, D-34)

- **Script `deploy/medir_memoria.sh`:**
  1. Espera a que todo esté sano (`docker compose up -d --wait`).
  2. Hace el **calentamiento**: llama 20 veces a la salud de cada proceso y del gateway, con `docker compose exec` y el mismo comando del *healthcheck*.
  3. Espera 30 segundos y toma `docker stats --no-stream`.
  4. Suma el consumo, lo compara con 8 GiB (escenario) y con 7 GiB (disparador de R-19, D-36) e imprime una tabla Markdown con el consumo por contenedor, el total, los límites, la fecha y el commit, lista para pegar en `evidencia.md`.
- **Entorno de referencia:** el workflow `perfil-consolidado.yml` en `ubuntu-latest`, con Docker. Se ejecuta a mano (`workflow_dispatch`) y en los pull requests que cambian el despliegue (filtro `paths`), porque GitHub solo deja ejecutar a mano un workflow que ya está en la rama principal. Desde un *checkout* limpio, que es un clon recién hecho, ejecuta `docker compose up -d --wait` y el script, y sube la tabla como artefacto y resumen del run. También demuestra el escenario 1 en un entorno limpio.
- **Uso de memoria del anfitrión:** el consumo medido no depende de la RAM del anfitrión, porque cada contenedor tiene su límite y cada JVM se dimensiona con él.
- **Cambios solo en `backend/**`:** no disparan la medición, aunque agreguen dependencias que suban la memoria. La medición se repite a mano al cerrar cada ola, y B00-H03 la repite después de iniciar sesión (D-34).
- **No corre en todos los pull requests:** construir las 5 imágenes y levantar el perfil cuesta varios minutos. Solo corre en los que cambian `compose.yaml`, `.env.ejemplo`, `deploy/**` o el propio workflow, y a mano al cerrar la ola. No es un check obligatorio.
- **Código de salida:** el paso de medición declara `shell: bash`, que en Actions agrega `pipefail`; sin eso, `tee` ocultaría un total de 8 GiB o más.
- **En local:** Docker Engine y Docker Compose v2 de los paquetes de Fedora (`moby-engine`, `docker-compose` y `docker-buildx`), que el autor instala con `sudo`. Testcontainers usa el mismo Docker. La medición de referencia es la del workflow.

## 10. Dependabot

- **Decisión:** se agrega el ecosistema `docker-compose` con directorio `/`, para que Dependabot proponga las actualizaciones de `postgres:18.6-alpine` en `compose.yaml`. Las imágenes de Temurin ya las cubre el ecosistema `docker` de `/deploy/docker`.

## 11. Esquemas, roles y servicios (ADR-006)

| Proceso | Rol de la base | Servicios y esquemas |
|---|---|---|
| identidad | `identidad` | `usuarios`, `roles` y `notificaciones` |
| datos | `datos` | `cargas`, `docentes`, `academico` y `normativa` |
| distributivo | `distributivo` | `generacion`, `validacionocs` y `firma` |
| salida | `salida` | `reporting` y `dashboard` |

## 12. Lo que queda fuera

- Las rutas del gateway, el JWT y la segunda medición, después de iniciar sesión, son de B00-H03 (D-34).
- La SPA (B00-H06), Caddy y el despliegue en el VPS con `compose.vps.yml` son de B00-H08.
- JPA y las tablas de negocio llegan con la primera historia de cada servicio.
- El almacenamiento de archivos (RNF-POR-01) llega con la primera historia que guarde archivos.

## 13. ADR-013 propuesto (constitución §6.4)

Las decisiones de §1, §3 y §4 afectan a todas las features futuras: cada servicio con datos y el despliegue de B00-H08. Se proponen como **ADR-013, «Persistencia por servicio y construcción de imágenes desde el código»**:

1. un esquema y un Flyway por servicio, con la configuración en la infraestructura del servicio y la primera migración en `db/migration/<servicio>`;
2. un rol de base por proceso, dueño de los esquemas de sus servicios;
3. el `java.Dockerfile` compila desde el código.

El autor lo aprobó el 2026-10-12; está registrado en `docs/decisiones.md` como **Aceptada**.
