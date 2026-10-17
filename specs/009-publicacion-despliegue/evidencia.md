# Evidencia: B00-H08

Registro de las comprobaciones de los cinco escenarios de la historia (issue #25), del cierre de ola sin URL pública (US6) y de los informes a GitHub Deployments (D-56). Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de GitHub Actions. Todo con valores ficticios, salvo la comprobación final en el PC del autor, cuyos secretos nunca salen del equipo.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | Las pruebas usan carpetas del *scratchpad*; `/opt/mdd` y systemd no se tocan hasta T032. |
| Docker Engine y buildx | 29.8.2 y 0.38.0 (Fedora) | Con `sg docker -c '…'`. |
| PostgreSQL | `postgres:18.6-alpine@sha256:77f585114c32fbca283dc835b0596f4e52b51b4c6662d7810b2f4084f60a1873` | La misma versión del perfil consolidado. |
| Caddy | `caddy:2.11.7-alpine@sha256:d8542f48d34a9cf4e4c11a478865229840e87e4c96ea3f439101f31a5d35f75f` | 2.11.7 salió el 2026-10-03 (14 días). |
| cloudflared | `cloudflare/cloudflared:2026.9.3@sha256:072c067d25ccbe61d46e18f0d0723255f2bb5304f7317caa95b27031520ff92c` | 2026-09-24; las 2026.10.x tienen menos de dos semanas. Solo en el perfil `tunel`. |
| Registro de pruebas | `registry:2.8.3@sha256:a3d8aaa63ed8681a604f1dea0aa03f100d5895b6a58ace528858a7b332415373` | Solo en `probar_equipo.sh`. |
| curl en contenedor | `curlimages/curl:8.22.0@sha256:58adaa4e8dca9c988bae2aba4ab3434a0bb2da16bbe3f92dec39ec7785166777` | 2026-09-02; la prueba del sitio del túnel. |
| busybox | `busybox:1.37@sha256:bdf57e528e45e4433820e045b29b4597825a1c9e38353532d90a01445013f82e` | Etapa previa del paquete (escribe `VERSION`). |
| `docker/setup-buildx-action` | v4.4.1, `f87e5991a6d7451dcb8d9637bfbc97413f497069` | 2026-09-16. |
| `docker/login-action` | v4.6.0, `dbcb813823bdd20940b903addbd779551569679f` | 2026-07-29. |
| Grype | 0.120.1 | La de B00-H07. |

## Escenario 1: publicación

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Construcción (T006, T007) | confirmada | `deploy/publicacion/construir.sh d8bc8828…`: `buildx bake` de los 6 servicios de `compose.yaml` (1 min 23 s con caché) y el paquete; el *label* `org.opencontainers.image.revision` tiene el SHA completo. El paquete extraído con `docker create`/`docker cp` trae `compose.demo.yml`, `Caddyfile`, `01-esquemas.sh`, `desplegar.sh`, `probar_humo.sh` y `VERSION`. Hubo que permitir esos 5 archivos en `.dockerignore`. | 2026-10-17 |
| `compose.demo.yml` (T003, T007) | confirmada | `docker compose config` con un `.env` ficticio: imágenes `localhost:5000/mdd:<componente>-d8bc8828a281`, PostgreSQL y Caddy por *digest*, `datos` recibe solo `DB_CLAVE_DATOS`, `JAVA_TOOL_OPTIONS` y `SPRING_DATASOURCE_URL`; límites que suman 3 072 MiB; Caddy publica solo `127.0.0.1:443`; `cloudflared` no aparece sin su perfil. | 2026-10-17 |
| `Caddyfile` (T004) | confirmada | `caddy validate` con Caddy 2.11.7: `Valid configuration` (sitio `{$DIRECCION:mdd.localhost}` con `tls internal` y `http://:8081`, los dos con el fragmento `(mdd)`). | 2026-10-17 |
| Publicación en un registro local (T008, T009) | confirmada | `registry:2.8.3` en `localhost:5000` y `publicar.sh localhost:5000/mdd d8bc8828…`: las 7 etiquetas `<componente>-d8bc8828a281` y `despliegue-main` con el mismo *digest* que `despliegue-d8bc8828a281` (`sha256:1bcba636…`). La prueba se escribió con `publicar.sh` ya existente, así que no se vio fallar antes. | 2026-10-17 |

## Escenario 2: imagen vulnerable

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Imagen vulnerable de prueba (T011) | confirmada | Una imagen sobre `alpine:3.18.0`: `resumen.sh` la pone en «Bloquean» con `libcrypto3`/`libssl3` 3.1.0-r4 → 3.1.7-r0 (CVE-2024-6119, alta), CVE-2024-5535 y CVE-2022-48174 (críticas), y termina con 1. | 2026-10-17 |
| Primera revisión de las imágenes reales (T013) | **las 9 bloqueadas** | `revisar.sh` sobre las 6 imágenes y las 3 de terceros. Java: `stdlib go1.27.1` de `/usr/bin/pebble` (supervisor de Canonical de la base `eclipse-temurin:25-jre`, que el proceso no usa), 1 crítica y 8 altas, corregidas en Go 1.27.2. `frontend` y `postgres`: `zlib 1.3.2-r0` (CVE-2026-85091), corregida en 1.3.2-r1. Caddy 2.11.7 y cloudflared 2026.9.3: Go 1.26.8 y `x/net`, críticas y altas. Las bases actuales del registro (Temurin, nginx, PostgreSQL y Caddy) seguían igual el 2026-10-17. | 2026-10-17 |
| Arreglo de las imágenes propias (T013) | confirmada | `java.Dockerfile` borra `/usr/bin/pebble`; `frontend.Dockerfile` hace `apk upgrade --no-cache zlib` como root y vuelve al usuario 101 (`id`: `uid=101(nginx)`). Las dos imágenes reconstruidas: 0 hallazgos que bloquean. | 2026-10-17 |
| Excepciones de las imágenes de terceros (T013, D-54) | confirmada, **aprobadas por el autor** | 35 excepciones en `.grype.yaml` (`desde: 2026-10-17`, `vence: 2026-11-16`), generadas desde los informes de Grype: `zlib` 1.3.2-r0 (PostgreSQL y Caddy); Go y `x/net` acotados por versión y por la ruta del binario (`/usr/bin/caddy`, `/usr/local/bin/cloudflared`), para no tapar un `stdlib` vulnerable en otra imagen; `libssl3t64` de cloudflared. `excepciones.sh`: `35 de 35 excepciones vigentes`. Condición del autor: el túnel no se activa mientras duren. | 2026-10-17 |
| Revisión después del arreglo (T013) | confirmada | `revisar.sh`: `Grype: imágenes (9)`, «Ninguna vulnerabilidad alta o crítica con versión corregida», 36 filas en «Suprimidas» y las altas sin arreglo a la vista; código 0. | 2026-10-17 |

## Escenario 3: despliegue sano

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 4: vuelta atrás

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 5: acceso restringido

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre de ola (US6)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Informes a GitHub (D-56)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Memoria (R-19)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
