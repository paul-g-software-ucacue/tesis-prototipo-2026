---

description: "Tareas de B00-H08: Publicación en Docker Hub y despliegue en el equipo de demostración"
---

# Tasks: Publicación en Docker Hub y despliegue en el equipo de demostración

**Input**: documentos de diseño en `specs/009-publicacion-despliegue/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas. `probar_equipo.sh` (el ciclo completo con un registro local y una API de GitHub simulada) se escribe antes que `actualizar.sh` y tiene que fallar sin él. `probar_humo.sh` es a la vez la prueba del escenario 5 y un paso del despliegue.

**Decisiones**: D-31, D-36, D-49, D-52, D-54, D-55, D-56, D-57, D-58, ADR-013, ADR-014, ADR-015 y ADR-018 en [`docs/decisiones.md`](../../docs/decisiones.md); constitución 2.1.3 (principio IX).

**Organization**: `compose.demo.yml`, el `Caddyfile`, el paquete y `construir.sh` son fundacionales, porque los usan la publicación y el equipo. Las historias de usuario son las de la especificación:
- **US1:** publicación;
- **US2:** imagen vulnerable;
- **US3:** despliegue sano;
- **US4:** vuelta atrás;
- **US5:** acceso restringido;
- **US6:** cierre de ola sin URL pública.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US6.
- Las rutas son relativas a la raíz del repositorio.

## Convenciones para todas las tareas

- **Herramientas en contenedor** (Grype, yq, shellcheck y actionlint): sobre copias en el *scratchpad*, con `docker cp` o leyendo la entrada estándar. Nunca montar el repositorio ni el `home` del autor; el único montaje con `:Z` es `/opt/mdd/secretos` o su equivalente temporal en las pruebas. Docker, con `sg docker -c '…'`.
- **Versiones y fijación:** imágenes de terceros por versión exacta y *digest*; acciones por SHA con la versión en un comentario. Cada versión nueva con al menos dos semanas (research §1).
- **Secretos:** ninguno en el repositorio, en GitHub (salvo `DOCKERHUB_TOKEN` en `publicacion`) ni en una imagen. Las pruebas usan valores ficticios generados en el momento. Los scripts nunca imprimen un secreto ni un token.
- **Scripts de Bash:** `set -euo pipefail`, comentario de propósito y de uso en español, el patrón `verificar` de `deploy/probar_errores.sh` en las pruebas, y `shellcheck` sin avisos. En los workflows, `shell: bash` donde haya tuberías (lección de B00-H07).
- **Portabilidad:** nada específico de Fedora; lo que haga falta para SELinux (`:Z`) no debe tener efecto en Ubuntu (FR-010).
- **Evidencia:** en `specs/009-publicacion-despliegue/evidencia.md`. Commits con `Refs #25`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/009-publicacion-despliegue/evidencia.md` con estas secciones:
  - entorno;
  - escenario 1 (publicación);
  - escenario 2 (imagen vulnerable);
  - escenario 3 (despliegue sano);
  - escenario 4 (vuelta atrás);
  - escenario 5 (acceso restringido);
  - cierre de ola (US6);
  - informes a GitHub (D-56);
  - memoria (R-19);
  - notas.
- [X] T002 [P] Fijar las versiones de research §1 y anotarlas en el «entorno» de la evidencia:
  - `caddy:2.11.7-alpine` (o la última 2.11.x con al menos dos semanas) con el *digest* de su índice;
  - el *digest* de `postgres:18.6-alpine`;
  - `cloudflare/cloudflared` en su última versión con al menos dos semanas, con *digest*;
  - `registry:2` con *digest*, para las pruebas;
  - los SHA de `docker/setup-buildx-action` y `docker/login-action` (con `gh api` sobre sus tags).

---

## Phase 2: Foundational (bloquea a todas las historias)

