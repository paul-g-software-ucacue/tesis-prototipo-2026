# Equipo de demostración del MDD

Lo que corre en el equipo que despliega el MDD solo, bajando las imágenes de Docker Hub (B00-H08,
D-55 a D-58). Cómo prepararlo y comprobarlo: [`specs/009-publicacion-despliegue/quickstart.md`](../../specs/009-publicacion-despliegue/quickstart.md).

| Archivo | Para qué |
|---|---|
| `preparar.sh` | Una vez por equipo, con `sudo`: usuario `mdd`, `/opt/mdd`, secretos generados en el equipo y el temporizador. |
| `actualizar.sh` | Cada 5 minutos (`mdd-actualizar.timer`): consulta `despliegue-main`, despliega e informa a GitHub Deployments. |
| `compose.demo.yml`, `probar_humo.sh` | Viajan en el paquete de despliegue de cada commit, con `deploy/desplegar.sh`, el `Caddyfile` y `01-esquemas.sh`. |
| `probar_equipo.sh`, `github_simulado.py` | La prueba del ciclo, sin Docker Hub ni GitHub; nunca toca el equipo real. |

Ningún secreto vive en el repositorio: todos se generan o se guardan en `/opt/mdd`, con permisos solo de su dueño.
