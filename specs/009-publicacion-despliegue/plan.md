# Implementation Plan: Publicación en Docker Hub y despliegue en el equipo de demostración

**Branch**: `009-publicacion-despliegue` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-publicacion-despliegue/spec.md` (issue #25, B00-H08)

## Summary

**Publicación (`cd.yml`).**
- En cada fusión a `main`, `docker buildx bake -f compose.yaml` construye las 6 imágenes con la misma definición del perfil, más el **paquete de despliegue** (D-58, propuesta): una imagen `FROM scratch` con `compose.vps.yml`, el `Caddyfile` y los scripts de ese commit.
- Grype revisa las 6 imágenes y las de terceros (PostgreSQL, Caddy y cloudflared, por *digest*) con las excepciones de D-54, **antes** de iniciar sesión en Docker Hub. Si una falla, no se sube nada.
- Después sube las 7 etiquetas `<componente>-<commit>` y, al final, mueve `despliegue-main`.
- Con `workflow_dispatch` en otra rama solo construye y revisa.

**Despliegue en el equipo (D-55).**
- Un temporizador de systemd en el PC corre `actualizar.sh` cada 5 minutos como el usuario `mdd`.
- Hace `HEAD` de `despliegue-main`, que no gasta el cupo de Docker Hub. Si el *digest* cambió, baja el paquete, lo extrae en `versiones/<commit>/` y corre su `desplegar.sh`.
- `desplegar.sh` baja las imágenes, espera 180 s a que todo quede sano, corre la prueba de humo por el proxy y vuelve a la versión anterior (imágenes **y** archivos) si algo falla.
- Cada intento se informa a GitHub *Deployments* con un token de grano fino (D-56).

**Secretos y acceso.**
- `preparar.sh` crea `/opt/mdd` y genera todos los secretos en el equipo, sin mostrarlos: la llave JWT persistente, las claves de la base, la del proceso, la de `autor.prueba` (D-57) y el hash del acceso de la demo.
- Caddy sirve HTTPS en `localhost` con su CA interna y pide usuario y contraseña. El túnel de Cloudflare es un perfil opcional de Compose.

**Pruebas y cierre de ola.**
- `perfil-consolidado` simula el ciclo completo en el runner: un registro local y una API de GitHub simulada, con un despliegue sano, uno roto con vuelta atrás y los dos informes.
- `cierre-ola` deja de usar una URL pública: levanta el perfil en el runner y corre ZAP sobre la SPA y los contratos que existen. k6 se omite si no hay script, y el SBOM es el de B00-H07.

Los detalles, lo comprobado y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Bash · YAML (GitHub Actions, Compose) · Caddyfile · unidades de systemd · Dockerfile

**Primary Dependencies**:
- Docker Engine 29 con Compose y buildx (`buildx bake`);
- Grype 0.120.1 y los scripts de `deploy/sca/` de B00-H07;
- Caddy 2.11.7, PostgreSQL 18.6 y cloudflared (opcional), por *digest*;
- `docker/setup-buildx-action` y `docker/login-action`, por SHA;
- `curl`, `jq`, `openssl`, `flock` y systemd en el equipo.

**Storage**: el volumen `datos-postgres` del proyecto `mdd` en el equipo; los archivos de `/opt/mdd` (data-model.md).

**Testing**:
- paso «Despliegue en un equipo simulado» de `perfil-consolidado` (`registry:2`, una API de GitHub simulada con `python3 -m http.server` o similar, escenarios 3 y 4, D-56);
- `probar_humo.sh` (escenario 5);
- un pull request de demostración con una imagen vulnerable (escenario 2);
- `cierre-ola` con `workflow_dispatch` (US6);
- `actionlint` y `shellcheck`;
- la comprobación final en el PC (quickstart §4).

**Target Platform**: GitHub Actions (`ubuntu-latest`) para publicar; el PC del autor (Fedora 44, SELinux) como equipo de demostración, con el mismo despliegue válido para Ubuntu Server (VPS o VM del IST).

**Project Type**: pipeline y despliegue del monorepo: `.github/workflows/`, `deploy/` y la imagen del paquete.

**Performance Goals**:
- publicación en menos de 30 minutos (SC-001);
- despliegue en menos de 15 minutos después de publicar (SC-003);
- vuelta atrás en menos de 5 minutos (SC-004);
- 0 descargas de Docker Hub en las consultas sin cambios.

**Constraints**:
- nada de GitHub entra al equipo ni guarda su dirección (D-55);
- los secretos solo en el equipo (FR-013);
- cada contenedor recibe solo sus variables;
- 3 072 MiB de límites en total (R-19, D-36);
- nada depende de la distribución (FR-010);
- nunca montar el `home` del autor con `:z` (memoria del proyecto): solo `/opt/mdd/secretos` con `:Z`;
- los nombres de los 10 checks no cambian (D-31).

**Scale/Scope**:
- 2 workflows nuevos o cambiados (`cd.yml` nuevo; `cierre-ola.yml` reescrito) y un paso nuevo en `perfil-consolidado.yml`;
- 1 Dockerfile nuevo, 1 Compose nuevo y el `Caddyfile` cambiado;
- 6 scripts nuevos o cambiados (`construir.sh`, `publicar.sh`, `preparar.sh`, `actualizar.sh`, `probar_humo.sh` y `desplegar.sh`) y 2 unidades de systemd;
- 7 etiquetas por commit.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I a IV, VI, VII | Sin dominio, servicios ni contratos nuevos. | ✅ N/A |
| V. Límites | El equipo despliega los 4 procesos y el API Gateway de ADR-006, con los límites de R-19. | ✅ |
| VIII. Pruebas | El ciclo de despliegue se prueba en cada PR que toca el despliegue (registro y API simulados): sano, roto con vuelta atrás e informes. Datos y secretos ficticios en la CI. | ✅ |
| IX. Seguridad (2.1.3) | *Entrega:* Grype sobre las 6 imágenes y las de terceros antes de publicar, con D-54. *Operación:* despliegue automático en el equipo de demostración, con acceso restringido (Caddy), *healthchecks* y vuelta atrás (D-55). Secretos solo en su entorno: en GitHub, solo el token de escritura de Docker Hub en `publicacion`; en el equipo, el resto, con 0600 y un usuario propio. Acciones por SHA e imágenes por *digest*. | ✅ |
| X. Simplicidad | Sin herramientas nuevas en el equipo más allá de Docker, `curl`, `jq` y systemd. El paquete de despliegue (D-58) evita clonar el repositorio o dar más permisos. El túnel es opcional. | ✅ |
| XI y XII | Los scripts llevan su comentario de propósito y de uso en español. | ✅ |
| XIII. Errores | N/A: no hay respuestas nuevas de la API. | ✅ N/A |
| §3 Infraestructura | Docker Compose en un equipo de 4 núcleos y 8 GiB por presupuesto (D-36); el pipeline no despliega en la VM del IST. | ✅ |
| §6.4 Decisiones | D-55, D-56 y D-57 aceptadas el 2026-10-17; **D-58 propuesta**, pendiente de aprobación del autor antes de implementar. | ⚠️ pendiente |

**Resultado del gate antes de la fase 0:** pasa, con D-58 pendiente de aprobación.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/009-publicacion-despliegue/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: versión publicada, equipo, despliegue y entornos
├── quickstart.md            # fase 1: reemplaza la guía del VPS
├── contracts/
│   ├── publicacion.md           # cd.yml, construir.sh, publicar.sh y el paquete
│   ├── equipo.md                # compose.vps.yml, Caddyfile, desplegar, humo, actualizar, systemd y preparar
│   └── cierre-ola.md            # ZAP y k6 sobre el perfil del runner
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
.github/workflows/
├── cd.yml                       # nuevo: construir, revisar y publicar
├── cierre-ola.yml               # cambia: sistema en el runner, contratos reales, k6 opcional, SBOM por módulo
└── perfil-consolidado.yml       # cambia: + paso «Despliegue en un equipo simulado»; rutas: deploy/**
deploy/
├── Caddyfile                    # cambia: sitio por DIRECCION y sitio interno para el túnel
├── desplegar.sh                 # cambia: versiones, enlaces, humo y vuelta atrás de archivos
├── docker/despliegue.Dockerfile # nuevo: el paquete (FROM scratch)
├── equipo/
│   ├── compose.vps.yml          # nuevo
│   ├── actualizar.sh            # nuevo
│   ├── probar_humo.sh           # nuevo
│   ├── preparar.sh              # nuevo
│   ├── probar_equipo.sh         # nuevo: la prueba del ciclo en el runner (research §12)
│   ├── github_simulado.py       # nuevo: API de GitHub simulada para esa prueba
│   ├── mdd-actualizar.service   # nuevo
│   └── mdd-actualizar.timer     # nuevo
└── publicacion/
    ├── construir.sh             # nuevo
    └── publicar.sh              # nuevo
docs/decisiones.md               # D-58 (propuesta)
specs/007-spa-base/contracts/perfil.md        # remite al Caddyfile nuevo
```