- [X] T003 Crear `deploy/equipo/compose.demo.yml` según contracts/equipo.md:
  - servicios `postgres`, `identidad`, `datos`, `distributivo`, `salida`, `gateway`, `frontend` y `caddy`, con las imágenes `${REPOSITORIO}:<componente>-${ETIQUETA}` y las de terceros por *digest* (T002);
  - los mismos *healthchecks*, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=60`, `SPRING_DATASOURCE_URL`, `depends_on` y límites de memoria de `compose.yaml`, más `caddy` con 64m: «3 072 MiB» en total;
  - **variables explícitas por servicio** (sin `env_file`): cada proceso recibe solo su `DB_CLAVE_*`; `identidad`, además, `MDD_CLAVE_PROCESO_DISTRIBUTIVO`, `MDD_USUARIO_PRUEBA_HABILITADO=true`, `MDD_USUARIO_PRUEBA_CLAVE` y `MDD_LLAVE_PRIVADA=/run/mdd/jwt.pem`; `distributivo`, `MDD_CLAVE_PROCESO_DISTRIBUTIVO`;
  - la llave montada como `${MDD_SECRETOS:-/opt/mdd/secretos}/jwt.pem:/run/mdd/jwt.pem:ro,Z`, y `01-esquemas.sh` y el `Caddyfile` desde la carpeta del paquete (rutas relativas al archivo), con `:ro,z`;
  - `caddy` publica solo `127.0.0.1:443:443` y monta un volumen para sus datos (la CA interna);
  - `cloudflared` en el perfil `tunel`, con `TUNEL_TOKEN`, sin puertos (FR-016);
  - comentario de cabecera: uso con `-p mdd --env-file`, D-55 y D-58.
- [X] T004 Cambiar `deploy/Caddyfile` según contracts/equipo.md y research §9:
  - un fragmento `(mdd)` con el `basic_auth` para todo menos `/api/*`, `/api/*` → `gateway:8080`, lo demás → `frontend:8080`, y las cabeceras actuales;
  - el sitio `{$DIRECCION:mdd.localhost}`, con `tls internal`, que importa el fragmento (sin modo Let's Encrypt: M1 y M2 del análisis);
  - el sitio `http://:8081`, que importa el fragmento, para el túnel.

  Actualizar el comentario de cabecera y la referencia de `specs/007-spa-base/contracts/perfil.md` («en el equipo de demostración, detrás de Caddy»).
- [X] T005 [P] Crear `deploy/docker/despliegue.Dockerfile` (contracts/publicacion.md, D-58):
  - `FROM scratch`, con `ARG REVISION`;
  - `COPY` de `deploy/equipo/compose.demo.yml`, `deploy/Caddyfile`, `deploy/postgres/01-esquemas.sh`, `deploy/desplegar.sh` y `deploy/equipo/probar_humo.sh` a `/paquete/`;
  - `VERSION` con el commit, escrito con una etapa previa de `busybox` fijada por *digest*;
  - un `CMD ["/nada"]` para que `docker create` funcione.
- [X] T006 [P] Crear `deploy/publicacion/construir.sh <commit>` (contracts/publicacion.md):
  - `docker buildx bake -f compose.yaml --load` de `identidad`, `datos`, `distributivo`, `salida`, `gateway` y `frontend`, con `--set <servicio>.tags=mdd/<servicio>:<commit12>` y `--set *.labels.org.opencontainers.image.revision=<commit>`;
  - `docker build -f deploy/docker/despliegue.Dockerfile --build-arg REVISION=<commit> -t mdd/despliegue:<commit12> .`;
  - imprime la lista de imágenes construidas.
- [X] T007 Comprobar en local:
  - `construir.sh $(git rev-parse HEAD)` deja las 7 imágenes `mdd/*:<commit12>`, con el *label* de revisión en las 6;
  - el paquete, extraído con `docker create` y `docker cp`, tiene los 6 archivos;
  - `docker compose -p mdd --env-file <ejemplo> -f deploy/equipo/compose.demo.yml config` resuelve las imágenes, las variables por servicio y los montajes, con un `.env` de ejemplo con valores ficticios en el *scratchpad*.

  Anotarlo en la evidencia.

**Checkpoint**: una sola definición de las imágenes, y un paquete con todo lo que el equipo necesita.

---

## Phase 3: User Story 1 - Cada fusión a main publica las imágenes revisadas (Priority: P1) 🎯 MVP

**Goal**: las 7 etiquetas `<componente>-<commit>` en el repositorio privado y, al final, `despliegue-main` (escenario 1; FR-001, FR-004 y FR-005; SC-001).

**Independent Test**: contra un registro local, `publicar.sh` sube las 7 etiquetas y mueve `despliegue-main` al final; después de la fusión, Docker Hub las tiene.

- [X] T008 [US1] Prueba primero: script temporal en el *scratchpad* que levanta `registry:2` (T002) en `localhost:5000`, corre `deploy/publicacion/publicar.sh localhost:5000/mdd <commit>` y comprueba con `curl` al registro las 7 etiquetas y que `despliegue-main` apunta al mismo *digest* que `despliegue-<commit12>`. Comprobar que falla mientras `publicar.sh` no existe.
- [X] T009 [US1] Crear `deploy/publicacion/publicar.sh <repositorio> <commit>` (contracts/publicacion.md):
  - etiqueta y sube, en orden, los 6 componentes, `despliegue-<commit12>` y, por último, `despliegue-main`;
  - termina con 1 si un `push` falla, antes de mover `despliegue-main`;
  - imprime una tabla Markdown con las etiquetas y sus *digest*.

  T008 pasa.
- [X] T010 [US1] Crear `.github/workflows/cd.yml` según contracts/publicacion.md:
  - `on: push: branches: [main]`, `pull_request` hacia `main` con `paths` (`deploy/**`, `compose.yaml`, `.grype.yaml`, `.github/workflows/cd.yml`) y `workflow_dispatch`; nombre del job y del check: `5 · imágenes` (no obligatorio, D-31 no cambia);
  - `concurrency: { group: cd, cancel-in-progress: false }`, `permissions: contents: read`;
  - el job con `environment: ${{ github.event_name == 'push' && github.ref == 'refs/heads/main' && 'publicacion' || '' }}` (si actionlint lo rechaza, dos jobs con la misma revisión y la publicación en un `if`);
  - pasos: checkout, `setup-buildx`, `construir.sh "$GITHUB_SHA"`, la revisión de T012 y, solo con `if: github.event_name == 'push' && github.ref == 'refs/heads/main'`, `login-action` con `vars.DOCKERHUB_USUARIO` y `secrets.DOCKERHUB_TOKEN` y `publicar.sh "$DOCKERHUB_REPOSITORIO" "$GITHUB_SHA"` con la tabla en `$GITHUB_STEP_SUMMARY`;
  - `shell: bash` en los pasos con tuberías; acciones por SHA.

**Checkpoint**: la publicación funciona contra un registro; falta la revisión (US2) antes de habilitarla en `main`.

---

## Phase 4: User Story 2 - Una imagen vulnerable no se publica ni se despliega (Priority: P1)

**Goal**: Grype revisa las 6 imágenes y las de terceros antes de iniciar sesión; si una falla, no se sube nada (escenario 2; FR-002 y FR-003; SC-002).

**Independent Test**: con una imagen de prueba sobre una base vulnerable, la revisión falla y el resumen dice la imagen, el paquete y la versión corregida.

- [X] T011 [US2] Prueba primero, en el *scratchpad* con el binario de Grype 0.120.1: revisar una imagen de prueba construida sobre una base antigua con una alta o crítica corregida (por ejemplo, un `alpine` viejo fijado por *digest*). Con `excepciones.sh` y `resumen.sh` (B00-H07), comprobar que el resumen la pone en «Bloquean» con la imagen, el paquete, la instalada y la corregida, y que el código es 1. Anotarlo en la evidencia.
- [X] T012 [US2] Crear `deploy/publicacion/revisar.sh <commit>`:
  - lista las 6 imágenes `mdd/<componente>:<commit12>` y las de terceros de `deploy/equipo/compose.demo.yml` (las `image:` con *digest*, leídas con `yq`, incluido el perfil `tunel`);
  - corre `excepciones.sh .grype.yaml` y Grype (`docker:<imagen>` o `registry:<ref>` para las de terceros) con `--show-suppressed -o table -o json=<nombre>.json`;
  - corre `resumen.sh` y termina con su código o con 1 si Grype falla (FR-013 de B00-H07).

  Agregar el paso «Revisión de las imágenes» a `cd.yml`, antes del inicio de sesión, con `download-grype` v7.4.2 y `grype-version: v0.120.1`, y el resumen en `$GITHUB_STEP_SUMMARY`.
- [X] T013 [US2] Comprobar en local `revisar.sh` sobre las 7 imágenes de T007 y las de terceros: sin hallazgos que bloquean (o, si los hay, actualizar la base o registrar la excepción según D-54, y anotarlo). Tiempo de la revisión en la evidencia.

**Checkpoint**: solo se publica lo que pasó Grype.

---

## Phase 5: User Story 3 - El equipo de demostración se actualiza solo y queda sano (Priority: P1)

**Goal**: el equipo detecta la versión nueva, la despliega, queda sano y pasa la prueba de humo, e informa el *deployment* (escenario 3; FR-006, FR-007, FR-009 a FR-014; SC-003, SC-005, SC-007, SC-010).

**Independent Test**: `deploy/equipo/probar_equipo.sh` publica la versión A en un registro local, prepara un equipo temporal y corre `actualizar.sh`: todo sano, humo en verde y `success` en la API simulada.

- [X] T014 [P] [US3] Crear `deploy/equipo/github_simulado.py`: un servidor HTTP mínimo con la biblioteca estándar de Python, que acepta `POST /repos/<dueño>/<repo>/deployments` (devuelve un `id`) y `POST …/deployments/<id>/statuses`, exige el encabezado `Authorization: Bearer <token de prueba>` y guarda cada petición como una línea JSON en un archivo. Sin dependencias externas.
- [X] T015 [US3] Prueba primero: crear `deploy/equipo/probar_equipo.sh` (research §12). Con el patrón `verificar`:
  0. con el registro vacío (antes de publicar nada), `actualizar.sh` termina con 0, registra «sin versión publicada» y la API simulada no recibe nada (M5);
  1. levanta `registry:2` en `localhost:5000` y `github_simulado.py`;
  2. publica la versión A con `construir.sh` (o reetiqueta las imágenes ya construidas) y `publicar.sh localhost:5000/mdd`;
  3. prepara un equipo en una carpeta temporal con `preparar.sh --no-interactivo --raiz <carpeta>`, con `REPOSITORIO=localhost:5000/mdd`, `AUTENTICACION` vacía (registro sin autenticación), `GITHUB_API=http://127.0.0.1:<puerto>` y `DIRECCION=mdd.localhost`;
  4. corre `actualizar.sh` una vez y comprueba:
     - código 0;
     - todos los contenedores del proyecto `mdd` sanos;
     - `actual` → la versión A;
     - `estado/ultimo-digest` guardado;
     - en la API simulada, un *deployment* con `ref` = el commit A y los estados `in_progress` y `success`;
  5. corre `actualizar.sh` otra vez y comprueba que, sin cambios, no despliega ni informa nada;
  6. llave persistente (SC-007, FR-012, M4): inicia sesión con `autor.prueba` por el proxy, corre `docker compose -p mdd restart identidad`, espera a que quede sano y comprueba que `GET /api/usuarios/sesion` con el **mismo** token da 200.

  Al final baja todo (`docker compose -p mdd down -v`, el registro y el simulador). Comprobar que falla mientras no existen `actualizar.sh` y `preparar.sh`.
- [X] T016 [US3] Crear `deploy/equipo/probar_humo.sh` (contracts/equipo.md): los 4 pasos con `curl --resolve "$DIRECCION:443:127.0.0.1"` (y `-k` solo si `DIRECCION` termina en `.localhost`), usando `DEMO_USUARIO`, `DEMO_CLAVE` y `MDD_USUARIO_PRUEBA_CLAVE` del `.env`; no imprime credenciales ni el token.
- [X] T017 [US3] Reescribir `deploy/desplegar.sh` según contracts/equipo.md: `desplegar.sh <carpeta de la versión>`:
  - mueve `anterior` y `actual`;
  - escribe `ETIQUETA` en `.env`;
  - corre `docker compose -p mdd --env-file "$RAIZ/.env" -f actual/compose.demo.yml pull` y `up -d --remove-orphans`;
  - espera la salud (`ESPERA_SALUD`, 180 s): ningún contenedor del proyecto `unhealthy` ni `starting`;
  - corre `actual/probar_humo.sh`;
  - imprime una línea de resumen;
  - termina con 0, 1 o 2.

  `RAIZ` por defecto `/opt/mdd` (variable para las pruebas). Mantiene el comportamiento de vuelta atrás de hoy y lo extiende a los archivos (US4).
- [X] T018 [US3] Crear `deploy/equipo/actualizar.sh` según contracts/equipo.md y research §6:
  - deriva el registro y la ruta de `REPOSITORIO` (`docker.io/<ruta>` → `registry-1.docker.io`), pide el token de lectura a `${AUTENTICACION}` (salvo que esté vacía) y hace `HEAD` de `https://<registro>/v2/<ruta>/manifests/despliegue-main` (o `http://` para `localhost`) con el *Accept* de índices y *manifests* OCI y Docker; un 404 es «sin versión publicada»: registro local, sin informe y salida con 0;
  - compara el *digest* con `estado/ultimo-digest`;
  - `docker pull`, `docker create` y `docker cp` del paquete a `versiones/<commit12>/`;
  - informe `in_progress` y después `success` o `failure` (D-56), con `curl` y `jq`;
  - `desplegar.sh`;
  - guarda el *digest*;
  - borra las versiones más viejas que las 3 últimas.

  Si el registro o GitHub no responden, registra y sigue según contracts/equipo.md. `--forzar` despliega aunque el *digest* no cambie. Lee `configuracion.env` y `.env` de `RAIZ`.
- [X] T019 [US3] Crear `deploy/equipo/preparar.sh` según research §11 y contracts/equipo.md:
  - modo normal (con `sudo`): crea el usuario de sistema `mdd` en el grupo `docker`, `/opt/mdd` (0750) y sus subcarpetas;
  - genera con `openssl rand -hex 24` `POSTGRES_PASSWORD`, las 4 `DB_CLAVE_*`, `MDD_CLAVE_PROCESO_DISTRIBUTIVO` y `MDD_USUARIO_PRUEBA_CLAVE` («nunca `cambiar-en-local-…`», D-57);
  - genera `secretos/jwt.pem` con `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048`, con dueño 10001 y modo 0400;
  - pide sin eco la contraseña de la demo y calcula `DEMO_CLAVE_HASH` con `caddy hash-password` (la imagen fijada en T002), escrito entre comillas simples;
  - pide el usuario y el token de Docker Hub y el token de GitHub, y escribe `configuracion.env` (0600); escribe `REPOSITORIO` en forma completa (`docker.io/<usuario>/mdd`) y `DIRECCION=mdd.localhost` en `.env`;
  - instala `actualizar.sh` en `bin/` y las unidades de systemd, y activa el temporizador.

  No pisa un archivo que ya existe. Modo `--no-interactivo --raiz <carpeta>` para la prueba: sin usuario de sistema ni systemd, con valores de entrada por variables.
- [X] T020 [P] [US3] Crear `deploy/equipo/mdd-actualizar.service` y `deploy/equipo/mdd-actualizar.timer` (contracts/equipo.md): `Type=oneshot`, `User=mdd`, `WorkingDirectory=/opt/mdd`, `ExecStart=/usr/bin/flock -n /opt/mdd/estado/candado /opt/mdd/bin/actualizar.sh`; temporizador con `OnBootSec=2min`, `OnUnitActiveSec=5min` y `Persistent=true`. Comprobar con `systemd-analyze verify`.
- [X] T021 [US3] Correr `probar_equipo.sh` en el PC del autor (en una carpeta temporal del *scratchpad*, no en `/opt/mdd`): pasos 1 a 5 en `OK`. Medir la memoria de los contenedores sanos con la lógica de `deploy/medir_memoria.sh` (SC-010). Anotarlo en la evidencia.
- [X] T022 [US3] Agregar a `.github/workflows/perfil-consolidado.yml` el paso «Despliegue en un equipo simulado», después de la medición de R-19 y de `docker compose down -v`: corre `deploy/equipo/probar_equipo.sh` con `shell: bash`. Subir `timeout-minutes` de 30 a 45 y anotar la duración real en la evidencia (M6). Agregar `deploy/equipo/**`, `deploy/publicacion/**`, `deploy/Caddyfile`, `deploy/desplegar.sh` y `deploy/docker/despliegue.Dockerfile` a las rutas que disparan el workflow, si las filtra.

**Checkpoint**: el equipo se actualiza solo, comprobado en el runner y en el PC.

---

## Phase 6: User Story 4 - Un despliegue que no queda sano vuelve atrás solo (Priority: P1)

**Goal**: con un proceso que no queda sano en 180 s, el equipo vuelve a la versión anterior (imágenes y archivos) y el *deployment* queda en `failure` (escenario 4; FR-008 y FR-009; SC-004 y SC-005).

**Independent Test**: `probar_equipo.sh` publica una versión B rota después de A: vuelta atrás a A, todo sano y `failure` en la API simulada.

- [X] T023 [US4] Extender `deploy/equipo/probar_equipo.sh` (prueba primero) con:
  6. publicar la versión B: el mismo commit con otro sufijo (por ejemplo, `<commit12>b`), cuyo paquete cambia en `compose.demo.yml` la `SPRING_DATASOURCE_URL` de `identidad` a una base que no existe, con `ESPERA_SALUD=60` en la prueba;
  7. `actualizar.sh` termina con código 1 (despliegue fallido), `actual` vuelve a A, todos sanos con A, y la API simulada tiene `failure` para B con «Vuelta atrás a <A>: identidad no quedó sano»;
  8. el primer despliegue de un equipo nuevo con B rota: sin `anterior`, `failure` sin vuelta atrás y los contenedores como quedaron.

  Comprobar primero que falla si `desplegar.sh` no vuelve los archivos.
- [X] T024 [US4] Ajustar `deploy/desplegar.sh` y `deploy/equipo/actualizar.sh` hasta que pasen los pasos 6 a 8: la vuelta atrás usa `anterior/compose.demo.yml` y la `ETIQUETA` anterior, y el resumen nombra el servicio que no quedó sano. Medir el tiempo de la vuelta atrás con la espera real de 180 s en una corrida en el PC (SC-004: menos de 5 minutos). Anotarlo en la evidencia.

**Checkpoint**: una versión mala nunca deja la demostración caída.

---

## Phase 7: User Story 5 - Solo quien tiene las credenciales ve el MDD (Priority: P1)

**Goal**: el proxy pide usuario y contraseña en la entrada HTTPS y en la del túnel (escenario 5; FR-015 y FR-016; SC-006).

**Independent Test**: sin credenciales, 401 en `https://localhost/` y en `http://caddy:8081/` desde la red de Compose; con ellas, la SPA.

- [X] T025 [US5] Agregar a `deploy/equipo/probar_equipo.sh`, después del paso 4, una comprobación del sitio del túnel: desde un contenedor `curlimages/curl` (fijado por *digest*) en la red del proyecto `mdd`, `http://caddy:8081/` sin credenciales da 401 y con ellas da 200; `http://caddy:8081/api/usuarios/jwks` responde sin `basic_auth` (la API la protege el token, ADR-014).
- [X] T026 [US5] Comprobar el perfil `tunel`: `docker compose --profile tunel … config` incluye `cloudflared` con `TUNEL_TOKEN` y sin puertos, y sin el perfil no aparece. Documentar en `deploy/equipo/compose.demo.yml` cómo activarlo (quickstart §5).

**Checkpoint**: el acceso restringido vale para la red local y para el túnel.

---

## Phase 8: User Story 6 - El cierre de ola no depende de una URL pública (Priority: P2)

**Goal**: `cierre-ola` levanta el perfil en el runner, corre ZAP sobre la SPA y los contratos que existen, omite k6 sin script y usa el SBOM por módulo (FR-017 a FR-019; SC-009).

**Independent Test**: `cierre-ola` con `workflow_dispatch` en la rama de la historia: ZAP sin hallazgos altos, k6 omitido con aviso y SBOM comprobado.

- [X] T027 [US6] Reescribir `.github/workflows/cierre-ola.yml` según contracts/cierre-ola.md:
  - un job `ZAP y k6 sobre el perfil` que hace `docker compose build`, `up -d --wait`, ZAP *full scan* sobre `http://localhost:8081`, y ZAP *API scan* por cada `contratos/*/openapi.yaml` (sin `comun/`), con el token de `autor.prueba` de `.env.ejemplo` y el API Gateway como destino (`-O http://localhost:8080` o el que corresponda a los `servers` del contrato);
  - k6 solo si existe `pruebas/k6/${GITHUB_REF_NAME}.js`, si no `::notice::`;
  - artefactos con los informes;
  - en `Evidencia de la ola`, `cyclonedxDirectBom` y `deploy/sca/verificar_sbom.sh` en lugar de `cyclonedxBom`.

  Sin `vars.*` ni secretos. `shell: bash` en las tuberías.
