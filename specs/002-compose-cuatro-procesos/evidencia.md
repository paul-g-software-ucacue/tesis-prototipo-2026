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

## Escenario 1: todo se levanta con un comando

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 2: cabe en la memoria del equipo (R-19)

Formato de cada medición: [contracts/medicion-r19.md](contracts/medicion-r19.md).

## Escenario 3: un esquema por servicio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
