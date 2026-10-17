# Data Model: Publicación en Docker Hub y despliegue en el equipo de demostración (B00-H08)

La historia no tiene datos de negocio. Sus datos son las etiquetas del repositorio de Docker Hub, los archivos del equipo de demostración y los *deployments* de GitHub. Los detalles están en [research.md](research.md) y en [contracts/](contracts/).

## Versión publicada

Un commit de `main` publicado en el repositorio privado `<usuario>/mdd` (research §4 y §5).

| Etiqueta | Contenido | Reglas |
|---|---|---|
| `identidad-<commit>`, `datos-<commit>`, `distributivo-<commit>`, `salida-<commit>`, `gateway-<commit>`, `frontend-<commit>` | Las 6 imágenes | `<commit>`: los 12 primeros caracteres del SHA. *Label* `org.opencontainers.image.revision` con el SHA completo. Solo existen si las 6 pasaron Grype (FR-003). |
| `despliegue-<commit>` | Paquete de despliegue (D-58) | `FROM scratch`. Archivos: `compose.demo.yml`, `Caddyfile`, `01-esquemas.sh`, `desplegar.sh`, `probar_humo.sh` y `VERSION` (el SHA completo). |
| `despliegue-main` | La misma imagen que el `despliegue-<commit>` más reciente | Se mueve al final de la publicación, después de las otras 7 etiquetas. |

## Equipo de demostración

Carpeta `/opt/mdd`, de un usuario de sistema `mdd` (research §8 y §11).

| Archivo | Modo | Contenido | Lo usa |
|---|---|---|---|
| `configuracion.env` | 0600 | `DOCKERHUB_USUARIO`, `DOCKERHUB_TOKEN` (solo lectura), `GITHUB_REPOSITORIO`, `GITHUB_TOKEN` (*deployments*), `GITHUB_ENTORNO=demostracion` | `actualizar.sh` |
| `.env` | 0600 | `POSTGRES_PASSWORD`, `DB_CLAVE_IDENTIDAD`, `DB_CLAVE_DATOS`, `DB_CLAVE_DISTRIBUTIVO`, `DB_CLAVE_SALIDA`, `MDD_CLAVE_PROCESO_DISTRIBUTIVO`, `MDD_USUARIO_PRUEBA_CLAVE`, `DEMO_USUARIO`, `DEMO_CLAVE`, `DEMO_CLAVE_HASH`, `DIRECCION` (por defecto `mdd.localhost`), `REPOSITORIO` (forma completa: `docker.io/p980/mdd`; única fuente, también para `actualizar.sh`), `ETIQUETA` y, opcional, `TUNEL_TOKEN` | Compose (interpolación), `probar_humo.sh` y `actualizar.sh` |
| `secretos/jwt.pem` | 0400, dueño 10001 | Llave RSA de 2048 bits en PKCS#8 | `identidad` (ADR-014), montada `:ro,Z` |
| `versiones/<commit>/` | 0750 | El paquete extraído | `desplegar.sh` |
| `actual`, `anterior` | enlaces | A una carpeta de `versiones/` | `desplegar.sh` |
| `estado/ultimo-digest` | 0640 | *Digest* de `despliegue-main` ya desplegado (o intentado) | `actualizar.sh` |

**Reglas:**
- Los secretos se generan en el equipo y no salen de él (FR-013). `.env.ejemplo` no se usa en el equipo, y `MDD_USUARIO_PRUEBA_CLAVE` nunca vale `cambiar-en-local-…` (D-57).
- `DEMO_CLAVE_HASH` va entre comillas simples, porque el hash de Caddy tiene `$` y Compose los interpolaría.
- Se guardan las 3 últimas carpetas de `versiones/`.

## Despliegue

Un intento de pasar de una versión a otra. Vive en el registro de systemd y en GitHub (D-56).

| Campo | Origen |
|---|---|
| Commit | `VERSION` del paquete |
| Entorno | `demostracion` |
| Estado | `in_progress` al empezar; `success` o `failure` al terminar |
| Descripción | «Sano en N s», «Vuelta atrás a <commit>: <servicio> no quedó sano», «Prueba de humo fallida: <paso>», «Error de Docker Hub: <código>» |

**Estados:**

```text
                      ┌─ todos sanos en 180 s y prueba de humo OK ──► success (actual = nueva)
consulta ─ digest nuevo ─ despliega ─┤
   │                                 └─ no sano o humo falla ─► vuelve a anterior ─► failure
   └─ digest igual o Docker Hub no responde ─► nada (solo registro local)
```

## Entornos de GitHub

| Entorno | Restricción | Secretos y variables | Lo usa |
|---|---|---|---|
| `publicacion` | Solo `main` | Secreto `DOCKERHUB_TOKEN` (escritura); variables `DOCKERHUB_USUARIO` y `DOCKERHUB_REPOSITORIO` | `cd.yml` |
| `demostracion` | Ninguna | Ninguno | Los *deployments* que informa el equipo |