- [ ] T028 [US6] Lanzar `cierre-ola` con `gh workflow run cierre-ola.yml --ref 009-publicacion-despliegue` (después de T030). Comprobar que ZAP de la SPA y de cada contrato terminan sin altos (o corregir lo que encuentren, o anotarlo con su justificación si es un falso positivo), que k6 se omite con el aviso y que el job de evidencia se omite fuera de un tag. Anotar el run en la evidencia.

**Checkpoint**: la ola 0 se puede cerrar con su tag sin el equipo de demostración.

---

## Phase 9: Polish, pull request y cierre

- [ ] T029 Correr `actionlint` sobre `cd.yml`, `cierre-ola.yml` y `perfil-consolidado.yml`, `shellcheck` sobre `deploy/equipo/*.sh`, `deploy/publicacion/*.sh` y `deploy/desplegar.sh`, y Gitleaks (`--log-opts="--text HEAD"`) sobre un clon de la rama: sin hallazgos. Actualizar `backend/plantillas/README.md` o el README que corresponda con una línea que remita al quickstart de esta historia para el despliegue. Comprobar con `git status` que no hay archivos temporales.
- [ ] T030 Hacer commit y push, y abrir el pull request de `009-publicacion-despliegue` hacia `main` **en borrador**, con `Refs #25`. Esperar los runs del borrador y marcarlo listo. Comprobar:
  - los 10 checks en verde;
  - `perfil-consolidado`, con el paso «Despliegue en un equipo simulado» en verde (escenarios 3 y 4 en la CI);
  - `5 · imágenes` (`cd.yml` en el pull request): construye y revisa las 6 imágenes y las de terceros, sin entorno, sin inicio de sesión y sin subir nada.
