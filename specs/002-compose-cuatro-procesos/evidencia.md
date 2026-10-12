# Evidencia: B00-H02

Registro de las comprobaciones de los tres escenarios de la historia (issue #19), de la medición de R-19 (D-34, D-36) y de los casos límite. Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44, 12 núcleos, 31 GiB de RAM | El equipo de despliegue tiene 4 núcleos y 8 GiB (D-36); cada contenedor tiene su límite de memoria. |
| JDK | OpenJDK 25.0.4.1 (Red Hat) | Gradle 9.8.1 con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`. |
| Docker Engine | 29.8.2 (`moby-engine` de Fedora) | Instalado por el autor el 2026-10-12. El usuario está en el grupo `docker`; hasta volver a iniciar sesión, los comandos se ejecutan con `sg docker -c '…'`. |
| Docker Compose | 5.5.1 (`docker-compose` de Fedora) | Se pide 2.24 o posterior (`env_file` con `required: false`). |
| Buildx | 0.37.2 (`docker-buildx` de Fedora) | BuildKit para `RUN --mount=type=cache`. |

## Construcción y pruebas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Plantillas (T016) | confirmada | Copias temporales `servicios/prueba` y `apps/procesoprueba`, creadas con los renombres del README: `spotlessApply build integrationTest` pasa. `PruebaFlywayConfigTest` 2, `ArquitecturaTest` 2, `PruebaEsquemaIntegrationTest` 1 (PostgreSQL 18.6 con Testcontainers sobre Docker 29.8.2), `ModulosTest` 1 y `EsquemasIntegrationTest` 1. Cobertura del servicio: 6 de 6 líneas; el proceso no tiene clases medibles (la de arranque está excluida). `EsquemasIntegrationTest` arranca aunque `DB_CLAVE_PROCESOPRUEBA` no tenga valor, porque `@ServiceConnection` tiene prioridad (hallazgo U4 del análisis). **Hallazgos:** (1) la suite `integrationTest` no veía las dependencias `implementation` del módulo; `mdd.java-conventions` ahora la hace extender `implementation` y `runtimeOnly`, como la suite `test`. (2) La prueba del proceso no veía `Flyway`, que el servicio tiene como dependencia interna; `mdd.aplicacion` agrega `flyway-core` a su suite `integrationTest`. (3) Dos nombres de prueba de la plantilla pasaban de 100 columnas; se acortaron. Copias borradas y `settings.gradle` restaurado. | 2026-10-12 |
| Servicios, procesos y gateway (T017 a T020) | confirmada | 12 servicios creados desde la plantilla (`servicios/usuarios` … `servicios/dashboard`), 4 procesos (`apps/identidad`, `apps/datos`, `apps/distributivo` y `apps/salida`) con sus servicios según ADR-006, y `gateway` con Spring Cloud 2025.1.3. `./gradlew spotlessApply build integrationTest` en 3 min 11 s, sin fallos ni pruebas omitidas: 12 `<Servicio>FlywayConfigTest`, 12 `ArquitecturaTest`, 12 `<Servicio>EsquemaIntegrationTest`, 4 `ModulosTest`, 4 `EsquemasIntegrationTest`, `GatewayAplicacionTest` y las 2 suites de `plataforma:arquitectura`. Cobertura de líneas: cada servicio 6 de 6; `plataforma:arquitectura` 34 de 34; los procesos y el gateway no tienen clases medibles (solo la de arranque, excluida). SpotBugs y Spotless pasan. `GatewayAplicacionTest` confirma que Spring Cloud 2025.1.3 carga con Spring Boot 4.1.1. | 2026-10-12 |

## Escenario 1: todo se levanta con un comando

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Construcción de las imágenes (T025) | confirmada | `docker compose build` en la copia de trabajo: 89 s para las 5 imágenes, con la caché de Gradle a medio llenar por un primer intento. Tamaños: `gateway` 583 MB; `identidad`, `datos`, `distributivo` y `salida` 533 MB cada una; `postgres:18.6-alpine` 434 MB. **Hallazgo:** el primer intento falló tras 114 s con `Timeout waiting to lock journal cache (/root/.gradle/caches/journal-1)`. Los 5 Gradle compartían la caché, pero cada uno corre en su contenedor y no pueden coordinar el bloqueo. Con `--mount=type=cache,…,sharing=locked`, BuildKit construye de a una imagen y las siguientes reutilizan las dependencias. Una construcción en frío, sin caché, se mide en el runner (T032). | 2026-10-12 |
| Clon limpio (T026) | confirmada | `git clone --branch 002-compose-cuatro-procesos` del repositorio local en un directorio temporal, sin crear `.env`. `docker compose up -d --wait` terminó con código 0 en 20 s (las capas de BuildKit ya estaban en caché por T025). Los 6 contenedores quedaron `healthy`. `curl localhost:8080/actuator/health/readiness` → `{"status":"UP"}`. `docker compose ps` muestra `postgres: 5432/tcp` (solo interno) y únicamente `gateway: 0.0.0.0:8080->8080/tcp`; `docker compose port postgres 5432` no devuelve un puerto publicado. Las 12 tablas `flyway_schema_history` quedaron cada una en su esquema. **SC-001:** con las imágenes construidas, `docker compose down && docker compose up -d --wait` tardó 17,5 s, muy por debajo de 180 s; esta máquina es más rápida que el equipo de despliegue y la referencia es el runner (T032). Caso límite «un contenedor supera su límite de memoria»: no se fuerza; quedaría visible en `docker compose ps` como detenido o no sano, porque no hay política de reinicio. | 2026-10-12 |
| Falta el archivo de variables (T027) | confirmada | Con `.env.ejemplo` renombrado, `docker compose up -d --wait` termina con código 1 y el mensaje `env file …/mdd-limpio/.env.ejemplo not found: stat …: no such file or directory`; no se crea ningún contenedor. Se restauró el archivo. | 2026-10-12 |

## Escenario 2: cabe en la memoria del equipo (R-19)

Formato de cada medición: [contracts/medicion-r19.md](contracts/medicion-r19.md).

## Escenario 3: un esquema por servicio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
