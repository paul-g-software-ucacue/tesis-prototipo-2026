# Research: Publicación en Docker Hub y despliegue en el equipo de demostración (B00-H08)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. «Comprobado» significa ensayado el 2026-10-17 en el PC del autor (Docker 29.8.2, buildx 0.38.0) o leído en el código y la documentación oficial citada.

## 1. Versiones e imágenes de terceros

| Pieza | Versión | Fijada por | Nota |
|---|---|---|---|
| PostgreSQL | 18.6-alpine | *digest* `sha256:77f585114c32fbca283dc835b0596f4e52b51b4c6662d7810b2f4084f60a1873` (índice) | La misma versión del perfil consolidado |
| Caddy | 2.11.7-alpine | *digest* del índice, tomado al implementar | `caddy:2-alpine` resuelve hoy a 2.11.7; se fija la versión exacta, con al menos dos semanas |
| cloudflared (opcional) | la última con al menos dos semanas | *digest* | Solo en el perfil `tunel` (§9) |
| `docker/setup-buildx-action`, `docker/login-action` | las versiones vigentes | SHA | Se fijan con `pinact`, como las demás |

Las 6 imágenes propias se construyen desde los Dockerfiles existentes (ADR-013, ADR-015).

## 2. Construcción: `docker buildx bake` sobre `compose.yaml`

- **Decisión:** `cd.yml` construye las 6 imágenes con `docker buildx bake -f compose.yaml`, con `--set <servicio>.tags=…` y `--set *.labels.org.opencontainers.image.revision=<commit>`.
- **Comprobado:** `bake --print` resuelve los 6 servicios de `compose.yaml` con el mismo `context`, `dockerfile` y `args` (`MODULO`, `NOMBRE`) que usa el perfil, y aplica las etiquetas y el *label* pedidos. Así la definición de cada imagen vive en un solo lugar (FR-001).
- **Alternativas:** `docker build` por servicio en un script (repite los argumentos de `compose.yaml`); `docker compose build` y después `docker tag` (no admite *labels* por línea de comandos).

## 3. Revisión de las imágenes con Grype

- **Decisión:** antes de iniciar sesión en Docker Hub, Grype revisa las 6 imágenes recién construidas (`docker:<imagen>`) y las de terceros que usa el despliegue (PostgreSQL, Caddy y cloudflared, por su *digest*). Usa `deploy/sca/excepciones.sh` y `deploy/sca/resumen.sh` de B00-H07 sin cambios: el nombre de cada JSON es el de la imagen, así que el resumen dice la imagen, el paquete, la instalada y la corregida (FR-002, D-54).
- **Si alguna falla, no se publica ninguna** (FR-003): la revisión termina antes del primer `docker push`.
- **Por qué:** una sola regla y un solo archivo de excepciones para dependencias e imágenes.
- **Alternativas:** revisar después de publicar y borrar las etiquetas (deja una ventana con imágenes vulnerables en el repositorio).

## 4. Etiquetas y punto de publicación

- **Decisión:** en el repositorio privado único (`<usuario>/mdd`):
  - las 6 imágenes, con la etiqueta `<componente>-<commit>`, donde `<commit>` son los 12 primeros caracteres del SHA;
  - el **paquete de despliegue** (§5), con `despliegue-<commit>`;
  - y, **al final**, la etiqueta móvil `despliegue-main`, que apunta al paquete del último commit publicado.
- **Por qué:** el equipo solo mira `despliegue-main`. Como se mueve después de subir las 7 imágenes del commit, el equipo nunca ve una versión a medias (FR-004). Las 6 imágenes no necesitan etiqueta móvil: el paquete dice qué commit desplegar.
- **Alternativas:** una etiqueta móvil por componente (el equipo podría verlas desfasadas); consultar la API de Docker Hub por la etiqueta más reciente (otra credencial y otra API).

## 5. Paquete de despliegue como imagen

- **Decisión:** una imagen `FROM scratch` con los archivos del despliegue de ese commit: `compose.vps.yml`, `Caddyfile`, `01-esquemas.sh`, `desplegar.sh`, `probar_humo.sh` y un archivo `VERSION` con el commit. El equipo la baja con `docker pull` y extrae los archivos con `docker create` y `docker cp`.
- **Por qué:**
  - si una versión necesita un cambio en el despliegue (una variable nueva, un servicio nuevo), el cambio viaja con las imágenes del mismo commit, y la vuelta atrás también vuelve a los archivos anteriores;
  - usa la misma credencial de solo lectura de Docker Hub: el equipo no necesita acceso al repositorio de GitHub, que es privado;
  - no tiene paquetes del sistema, así que Grype no tiene nada que revisar.
- **Se registró como D-58**, aprobada por el autor el 2026-10-17, porque el issue habla de 6 imágenes: esta es un séptimo artefacto, técnico, sin servicio.
- **Alternativas:** clonar el repositorio en el equipo (necesitaría un token con lectura de código); instalar los archivos a mano una vez (un cambio de `compose.vps.yml` obligaría a desplegar a mano, y la vuelta atrás no volvería a la configuración anterior); un artefacto OCI con ORAS (una herramienta más en el equipo).

