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

### Medición local (T031)

**Medición de R-19** · 2026-10-12T06:16:56-05:00 · commit `70f3988` · entorno: local (Docker 29.8.2)
Calentamiento: 20 llamadas a la salud por componente y 30 s de espera.

| Contenedor | Consumo (MiB) | Límite (MiB) |
|---|---:|---:|
| postgres | 93,8 | 512 |
| identidad | 182,2 | 512 |
| datos | 184,9 | 512 |
| distributivo | 187,2 | 512 |
| salida | 185,2 | 512 |
| gateway | 168,3 | 384 |
| **Total** | **1 001,6** (0,98 GiB) | 2 944 |

Resultado: cumple (< 8 GiB). Sin alerta de R-19 (≤ 7 GiB).

`deploy/medir_memoria.sh` terminó con código 0 en unos 60 s. Una medición anterior, con el commit `e13622c`, dio 989,6 MiB: la variación entre lecturas es de unos 10 MiB. En esta máquina cada JVM ve 12 núcleos; en el equipo de despliegue verá 4. Con más núcleos la JVM crea más hilos, así que la medición local tiende a dar un poco más que la del equipo de despliegue: el margen queda a favor. La medición no incluye JPA, Spring Security ni la generación de PDF, que llegan con las historias siguientes; B00-H03 vuelve a medir después de iniciar sesión (D-34).

### Medición de referencia en GitHub Actions (T032)

Run [38245724258](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38245724258) de `perfil-consolidado`, al marcar el PR #83 como listo para revisión (cabeza `095fe9e`; el commit medido, `a34cd4d`, es el *merge* del PR con `main` que hace GitHub en los eventos `pull_request`). Resultado: éxito.

**Medición de R-19** · 2026-10-12T11:58:47+00:00 · commit `a34cd4d` · entorno: ubuntu-latest (Docker 28.0.4)
Calentamiento: 20 llamadas a la salud por componente y 30 s de espera.

| Contenedor | Consumo (MiB) | Límite (MiB) |
|---|---:|---:|
| postgres | 110,1 | 512 |
| identidad | 162,2 | 512 |
| datos | 160,3 | 512 |
| distributivo | 153,1 | 512 |
| salida | 153,8 | 512 |
| gateway | 169,6 | 384 |
| **Total** | **909,1** (0,89 GiB) | 2 944 |

Resultado: cumple (< 8 GiB). Sin alerta de R-19 (≤ 7 GiB).

- **Escenario 1 en un clon limpio con Docker:** el *checkout* del runner, sin caché, construyó las 5 imágenes en 2 min 24 s (`time docker compose build`) y `docker compose up -d --wait` dejó los 6 contenedores sanos en **33,2 s**. Es la referencia de SC-001 (180 s como máximo), en un equipo más parecido al de despliegue que el local.
- La medición del runner (909,1 MiB) es algo menor que la local (1 001,6 MiB), como se esperaba: en local cada JVM ve 12 núcleos.
- El commit `af0b495` (Tomcat 11.0.27 y Jackson 3.1.7) no cambia `compose.yaml` ni `deploy/**`, así que el filtro `paths` no vuelve a ejecutar el workflow. La medición se repite al cerrar la ola y en B00-H03 (D-34).

