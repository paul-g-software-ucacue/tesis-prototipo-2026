# Quickstart: Publicación en Docker Hub y despliegue en el equipo de demostración (B00-H08)

Lo que garantiza cada pieza está en [contracts/](contracts/). Reemplaza la guía del VPS que se le pasó al autor el 2026-10-17: ya no hay VPS, ni llave SSH, ni puertos abiertos (D-55).

> **Ningún secreto pasa por el chat ni por el repositorio.** Los comandos que crean o guardan secretos se corren en la terminal del autor, no con `!` en Claude Code.

## 1. Una vez: Docker Hub y GitHub

**1.1 · Docker Hub.** En hub.docker.com:
- un repositorio **privado** `mdd`;
- en *Account settings → Personal access tokens*, dos tokens: `mdd-ci` (**Read & Write**) y `mdd-equipo` (**Read-only**). Se muestran una sola vez: guardarlos en el gestor de contraseñas.

**1.2 · Entornos de GitHub** (en la terminal del autor, con `gh` autenticado):
```bash
R=paul-g-software-ucacue/tesis-prototipo-2026
# publicacion: solo main, con el token de escritura de Docker Hub
gh api -X PUT repos/$R/environments/publicacion \
  -F 'deployment_branch_policy[protected_branches]=false' -F 'deployment_branch_policy[custom_branch_policies]=true'
gh api -X POST repos/$R/environments/publicacion/deployment-branch-policies -f name=main -f type=branch
gh variable set DOCKERHUB_USUARIO --env publicacion --body "<usuario-dockerhub>"
gh variable set DOCKERHUB_REPOSITORIO --env publicacion --body "<usuario-dockerhub>/mdd"
gh secret set DOCKERHUB_TOKEN --env publicacion        # pegar el token mdd-ci; no se muestra
# demostracion: sin secretos; ahí aparecen los despliegues del equipo (D-56)
gh api -X PUT repos/$R/environments/demostracion
```

**1.3 · Token de GitHub para el equipo** (D-56). En *Settings → Developer settings → Fine-grained tokens → Generate new token*:
- *Resource owner*: `paul-g-software-ucacue`; *Repository access*: solo `tesis-prototipo-2026`;
- *Repository permissions*: **Deployments: Read and write** (Metadata: Read se agrega solo);
- vencimiento: hasta el fin de la tesis.

Si la organización pide aprobar los tokens, aprobarlo como dueño.

## 2. Una vez: preparar el PC

Requisitos: Docker con Compose, `curl`, `jq`, `openssl` y systemd. En Fedora ya están; en Ubuntu, `sudo apt-get install -y jq`.

```bash
sudo deploy/equipo/preparar.sh
```

Pide, sin mostrarlos:
- la contraseña del acceso de la demo (elegir una larga y guardarla en el gestor);
- el usuario de Docker Hub y el token `mdd-equipo`;
- el token de GitHub de 1.3.

Genera las demás claves y la llave JWT, crea el usuario `mdd` y `/opt/mdd`, e instala y arranca `mdd-actualizar.timer`.

Comprobar:
```bash
systemctl status mdd-actualizar.timer             # active (waiting)
sudo ls -l /opt/mdd /opt/mdd/secretos             # .env y configuracion.env con -rw-------
```

## 3. Escenarios en la CI de la historia

| Comprobación | Dónde | Resultado esperado |
|---|---|---|
| Despliegue sano y vuelta atrás (escenarios 3 y 4) | Paso «Despliegue en un equipo simulado» de `perfil-consolidado`, en el pull request | Versión A sana con la prueba de humo en verde; versión B rota que vuelve a A; los informes `success` y `failure` llegan a la API simulada |
| Imagen vulnerable (escenario 2) | Pull request de demostración que cambia la base de una imagen a una versión con una alta o crítica corregida: corre `5 · imágenes` (`cd.yml`), que en un pull request solo construye y revisa | `5 · imágenes` falla y no se sube nada |
| Cierre de ola (US6) | `cierre-ola` con `workflow_dispatch` en la rama | ZAP sin hallazgos altos sobre la SPA y la API; k6 omitido con un aviso |

## 4. Después de fusionar: en el PC

1. `cd.yml` corre sobre `main` y publica las 7 etiquetas y `despliegue-main` (escenario 1). Comprobar en Docker Hub.
2. En menos de 15 minutos: `journalctl -u mdd-actualizar.service -n 50` dice «Sano en N s», y `https://mdd.localhost` (aceptando la CA interna de Caddy) pide usuario y contraseña (escenario 5).
3. En GitHub, *Deployments → demostracion* muestra el commit con `success` (D-56).
4. Iniciar sesión en la SPA con `autor.prueba` y la clave de `/opt/mdd/.env` (`sudo grep MDD_USUARIO_PRUEBA_CLAVE /opt/mdd/.env`); con el token del inicio de sesión, `docker restart mdd-identidad-1` y comprobar que el token sigue valiendo (SC-007).

## 5. Opcional: acceso desde fuera de la casa (túnel)

1. Un dominio administrado por Cloudflare (DNS gratis) y, en *Zero Trust → Networks → Tunnels*, un túnel con un nombre público, por ejemplo `mdd.<dominio>`, que apunte a `http://caddy:8081`.
2. Copiar el token del túnel a `/opt/mdd/.env` como `TUNEL_TOKEN=…` (con `sudoedit`).
3. **Solo cuando venzan o se retiren las excepciones de Caddy y cloudflared de `.grype.yaml`** (condición del autor del 2026-10-17). Con el próximo despliegue, o con `sudo -u mdd env RAIZ=/opt/mdd /opt/mdd/bin/actualizar.sh --forzar`, se levanta `cloudflared`. Comprobar que `https://mdd.<dominio>` pide usuario y contraseña.

## 6. Prueba del ciclo en local

```bash
deploy/equipo/probar_equipo.sh      # registro local y API de GitHub simulada; proyecto mdd-prueba, puerto 8443
```

Nunca toca el equipo real (`mdd`, puerto 443). Resultado esperado: los pasos 0 a 10 en `OK`.

## 7. Lo que no debe pasar

- Que `gh secret list` en cualquier entorno muestre algo distinto de `DOCKERHUB_TOKEN` (SC-008).
- Que un contenedor reciba claves que no son suyas (`docker exec mdd-datos-1 env` no muestra `DB_CLAVE_IDENTIDAD`).
- Que `/opt/mdd` quede legible por otros usuarios.
- Que una consulta sin cambios gaste una descarga de Docker Hub: solo `HEAD`.
