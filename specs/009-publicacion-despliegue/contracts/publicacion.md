# Contrato: publicación en Docker Hub (`cd.yml`)

## Disparo y permisos

| Clave | Valor |
|---|---|
| Evento | `push` a `main` (solo llegan fusiones con los 10 checks en verde, D-31); `pull_request` hacia `main` que toque `deploy/**`, `compose.yaml`, `.grype.yaml` o `.github/workflows/cd.yml`; y `workflow_dispatch` |
| Concurrencia | Grupo `cd`, sin cancelar: las publicaciones van en orden |
| Entorno | `publicacion` solo con `push` a `main` (`environment` con una expresión). En un `pull_request` o en otra rama, el job construye y revisa sin entorno, sin secretos y sin subir nada. El check se llama `5 · imágenes` y **no** es obligatorio (D-31 no cambia). GitHub solo permite `workflow_dispatch` de un workflow que ya está en la rama por defecto, así que antes de la fusión el escenario 2 se demuestra con un pull request (análisis del 2026-10-17, H1) |
| Permisos | `contents: read` |
| Límite | 45 minutos |

## Pasos

| Paso | Qué hace | Falla si |
|---|---|---|
| Construcción | `deploy/publicacion/construir.sh <commit>`: `docker buildx bake -f compose.yaml` de los 6 servicios con el *label* `org.opencontainers.image.revision`, y el paquete con `deploy/docker/despliegue.Dockerfile` | Una imagen no compila |
| Revisión | `deploy/publicacion/revisar.sh <commit>` (abajo), con Grype `v0.120.1`; el resumen va al del job | Una alta o crítica con arreglo sin excepción vigente, en cualquiera |
| Inicio de sesión | Solo con `push` a `main`: `docker/login-action` con `DOCKERHUB_USUARIO` y `DOCKERHUB_TOKEN` | Credencial inválida |
| Publicación | Solo con `push` a `main`: `deploy/publicacion/publicar.sh <repositorio> <commit>`: sube las 7 etiquetas `<componente>-<commit>` y `despliegue-<commit>`, y al final mueve `despliegue-main` | Un `push` falla: `despliegue-main` no se mueve |
| Resumen | Repositorio, commit, las 7 etiquetas y sus *digest* | — |

La revisión termina antes del inicio de sesión: si falla, no se sube nada (FR-003).

## `deploy/publicacion/construir.sh`

**Uso:** `construir.sh <commit>`. Deja en el Docker local las imágenes `mdd/<componente>:<commit12>` y `mdd/despliegue:<commit12>`. Lo usan `cd.yml` y la prueba de `perfil-consolidado` (research §12).

## `deploy/publicacion/revisar.sh`

**Uso:** `revisar.sh <commit>`. Revisa con Grype las 6 imágenes `mdd/<componente>:<commit12>` y las de terceros de `deploy/equipo/compose.demo.yml` (las `image:` con *digest*, leídas con `yq`, incluido el perfil `tunel`), con `deploy/sca/excepciones.sh` y `deploy/sca/resumen.sh` de B00-H07. Termina con el código de `resumen.sh`, o con 1 si Grype falla.

## `deploy/publicacion/publicar.sh`

**Uso:** `publicar.sh <repositorio> <commit>`, con sesión iniciada en el registro. Etiqueta y sube en este orden: los 6 componentes, `despliegue-<commit12>` y, por último, `despliegue-main`. Termina con 1 si un `push` falla.

## `deploy/docker/despliegue.Dockerfile`

`FROM scratch`. Copia `deploy/equipo/compose.demo.yml`, `deploy/Caddyfile`, `deploy/postgres/01-esquemas.sh`, `deploy/desplegar.sh` y `deploy/equipo/probar_humo.sh` a `/paquete/`, y escribe el commit en `/paquete/VERSION` (argumento de construcción). Necesita un `CMD` ficticio para que `docker create` funcione.
