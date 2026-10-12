# Contrato: perfil consolidado en `compose.yaml`

Es lo que garantiza `docker compose up` desde un clon limpio (escenario 1; FR-001 a FR-006 y FR-012). Las decisiones están en `research.md` §1, §2, §5, §6 y §8, y ADR-013 las registra.

## Uso

```bash
docker compose up -d --wait     # construye las imágenes la primera vez y espera a que todo esté sano
docker compose ps               # los 6 contenedores en estado healthy
docker compose down             # detiene; los datos quedan en el volumen
docker compose down -v          # detiene y borra los datos (el script de esquemas vuelve a correr)
```

No hace falta compilar antes ni copiar ningún archivo: `.env.ejemplo` ya trae valores que funcionan en local. Para usar otros valores, se crea un `.env`, que Git ignora, con las variables que se quieran cambiar.

## Contenedores

| Servicio de Compose | Construcción o imagen | `depends_on` | Puertos publicados | `deploy.resources.limits.memory` | *healthcheck* |
|---|---|---|---|---|---|
| `postgres` | `postgres:18.6-alpine` | — | — | `512m` | `pg_isready -U postgres -d mdd` |
| `identidad` | `deploy/docker/java.Dockerfile`, contexto `backend`, `MODULO=apps:identidad`, `NOMBRE=identidad` | `postgres: service_healthy` | — | `512m` | readiness por `/dev/tcp` |
| `datos` | ídem, `apps:datos` | `postgres: service_healthy` | — | `512m` | ídem |
| `distributivo` | ídem, `apps:distributivo` | `postgres: service_healthy` | — | `512m` | ídem |
| `salida` | ídem, `apps:salida` | `postgres: service_healthy` | — | `512m` | ídem |
| `gateway` | ídem, `MODULO=gateway`, `NOMBRE=gateway` | — | `8080:8080` | `384m` | ídem |

- **Comando de salud de los componentes Java**, que se ejecuta dentro del contenedor:

  ```bash
  bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health/readiness HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"status\":\"UP\"" <&3'
  ```

- **Tiempos del *healthcheck*:** `interval: 10s`, `timeout: 5s`, `retries: 12` y `start_period: 60s`. Así un proceso tiene hasta unos 180 s para quedar sano (SC-001).

## Variables (`.env.ejemplo`)

| Variable | Usada por | Valor de ejemplo |
|---|---|---|
| `POSTGRES_PASSWORD` | `postgres` (superusuario) | ficticio |
| `DB_CLAVE_IDENTIDAD`, `DB_CLAVE_DATOS`, `DB_CLAVE_DISTRIBUTIVO`, `DB_CLAVE_SALIDA` | `postgres` (script de esquemas) y cada proceso | ficticios, uno por rol |
| `SPRING_DATASOURCE_URL` | procesos | `jdbc:postgresql://postgres:5432/mdd` |
| `JAVA_TOOL_OPTIONS` | procesos y gateway | `-XX:MaxRAMPercentage=60` |

- Cada proceso recibe `SPRING_DATASOURCE_USERNAME` (el nombre de su rol) y `SPRING_DATASOURCE_PASSWORD` (su `DB_CLAVE_*`), que se definen en su bloque `environment` de `compose.yaml`, sin interpolación.
- `.env.ejemplo` no contiene secretos reales; la etapa `3 · secretos` de la CI pasa (SC-005).

## Garantías

1. Un `docker compose up` desde un clon limpio deja los 6 contenedores `healthy`, sin pasos previos (escenario 1).
2. Ningún proceso queda sano sin conexión a PostgreSQL. Los procesos no arrancan antes de que PostgreSQL esté sano (FR-003, FR-004).
3. Solo el gateway es accesible desde la máquina anfitriona.
4. Cada contenedor tiene su límite de memoria y cada JVM se dimensiona con él (FR-006).
5. Volver a levantar el perfil sobre el mismo volumen no repite migraciones y vuelve a quedar sano (SC-004).
6. Si falta `.env.ejemplo`, Compose se niega a arrancar y dice qué archivo falta.