**Structure Decision**: lo que corre en el equipo vive en `deploy/equipo/`, y lo que corre en la CI para publicar, en `deploy/publicacion/`. `desplegar.sh` y el `Caddyfile` se quedan donde están porque ya los citan otras historias. El paquete se arma desde esas rutas.

## Fases de implementación (guía para /speckit-tasks)

1. **D-58:** aprobación del autor.
2. **Archivos del equipo**, con su prueba primero: `compose.vps.yml`, `Caddyfile`, `probar_humo.sh` y `desplegar.sh`. Después la prueba del ciclo (`probar_equipo.sh` con `registry:2` y la API simulada), que tiene que fallar antes de `actualizar.sh` y pasar después.
3. **Publicación:** `despliegue.Dockerfile`, `construir.sh`, `publicar.sh` y `cd.yml` (revisión con Grype antes del inicio de sesión).
4. **Preparación del equipo:** `preparar.sh` y las unidades de systemd; probado en la CI en modo no interactivo y en el PC del autor.
5. **Cierre de ola:** `cierre-ola.yml` reescrito, probado con `workflow_dispatch`.
6. **Pull request:** los 10 checks y `perfil-consolidado` con el paso nuevo en verde; la demostración del escenario 2 con una imagen vulnerable.
7. **Autor:** Docker Hub, entornos y tokens (quickstart §1) y `preparar.sh` en su PC (quickstart §2).
8. **Cierre:** fusión con la aprobación del autor; comprobar en el PC el despliegue de `main` (escenarios 1, 3 y 5, SC-007); evidencia con `Closes #25`; cierre del feature #2.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Paquete de despliegue como imagen (D-58) | La configuración del despliegue cambia junto con las imágenes, y la vuelta atrás tiene que volver a las dos | Instalar los archivos a mano obliga a desplegar a mano cuando cambian; clonar el repositorio necesita un token con lectura de código. |
| `actualizar.sh` con `HEAD` al registro | Consultar cada 5 minutos sin gastar el cupo de 200 descargas cada 6 horas | `docker pull` en cada consulta gasta el 36 % del cupo sin desplegar nada. |
| Prueba del ciclo con registro y API simulados | Los escenarios 3 y 4 tienen que probarse en cada PR, sin Docker Hub ni el PC | Probarlos solo en el PC no se repite y no deja evidencia en la CI. |