- [ ] T031 [US2] Demostración del escenario 2, **con la aprobación del autor**: en una rama `demo-h08-vulnerable` (desde la de la historia), cambiar la base de la etapa final de `frontend.Dockerfile` a una versión con una alta o crítica corregida (la de T011) y abrir un pull request hacia `main`. Comprobar que `5 · imágenes` falla con esa imagen en «Bloquean» y que no hubo inicio de sesión ni `push`. Cerrarlo sin fusionar, borrar la rama y anotar el run (H1: `workflow_dispatch` no sirve antes de que `cd.yml` esté en `main`).
- [ ] T032 **Pasos del autor** (quickstart §1 y §2), con su ayuda y sin que ningún secreto pase por el chat:
  - repositorio privado y tokens en Docker Hub;
  - entornos `publicacion` y `demostracion`, con `DOCKERHUB_*`;
  - token de grano fino de *deployments*;
  - `sudo deploy/equipo/preparar.sh` en su PC, desde la rama de la historia.

  Comprobar con `gh secret list --env publicacion` y `gh variable list --env publicacion` (solo nombres) y con `systemctl status mdd-actualizar.timer`.
- [ ] T033 Completar `evidencia.md` y la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**. Después, en `main`:
  - `cd.yml` publica las 7 etiquetas y `despliegue-main` (escenario 1, SC-001: tiempo);
  - en menos de 15 minutos el PC despliega solo y queda sano (escenario 3, SC-003), y *Deployments → demostracion* muestra `success` con el commit (D-56, SC-005);
  - `https://mdd.localhost` pide credenciales (escenario 5, SC-006);
  - el token sigue valiendo después de `docker compose -p mdd restart identidad` (SC-007);
  - `gh secret list` de los dos entornos solo muestra `DOCKERHUB_TOKEN` (SC-008);
  - la memoria del equipo está por debajo de 7 GiB (SC-010).

  En las notas de la evidencia, anotar el riesgo para historias futuras: `01-esquemas.sh` solo corre al crear el volumen, así que un esquema nuevo no aparece en el equipo sin recrearlo (L3).

  Abrir el pull request de evidencia con `Closes #25` y, cuando se fusione, cerrar el feature #2 si todas sus historias están cerradas. Borrar las ramas fusionadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: T001 y T002 son independientes; T002 va antes que T003.
