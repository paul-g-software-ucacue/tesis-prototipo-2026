# Implementation Plan: Despliegue con Docker Compose en 4 procesos

**Branch**: `002-compose-cuatro-procesos` | **Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-compose-cuatro-procesos/spec.md` (issue #19, B00-H02)

## Summary

Un `compose.yaml` en la raíz levanta, con un solo `docker compose up`, el perfil consolidado de ADR-006:
- PostgreSQL 18.6, con un rol por proceso y un esquema por servicio;
- los 4 procesos (identidad, datos, distributivo y salida), que ensamblan los 12 servicios de la ERS, creados vacíos desde la plantilla (D-35);
- el API Gateway, con Spring Cloud Gateway 2025.1.3.

Las imágenes se construyen desde el código con el `java.Dockerfile` de varias etapas. Cada contenedor tiene un *healthcheck* y un límite de memoria. Cada servicio migra con su propio Flyway en su esquema. Una prueba de integración por servicio comprueba que sus tablas quedan solo en su esquema, y una por proceso comprueba el conjunto de esquemas del proceso. El script `deploy/medir_memoria.sh` y el workflow manual `perfil-consolidado.yml` miden la memoria después de un calentamiento (D-34) y dejan la evidencia de R-19.

Los detalles y sus alternativas están en [research.md](research.md); las decisiones que afectan a varias features están en ADR-013, aprobado.

## Technical Context

**Language/Version**: Java 25 (toolchain de Gradle; imágenes `eclipse-temurin:25-jdk` para compilar y `25-jre` para ejecutar) · YAML (Compose y Actions) · Bash (script de inicialización de PostgreSQL, *healthchecks* y medición) · SQL (migraciones)

**Primary Dependencies**:
- Plataforma: Spring Boot 4.1.1 (`starter-webmvc`, `starter-jdbc`, `starter-actuator` y `starter-flyway`), Flyway 12.4.0 con `flyway-database-postgresql` y el controlador PostgreSQL 42.7.13, todos del BOM de Boot.
- Gateway: Spring Cloud 2025.1.3 (Gateway 5.0.3, `spring-cloud-starter-gateway-server-webflux`).
- Contenedores: Docker Compose v2.24 o posterior y PostgreSQL 18.6 (`postgres:18.6-alpine`).

**Storage**: PostgreSQL 18.6, una base `mdd` con 12 esquemas (uno por servicio) y 4 roles (uno por proceso), en un volumen de Compose.

**Testing**:
- JUnit Jupiter 6, AssertJ, ArchUnit y Spring Modulith, como en B00-H01.
- Una prueba unitaria de la configuración de Flyway por servicio.
- `<Servicio>EsquemaIntegrationTest` por servicio, sin Spring, y `EsquemasIntegrationTest` por proceso, con Testcontainers 2.0.5 y PostgreSQL 18.6.
- Se repiten las pruebas de arquitectura existentes.

**Target Platform**:
- Docker Compose sobre Ubuntu Server, compatible con aaPanel (RNF-POR-01), en una VM de 4 vCPU y 8 GiB (D-36). La referencia es `ubuntu-latest` en GitHub Actions.
- En local, Docker Engine con Docker Compose v2, de los paquetes de Fedora.

**Project Type**: monorepo web: multiproyecto Gradle en `backend/`, despliegue en `compose.yaml` y `deploy/`.

**Performance Goals**: los 6 contenedores quedan sanos en 180 s o menos, ya construidas las imágenes (SC-001; es lo que espera `desplegar.sh`).

**Constraints**:
- Consumo total por debajo de 8 GiB después del calentamiento, con alerta en 7 GiB (R-19, D-36).
- La suma de los límites iniciales es 5 888 MiB.
- Solo el gateway publica un puerto.
- No hay secretos reales en el repositorio.
- No hay JPA hasta que exista una entidad.

**Scale/Scope**:
- 6 contenedores, 4 procesos, 1 gateway y 12 servicios vacíos.
- 1 Dockerfile que cambia, 1 script de inicialización, 1 script de medición y 1 workflow manual.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Cinco capas | Los 12 servicios salen de la plantilla. La configuración de Flyway vive en `infraestructura` y el dominio queda vacío; `ArquitecturaTest` corre en cada servicio. | ✅ |
| II. DDD según el subdominio | Los servicios se crean sin lógica; la profundidad de DDD llega con cada historia. | ✅ N/A |
| III. Lenguaje ubicuo | No hay términos de dominio nuevos. Los nombres de servicios, esquemas y procesos son los de ADR-006 y ADR-012; la clase técnica `<Servicio>FlywayConfig` sigue ADR-008. | ✅ |
| IV. API primero | No hay endpoints de negocio. La salud de Actuator es operativa y no se publica por el gateway. | ✅ N/A |
| V. Límites de servicio | Un esquema por servicio, una aplicación por proceso y un rol por proceso. `ModulosTest` y `EsquemasIntegrationTest` en cada proceso. | ✅ |
| VI y VII | Esta historia no tiene reglas ni datos de negocio. | ✅ N/A |
| VIII. Pruebas | Prueba unitaria de Flyway y de integración de esquemas por servicio, y de integración de esquemas por proceso, con PostgreSQL real (Testcontainers). Cobertura del 60 % en cada módulo. | ✅ |
| IX. Seguridad | `.env.ejemplo` con valores ficticios y `.env` ignorado. Roles de base por proceso, solo el gateway expuesto, ningún paquete extra en las imágenes y Gitleaks en verde. | ✅ |
| X. Simplicidad | Ni colas, ni caché, ni un `DataSource` por servicio. JPA no entra todavía. El workflow de medición es manual. | ✅ |
| XI. Diseño pragmático | Una configuración explícita de tres líneas por servicio en lugar de una abstracción compartida. Las alternativas descartadas están en research §1 a §4. | ✅ |
| XII. JavaDoc | Las clases `<Servicio>FlywayConfig` y la prueba de esquemas del esqueleto llevan JavaDoc en español. | ✅ |
| XIII. Errores | Esta historia no tiene errores de API. | ✅ N/A |
| §3 Infraestructura y memoria | Docker Compose en la VM de 4 vCPU y 8 GiB (constitución 2.1.0, D-36), un esquema por módulo, límites explícitos por JVM y por contenedor, y medición de R-19 en la ola 0. | ✅ |
| §6.4 ADR | Las decisiones que afectan a varias features (persistencia por servicio, roles por proceso y construcción de imágenes) están en ADR-013, aprobado por el autor el 2026-10-12. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. ADR-013 quedó aprobado el 2026-10-12.

**Revisión después de la fase 1:** pasa. El diseño de [data-model.md](data-model.md) y [contracts/](contracts/) no agrega servicios, capas ni infraestructura fuera de lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/002-compose-cuatro-procesos/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: contenedores, procesos, servicios, esquemas, roles y medición
├── quickstart.md            # fase 1
├── contracts/
│   ├── compose.md               # contenedores, puertos, salud, límites, variables y orden de arranque
│   ├── persistencia.md          # esquemas, roles, Flyway por servicio y comprobación de esquemas
│   └── medicion-r19.md          # calentamiento, medición y formato de la evidencia
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
compose.yaml                                 # nuevo: postgres, identidad, datos, distributivo, salida y gateway
.env.ejemplo                                 # nuevo: valores ficticios
.gitignore                                   # cambia: + .env
.github/
├── dependabot.yml                           # cambia: + ecosistema docker-compose en /
└── workflows/
    └── perfil-consolidado.yml               # nuevo: manual y en PR que cambian el despliegue; mide R-19
deploy/
├── docker/java.Dockerfile                   # cambia: etapa de compilación desde el código (ARG MODULO, NOMBRE)
├── postgres/01-esquemas.sh                  # nuevo: 4 roles y 12 esquemas (docker-entrypoint-initdb.d)
└── medir_memoria.sh                         # nuevo: calentamiento, docker stats y tabla de evidencia
backend/
├── .dockerignore                            # nuevo
├── settings.gradle                          # cambia: include de 12 servicios, 4 procesos y gateway
├── gradle.properties                        # cambia: + versionSpringCloud=2025.1.3
├── buildSrc/src/main/groovy/
│   ├── mdd.servicio.gradle                  # cambia: + spring-boot-starter-flyway y flyway-database-postgresql
│   └── mdd.aplicacion.gradle                # cambia: + spring-boot-starter-actuator
├── plantillas/
│   ├── servicio/                            # cambia: + <Servicio>FlywayConfig, V1__esquema_servicio.sql y su prueba
│   └── aplicacion/                          # cambia: + webmvc, jdbc y postgresql; application.yaml; EsquemasIntegrationTest
├── servicios/                               # nuevo (D-35): los 12 servicios creados desde la plantilla
│   ├── usuarios/  roles/  notificaciones/
│   ├── cargas/  docentes/  academico/  normativa/
│   ├── generacion/  validacionocs/  firma/
│   └── reporting/  dashboard/
├── apps/                                    # nuevo: los 4 procesos creados desde la plantilla
│   ├── identidad/  datos/  distributivo/  salida/
└── gateway/                                 # nuevo: Spring Cloud Gateway, solo salud en esta historia
```