## Escenario 3: un esquema por servicio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Migración en el esquema de otro servicio (T033) | confirmada | Con `V2__intruso.sql` temporal en `servicios/normativa`: (1) `create table docentes.intruso(id int);` hace fallar `NormativaEsquemaIntegrationTest` en la migración, con `ERROR: schema "docentes" does not exist`, porque la base de la prueba solo tiene el esquema de `normativa`; (2) `create table public.intruso(id int);`, que sí se puede crear, hace fallar la comprobación: `Expecting all elements of: ["normativa.flyway_schema_history", "public.intruso"] to match given predicate but this element did not: "public.intruso"`. En ambos casos `:servicios:normativa:integrationTest` termina en `FAILED`; al borrar el archivo vuelve a pasar. | 2026-10-12 |
| Consulta de esquemas en el perfil (T034) | confirmada | `docker compose exec postgres psql -U postgres -d mdd -c "select table_schema, table_name from information_schema.tables where table_schema not in ('pg_catalog','information_schema') order by 1,2;"` → 12 filas, una `flyway_schema_history` en cada esquema (`academico`, `cargas`, `dashboard`, `docentes`, `firma`, `generacion`, `normativa`, `notificaciones`, `reporting`, `roles`, `usuarios` y `validacionocs`), ninguna en `public`. | 2026-10-12 |
| Permisos entre procesos (T035) | confirmada | Con `psql -h 127.0.0.1` y las claves de `.env.ejemplo`: como `identidad`, `create table normativa.intruso(id int);` → `ERROR: permission denied for schema normativa`, y `select count(*) from normativa.flyway_schema_history;` → `ERROR: permission denied for schema normativa`. Como `salida`, `create table firma.intruso(id int);` → `ERROR: permission denied for schema firma`, y `create table public.intruso(id int);` → `ERROR: permission denied for schema public`. Como `identidad`, leer su propio esquema (`usuarios.flyway_schema_history`) funciona. | 2026-10-12 |
| Segunda ejecución (T036, SC-004) | confirmada | `docker compose down && docker compose up -d --wait` sobre el mismo volumen: los 6 contenedores `healthy`; en los registros, Flyway dice `Schema "…" is up to date. No migration necessary.` para los 12 esquemas (identidad 3, datos 4, distributivo 3, salida 2) y ninguna línea `Successfully applied`. Siguen las mismas 12 tablas. La primera vez, `up --wait` tardó 714 s porque el equipo se suspendió en medio (journal: `The system will suspend now!` a las 06:18:09 y reanudación a las 06:29:45); al repetirlo tardó 17 s (`down` 3 s). | 2026-10-12 |

## Notas

- **JavaDoc (T037):** todas las clases nuevas tienen JavaDoc en español y ninguna usa `@author`. `./gradlew javadoc` termina sin errores; solo hay avisos de *doclint* por el constructor por defecto, `main` y el método del bean, igual que en las clases de B00-H01. `spotlessCheck` pasa y `git status` no muestra archivos temporales.
- **Gitleaks en local:** `gitleaks:v8.30.1 git` con `.gitleaks.toml` sobre un clon de la rama: 18 commits, `no leaks found`. `.env.ejemplo` no necesita excepción.
- **SELinux en la máquina del autor:** el script de esquemas se monta con `:ro,z` para que el contenedor lo pueda leer. En Ubuntu, sin SELinux, la opción no tiene efecto.
- **Lint:** actionlint 1.7.12 sobre todos los workflows y shellcheck sobre `deploy/medir_memoria.sh` y `deploy/postgres/01-esquemas.sh`, sin hallazgos. Las acciones de `perfil-consolidado.yml` reutilizan los SHA ya fijados con pinact en `ci.yml` (checkout v7.0.1 y upload-artifact v7.0.2).
- **Grype en el PR #83 (primer run listo para revisión, `095fe9e`):** `4 · sca` falló porque el BOM de Spring Boot 4.1.1, que sigue siendo la última versión, trae Tomcat 11.0.24 (GHSA-9xv2-5v5q-p794, GHSA-gcx9-497g-6cp6 y GHSA-h3x4-894j-xpx5, críticas; corregidas en 11.0.25) y Jackson 3.1.5 (GHSA-q4xh-88c3-wmh7, GHSA-cxp5-3px4-pw24, GHSA-wv8q-qhhj-9h54, GHSA-p6pp-m3f8-5c89 y GHSA-7hhh-6rmp-j9qf, altas; corregidas en 3.1.6 y 3.1.7). Aparecen ahora porque B00-H02 es la primera historia con servidor web. `mdd.java-conventions` fija Tomcat 11.0.27 (los tres módulos *embed*) y el BOM de Jackson 3.1.7 por encima del de Spring Boot, con las versiones y su motivo en `gradle.properties`. Grype 0.110.0 en local sobre los SBOM de los 4 procesos y del gateway: `No vulnerabilities found`. En el mismo run, `2 · test-integracion` falló por un 504 al descargar Gradle de `services.gradle.org`, que es transitorio.
