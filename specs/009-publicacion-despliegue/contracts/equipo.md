# Contrato: equipo de demostración

## `deploy/equipo/compose.vps.yml`

Se corre siempre como `docker compose -p mdd --env-file /opt/mdd/.env -f /opt/mdd/actual/compose.vps.yml …`.

| Servicio | Imagen | Puertos | Memoria | Variables (solo las suyas) |
|---|---|---|---|---|
| `postgres` | `postgres:18.6-alpine@sha256:…` | ninguno | 512m | `POSTGRES_PASSWORD`, `POSTGRES_DB`, las 4 `DB_CLAVE_*` (para `01-esquemas.sh`) |
| `identidad` | `${REPOSITORIO}:identidad-${ETIQUETA}` | ninguno | 512m | `DB_CLAVE_IDENTIDAD`, `MDD_CLAVE_PROCESO_DISTRIBUTIVO`, `MDD_USUARIO_PRUEBA_HABILITADO=true`, `MDD_USUARIO_PRUEBA_CLAVE`, `MDD_LLAVE_PRIVADA=/run/mdd/jwt.pem` |
| `datos`, `distributivo`, `salida` | `${REPOSITORIO}:<proceso>-${ETIQUETA}` | ninguno | 512m | Su `DB_CLAVE_*`; `distributivo`, también `MDD_CLAVE_PROCESO_DISTRIBUTIVO` |
| `gateway` | `${REPOSITORIO}:gateway-${ETIQUETA}` | ninguno | 384m | — |
| `frontend` | `${REPOSITORIO}:frontend-${ETIQUETA}` | ninguno | 64m | — |
| `caddy` | `caddy:2.11.7-alpine@sha256:…` | `127.0.0.1:443:443` | 64m | `DIRECCION`, `DEMO_USUARIO`, `DEMO_CLAVE_HASH` |
| `cloudflared` (perfil `tunel`) | `cloudflare/cloudflared:<versión>@sha256:…` | ninguno | 64m | `TUNEL_TOKEN` |

Los *healthchecks*, `JAVA_TOOL_OPTIONS`, `SPRING_DATASOURCE_URL` y el volumen `datos-postgres` son los de `compose.yaml`. El total de los límites (sin el túnel) es de 3 072 MiB, por debajo de la alerta de 7 GiB de R-19 (FR-011). Las variables se pasan explícitamente a cada servicio, no con `env_file`: un proceso no recibe las claves de otro.

## `deploy/Caddyfile` (cambia)

| Sitio | Para qué |
|---|---|
| `{$DIRECCION}` (por defecto `localhost`) | HTTPS: CA interna con `localhost`, Let's Encrypt con un dominio propio |
| `http://:8081` | Entrada del túnel, solo dentro de la red de Compose |

Los dos importan el mismo fragmento: `basic_auth` para todo menos `/api/*`, `/api/*` → `gateway:8080`, lo demás → `frontend:8080`, y las cabeceras de seguridad de hoy.

## `deploy/desplegar.sh` (cambia)

**Uso:** `desplegar.sh <carpeta de la versión nueva>`, desde `/opt/mdd`. Termina con 0 (sano), 1 (vuelta atrás) o 2 (la versión anterior tampoco quedó sana). Escribe una línea de resumen en la salida estándar para el informe.

| Paso | Detalle |
|---|---|
| Cambio de versión | `anterior` → la de `actual`; `actual` → la nueva; `ETIQUETA` en `.env` ← su `VERSION` (12 caracteres) |
| Arranque | `pull` y `up -d --remove-orphans` |
| Salud | Hasta `ESPERA_SALUD` (180 s) a que ningún contenedor con *healthcheck* esté `unhealthy` ni `starting` |
| Humo | `actual/probar_humo.sh` |
| Vuelta atrás | Si falla la salud o el humo: `actual` → `anterior`, `ETIQUETA` anterior, `up -d --remove-orphans` y salud otra vez |

## `deploy/equipo/probar_humo.sh` (nuevo)

Usa `DIRECCION`, `DEMO_USUARIO`, `DEMO_CLAVE` y `MDD_USUARIO_PRUEBA_CLAVE` del `.env`. Con `curl --resolve` a `127.0.0.1:443`, acepta la CA interna de Caddy solo con `DIRECCION=localhost`. No imprime credenciales ni el token.

| Paso | Petición | Comprueba |
|---|---|---|
| 1 | `GET /` sin credenciales | 401 con `WWW-Authenticate: Basic` (escenario 5) |
| 2 | `GET /` con las credenciales de la demo | 200, HTML con `lang="es"` |
| 3 | `POST /api/usuarios/sesion` con `autor.prueba` | 200 con `token` |
| 4 | `GET /api/usuarios/sesion` con el token | 200 con `autor.prueba` |

## `deploy/equipo/actualizar.sh` (nuevo, instalado en `/opt/mdd/bin/`)

Lo corre `mdd-actualizar.service` cada 5 minutos, como el usuario `mdd`, con `flock` para no solaparse.

| Paso | Detalle | Si falla |
|---|---|---|
| Consulta | Token de lectura de `auth.docker.io` y `HEAD` de `${REPOSITORIO}:despliegue-main` → `Docker-Content-Digest` | Registro local y salida con 0; reintenta en la próxima |
| ¿Nuevo? | Compara con `estado/ultimo-digest` | Igual: sale con 0 |
| Paquete | `docker pull`, `docker create`, `docker cp /paquete` → `versiones/<commit>/` | Informe `failure` |
| Informe | *Deployment* `in_progress` (D-56) | GitHub no responde: sigue, solo registro local |
| Despliegue | `versiones/<commit>/desplegar.sh versiones/<commit>` | — |
| Resultado | `success` (código 0) o `failure` (1 o 2) con la línea de resumen; guarda el *digest*; borra versiones más viejas que las 3 últimas | — |

Variables para la prueba de la CI: `REGISTRO` (por defecto `registry-1.docker.io`), `AUTENTICACION` (por defecto `auth.docker.io`) y `GITHUB_API` (por defecto `https://api.github.com`).

## Unidades de systemd

| Unidad | Clave |
|---|---|
| `mdd-actualizar.service` | `Type=oneshot`, `User=mdd`, `WorkingDirectory=/opt/mdd`, `ExecStart=/usr/bin/flock -n /opt/mdd/estado/candado /opt/mdd/bin/actualizar.sh` |
| `mdd-actualizar.timer` | `OnBootSec=2min`, `OnUnitActiveSec=5min`, `Persistent=true` |

## `deploy/equipo/preparar.sh` (nuevo)

**Uso:** `sudo deploy/equipo/preparar.sh`, una vez por equipo. Con `--no-interactivo` y las variables de entrada, para la prueba de la CI. Hace lo de research §11 sin mostrar secretos, y no pisa lo que ya existe.