## 6. Consulta desde el equipo sin gastar el cupo de Docker Hub

- **Docker Hub** (documentación oficial): una cuenta personal autenticada tiene 200 descargas cada 6 horas; una descarga es un `GET` del *manifest*, y las peticiones `HEAD` **no cuentan**.
- **Decisión:** cada 5 minutos, `actualizar.sh` pide un token de lectura a `auth.docker.io` con el usuario y el token de solo lectura, y hace `HEAD` del *manifest* de `despliegue-main`. Compara el *digest* (`Docker-Content-Digest`) con el del último despliegue. Solo si cambió, hace `docker pull` del paquete y despliega. Un despliegue baja 7 imágenes: 7 descargas.
- **Por qué:** 72 consultas cada 6 horas no gastan cupo, y un despliegue gasta 7.
- **Alternativas:** `docker pull` en cada consulta (72 descargas cada 6 horas, el 36 % del cupo sin desplegar nada); Watchtower (despliega contenedor por contenedor, sin vuelta atrás conjunta ni prueba de humo).

## 7. Servicio del equipo: systemd

- **Decisión:** `mdd-actualizar.service` (de un solo disparo) y `mdd-actualizar.timer` (cada 5 minutos, `Persistent=true`, para que corra al encender si se perdió una consulta). El servicio corre como un usuario de sistema propio, `mdd`, miembro del grupo `docker`, con `/opt/mdd` como directorio de trabajo. Un candado (`flock`) evita dos despliegues a la vez.
- **Por qué:** systemd está en Fedora, en Ubuntu Server y en la VM del IST (FR-010). Un usuario propio separa los secretos del despliegue de la cuenta personal del autor.
- **Alternativas:** un temporizador de usuario de systemd (solo corre con la sesión iniciada, salvo `loginctl enable-linger`); cron (sin registro integrado ni `Persistent`).

## 8. Estructura del equipo y despliegue con vuelta atrás

```text
/opt/mdd/                       (dueño mdd, 0750)
├── configuracion.env           (0600) repositorio, usuario y token de lectura de Docker Hub, token de GitHub
├── .env                        (0600) secretos de la aplicación y ETIQUETA (los lee Compose)
├── secretos/jwt.pem            (dueño 10001, 0400) llave de firma de Usuarios
├── versiones/<commit>/         paquete extraído de cada versión desplegada (se guardan las 3 últimas)
├── actual -> versiones/<commit>
├── anterior -> versiones/<commit>
└── estado/ultimo-digest        digest de despliegue-main ya desplegado
```

- **`desplegar.sh`** (cambia): recibe la carpeta de la versión nueva. Mueve `actual` y `anterior`, escribe `ETIQUETA` en `.env`, corre `docker compose -p mdd --env-file /opt/mdd/.env -f actual/compose.vps.yml pull` y `up -d --remove-orphans`, espera hasta 180 s a que todos queden sanos (como hoy) y corre `probar_humo.sh`. Si algo falla, vuelve `actual` a `anterior` y levanta esa versión. El nombre de proyecto fijo (`-p mdd`) hace que los contenedores y el volumen de PostgreSQL sean los mismos aunque cambie la carpeta.
- **`actualizar.sh`** (nuevo, se instala una vez): la consulta de §6, la extracción del paquete, la llamada a `desplegar.sh` y el informe a GitHub (§10). Si Docker Hub no responde, registra el error y sale sin tocar nada.
- **Comprobado en el código:** las imágenes de Java corren como el uid 10001 (`java.Dockerfile`). La llave se monta de solo lectura con `:ro,Z`, y su archivo pertenece al uid 10001 con modo 0400. En Fedora, `Z` reetiqueta solo esa carpeta (no el `home` del autor); en Ubuntu no tiene efecto.

## 9. Proxy, acceso y túnel

- **Decisión:** el `Caddyfile` del paquete define el sitio con la variable `DIRECCION`:
  - por defecto `localhost`: Caddy sirve HTTPS con su CA interna, en `127.0.0.1:443`;
  - con un dominio propio que apunte al equipo: certificado de Let's Encrypt (hay que abrir 80 y 443);
  - y un segundo sitio, `http://:8081` dentro de la red de Compose y sin publicar, para el túnel.

  Los dos sitios importan el mismo fragmento: `basic_auth` para todo menos `/api/*`, `/api/*` al API Gateway y lo demás a la SPA, con las cabeceras de seguridad de hoy (FR-015).
- **Túnel (opcional):** un servicio `cloudflared` en el perfil de Compose `tunel`, que solo arranca si el equipo define su token. Sale hacia Cloudflare y llega a `http://caddy:8081`, sin puertos de entrada (FR-016). Necesita un dominio administrado por Cloudflare (gratis para el túnel).
- **Alternativas:** Tailscale Funnel (no necesita dominio, pero agrega un demonio en el anfitrión); publicar 443 en todas las interfaces sin túnel (expone el PC en la red de la casa).