**Structure Decision**: se mantienen las rutas de ADR-012 (`servicios/<servicio>`, `apps/<proceso>` y `gateway`), que también espera el `cd.yml` pendiente. El despliegue local vive en la raíz (`compose.yaml` y `.env.ejemplo`) y sus piezas en `deploy/`. El `compose.vps.yml` del VPS es de B00-H08.

## Fases de implementación (guía para /speckit-tasks)

1. **ADR-013:** aprobado el 2026-10-12 (`docs/decisiones.md`).
2. **Plantillas primero:**
   - `mdd.servicio` (Flyway) y `mdd.aplicacion` (Actuator);
   - el esqueleto de servicio con `FlywayConfig`, la migración V1 y su prueba, primero en rojo;
   - el esqueleto de aplicación con dependencias, `application.yaml` y `EsquemasIntegrationTest`;
   - validación con copias temporales, como en B00-H01.
3. **Los 12 servicios y los 4 procesos** creados desde los esqueletos (D-35), más `settings.gradle`. `./gradlew build integrationTest` en verde.
4. **Gateway:** `backend/gateway` con Spring Cloud 2025.1.3, solo salud.
5. **Imágenes:** `java.Dockerfile` con la etapa de compilación y `backend/.dockerignore`.
6. **Compose:** `compose.yaml`, `.env.ejemplo`, `deploy/postgres/01-esquemas.sh` y `.gitignore`. `docker compose up -d --wait` en local, con Docker Engine y Docker Compose v2.
7. **Medición:** `deploy/medir_memoria.sh`, `perfil-consolidado.yml` (manual y en los pull requests que cambian el despliegue) y Dependabot. El run del pull request es la evidencia de los escenarios 1 y 2.
8. **Esquemas y permisos:** comprobar en el perfil levantado que un rol no puede crear tablas en el esquema de otro proceso.
9. **Cierre:** `evidencia.md`, pull request con la CI en verde, actualización de las plantillas en el README, y fusión con la aprobación del autor.

## Complexity Tracking

No hay violaciones de la constitución. ADR-013 está aprobado (§6.4).
