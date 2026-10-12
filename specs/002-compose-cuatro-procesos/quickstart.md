# Quickstart: validar B00-H02

Guía para comprobar los tres escenarios. Las garantías están en [contracts/](contracts/) y las decisiones en [research.md](research.md). Los resultados van a `evidencia.md`.

## Requisitos previos

- JDK 25 y el Wrapper de Gradle, como en B00-H01. En local, Gradle corre con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`.
- **Motor de contenedores con Docker Compose v2.24 o posterior:**
  - En CI y en el VPS, Docker.
  - En la máquina del autor, Podman: `podman compose` falla porque no hay proveedor de Compose. Hay que instalar el binario de Docker Compose v2 de su release en `~/.docker/cli-plugins/docker-compose` y activar el socket:

    ```bash
    systemctl --user enable --now podman.socket
    export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
    export TESTCONTAINERS_RYUK_DISABLED=true     # para integrationTest con Testcontainers
    ```

## 1. Construcción y pruebas

```bash
cd backend
./gradlew build integrationTest
```

**Resultado esperado:**
- todo en verde;
- cada servicio con su `<Servicio>FlywayConfigTest`;
- cada proceso con `ModulosTest` y `EsquemasIntegrationTest` (escenario 3, automático; [contracts/persistencia.md](contracts/persistencia.md));
- cobertura del 60 % o más en cada módulo.

## 2. Todo se levanta con un comando (escenario 1)

Desde un clon limpio, sin crear `.env`:

```bash
git clone git@github.com:paul-g-software-ucacue/tesis-prototipo-2026.git /tmp/mdd-limpio && cd /tmp/mdd-limpio
time docker compose up -d --wait
docker compose ps --format '{{.Name}} {{.Health}}'
```

**Resultado esperado:** los 6 contenedores (`postgres`, `identidad`, `datos`, `distributivo`, `salida` y `gateway`) están `healthy`, y `docker compose up --wait` termina con código 0. Con las imágenes ya construidas, se puede medir el tiempo hasta que todo está sano con `docker compose down && time docker compose up -d --wait`; tiene que ser de 180 s o menos (SC-001).

Además:
- `curl -s localhost:8080/actuator/health/readiness` responde `UP`, porque el gateway es el único puerto publicado.
- `docker compose port postgres 5432` no devuelve nada, porque PostgreSQL no está publicado.

## 3. Cabe en la memoria de la VM (escenario 2)

**Referencia:** GitHub → Actions → *perfil-consolidado* → *Run workflow* sobre la rama. La tabla aparece en el resumen del run y en el artefacto `medicion-r19` ([contracts/medicion-r19.md](contracts/medicion-r19.md)).

**En local:**

```bash
deploy/medir_memoria.sh | tee /tmp/medicion.md; echo "salida=$?"
```

**Resultado esperado:** el total es menor que 4 GiB y el código de salida es 0. Copia la tabla a `evidencia.md`. Si pasa de 3,5 GiB, anota la alerta de R-19. Si llega a 4 GiB o más, ajusta los límites o las opciones de la JVM (research §8) y vuelve a medir. Si aun así no cabe, registra una decisión nueva (alternativa: Nginx en lugar de Spring Cloud Gateway).

## 4. Un esquema por servicio (escenario 3, sobre el perfil levantado)

```bash
docker compose exec postgres psql -U postgres -d mdd -c \
  "select table_schema, table_name from information_schema.tables where table_schema not in ('pg_catalog','information_schema') order by 1,2;"
```

**Resultado esperado:** 12 filas, `<servicio> | flyway_schema_history`, una por cada esquema de servicio, y ninguna en `public`.

**Permisos entre procesos:** un rol no puede crear tablas en el esquema de otro proceso. La clave de ejemplo está en `.env.ejemplo`.

```bash
docker compose exec -e PGPASSWORD=<DB_CLAVE_IDENTIDAD> postgres psql -h 127.0.0.1 -U identidad -d mdd \
  -c "create table normativa.intruso(id int);"
```

**Resultado esperado:** `ERROR: permission denied for schema normativa`.

## 5. Segunda ejecución (SC-004)

```bash
docker compose down && docker compose up -d --wait
docker compose logs identidad | grep -i "Schema .* is up to date"
```

**Resultado esperado:** todo vuelve a quedar sano, y Flyway informa que cada esquema ya está al día, sin migraciones nuevas aplicadas.

## 6. Cierre

- `evidencia.md` tiene el run del workflow (escenarios 1 y 2), la tabla de memoria, la consulta de esquemas y la prueba de permisos.
- El pull request de B00-H02 tiene la CI en verde, incluidos `2 · test-integracion` (con `EsquemasIntegrationTest`) y `3 · secretos` (con `.env.ejemplo`).