## 10. Informe a GitHub Deployments (D-56)

- **Decisión:** `actualizar.sh` crea un *deployment* (`POST /repos/{dueño}/{repo}/deployments`, con `ref` = el commit, `environment` = `demostracion`, `auto_merge: false`, `required_contexts: []`) y le pone los estados `in_progress`, después `success` o `failure`, con una descripción breve (sano, vuelta atrás a `<commit>`, error de Docker Hub). Usa `curl` y `jq`.
- **Token:** de grano fino, solo para este repositorio, con el permiso *Deployments: Read and write* (más *Metadata: Read*, que GitHub agrega siempre), con vencimiento hasta el fin de la tesis. Vive en `configuracion.env`.
- **Si GitHub no responde,** el despliegue sigue y el informe queda en el registro de systemd.
- **Entornos de GitHub:** `publicacion`, restringido a `main`, con el token de escritura de Docker Hub; y `demostracion`, sin secretos, donde aparecen los *deployments*.
- **Alternativas:** un único entorno (las reglas de rama de `publicacion` no tienen sentido para los informes del equipo).

## 11. Secretos del equipo: `preparar.sh`

- **Decisión:** `deploy/equipo/preparar.sh` prepara el equipo una vez, sin mostrar ningún secreto:
  - crea el usuario `mdd` y `/opt/mdd` con sus permisos;
  - genera las claves de la base, la del proceso distributivo (D-49) y la de `autor.prueba` (D-57), con `openssl rand`;
  - genera la llave RSA de 2048 bits en PKCS#8 (ADR-014);
  - pide la contraseña del acceso de la demo sin mostrarla y guarda su hash de Caddy;
  - pide el usuario y el token de solo lectura de Docker Hub y el token de GitHub;
  - instala `actualizar.sh` y las unidades de systemd.

  Si un archivo ya existe, no lo pisa.
- **Por qué:** reemplaza los pasos manuales de la guía que se le pasó al autor y evita errores de permisos.
- **La prueba de humo necesita la contraseña de la demo en claro;** se guarda en `.env` (0600) junto a su hash.

## 12. Prueba de punta a punta del despliegue en la CI

- **Decisión:** un paso nuevo de `perfil-consolidado`, al final y después de bajar el perfil, simula el ciclo completo en el runner:
  1. un registro local (`registry:2`, fijado por *digest*);
  2. publica en él la versión A con el mismo script que usa `cd.yml`;
  3. prepara un equipo en una carpeta temporal con `preparar.sh` en modo no interactivo;
  4. corre `actualizar.sh` una vez: despliegue sano, prueba de humo en verde;
  5. publica una versión B rota (un paquete cuyo `identidad` apunta a una base que no existe);
  6. corre `actualizar.sh`: vuelta atrás a A, todo sano otra vez;
  7. un servidor HTTP mínimo en el runner hace de API de GitHub y guarda los informes: se comprueba que llegaron `success` para A y `failure` para B.
- **Por qué:** los escenarios 3 y 4 se prueban en cada pull request que toque el despliegue, sin Docker Hub ni el PC (principio VIII).
- **Alternativas:** probar solo en el PC del autor (no se repite en la CI); un runner propio (expone el PC).

## 13. Cierre de ola sin URL pública (D-55)

- **Decisión:** `cierre-ola.yml` levanta el perfil consolidado en el runner, como `perfil-consolidado`, y en el mismo job corre:
  - ZAP sobre la SPA (`http://localhost:8081`);
  - ZAP sobre la API de cada `contratos/<servicio>/openapi.yaml` que exista (sin `comun/`), con el token de `autor.prueba` del perfil local;
  - k6 si existe `pruebas/k6/<tag>.js`, y si no, un aviso.

  La release usa `cyclonedxDirectBom` y `verificar_sbom.sh` (FR-019).
- **Encontrado en el `cierre-ola.yml` de B00-H01:** usaba `vars.DOMINIO` y secretos sin declarar el entorno, buscaba `contratos/<servicio>.yaml` (la forma es `contratos/<servicio>/openapi.yaml`, ADR-015), esperaba `pruebas/k6/nomina_base.js` (no existe) y generaba el SBOM agregado que B00-H07 quitó.
- **Por qué un solo job:** levantar el sistema cuesta unos 8 minutos; repetirlo en tres jobs triplica el tiempo.

## 14. Lo que el autor tiene que hacer

| Paso | Dónde |
|---|---|
| Crear el repositorio privado en Docker Hub y dos tokens (escritura y solo lectura) | hub.docker.com |
| Crear los entornos `publicacion` y `demostracion`, el secreto `DOCKERHUB_TOKEN` y las variables `DOCKERHUB_USUARIO` y `DOCKERHUB_REPOSITORIO` | GitHub (quickstart §1) |
| Crear el token de grano fino de *deployments* | GitHub (quickstart §1) |
| Correr `deploy/equipo/preparar.sh` | El PC (quickstart §2) |
| Opcional: dominio en Cloudflare y token del túnel | Cloudflare (quickstart §5) |