- **Foundational (fase 2)**: T003 y T004 antes que T005; T005 y T006 en cualquier orden; T007 cierra la fase.
- **US1 (fase 3)**: depende de la fase 2. T008 antes que T009; T010 depende de T009.
- **US2 (fase 4)**: depende de la fase 2. T011 antes que T012; T012 completa `cd.yml` (T010); T013 después.
- **US3 (fase 5)**: depende de la fase 2 y de T009 (publicar en el registro local). T014 es libre; T015 va antes que T016 a T019; T020 es libre; T021 y T022 al final.
- **US4 (fase 6)**: depende de US3. T023 antes que T024.
- **US5 (fase 7)**: depende de US3 (el equipo de la prueba). T025 y T026 en cualquier orden.
- **US6 (fase 8)**: T027 es independiente de las fases 3 a 7; T028 depende de T030.
- **Cierre (fase 9)**: T029 y T030 dependen de todo lo anterior; T031 y T028 de T030; T032 puede hacerse en paralelo desde que existe `preparar.sh` (T019); T033 depende de todo y de la aprobación del autor.

### Parallel Opportunities

- Fase 1: T001 y T002.
- Fase 2: T005 y T006.
- US1 y US2 avanzan en paralelo con US3 desde T009.
- US3: T014 y T020.
- US6 (T027) en cualquier momento.
- T032 (pasos del autor) en paralelo con el pull request.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. Fases 1 y 2: `compose.demo.yml`, `Caddyfile`, el paquete y `construir.sh`.
2. US1: `publicar.sh` contra un registro local y `cd.yml`.
3. **Validar:** las 7 etiquetas y `despliegue-main` en el registro local.

### Incremental

1. **US2:** la revisión con Grype antes de publicar.
2. **US3 y US4:** el equipo con su prueba de punta a punta, en el PC y en la CI.
3. **US5:** el acceso restringido en las dos entradas.
4. **US6:** el cierre de ola en el runner.
5. **Cierre:** pull request, demostración del escenario 2, pasos del autor, fusión y comprobación en el PC.

---

## Notes

- Hasta T032, nada toca `/opt/mdd` ni systemd en el PC del autor: las pruebas usan carpetas temporales y el proyecto `mdd` se baja al terminar. Si el autor tiene levantado el perfil local (`compose.yaml`), comparten Docker pero no puertos (el equipo solo publica `127.0.0.1:443`).
- Las demostraciones solo usan valores ficticios.
- Hacer commit por tarea o por grupo lógico en `009-publicacion-despliegue`, con `Refs #25`.
