# Equipo de demostración del MDD

Lo que corre en el equipo que despliega el MDD solo, bajando las imágenes de Docker Hub (B00-H08,
D-55 a D-58). Cómo prepararlo y comprobarlo: [`specs/009-publicacion-despliegue/quickstart.md`](../../specs/009-publicacion-despliegue/quickstart.md).

| Archivo | Para qué |
|---|---|
| `preparar.sh` | Una vez por equipo, con `sudo`: usuario `mdd`, `/opt/mdd`, secretos generados en el equipo y el temporizador. |
| `actualizar.sh` | Cada 5 minutos (`mdd-actualizar.timer`): consulta `despliegue-main`, despliega e informa a GitHub Deployments. |
| `compose.demo.yml`, `probar_humo.sh` | Viajan en el paquete de despliegue de cada commit, con `deploy/desplegar.sh`, el `Caddyfile` y `01-esquemas.sh`. |
| `poblar_demo.sh` | Con `sudo`, sobre una demo vacía: las 10 cargas de prueba, la política de 2026-I, la carrera de `coordinacion.demo`, el distributivo y una actividad con 4 horas, para las comprobaciones a mano. No borra lo que crea. |
| `probar_equipo.sh`, `github_simulado.py` | La prueba del ciclo, sin Docker Hub ni GitHub; nunca toca el equipo real. |

Ningún secreto vive en el repositorio: todos se generan o se guardan en `/opt/mdd`, con permisos solo de su dueño.

## Los archivos del negocio

El informe sustentado se guarda en el volumen `mdd_archivos`, montado solo en `distributivo` (D-88). La base guarda su clave y su huella.

Si haces copias de seguridad, incluye ese volumen junto con la base: si se pierde, la base sigue, pero el informe no se puede descargar. El sistema todavía no hace copias por su cuenta.

## Rotar la llave de firma de los tokens

`identidad` firma las sesiones con `/opt/mdd/secretos/jwt.pem` (ADR-014). El API Gateway y los procesos validan las sesiones con las llaves públicas que guardan en caché, así que cambiar la llave exige reiniciarlos a todos (D-116). Si solo se reinicia `identidad`, las sesiones nuevas pueden rechazarse con «Sesión no válida».

```bash
sudo mv /opt/mdd/secretos/jwt.pem /opt/mdd/secretos/jwt.pem.anterior
sudo deploy/equipo/preparar.sh          # genera la llave nueva; no toca lo demás
# identidad con la llave nueva y, después, los que validan las sesiones con su caché de llaves
sudo docker restart mdd-identidad-1
sudo docker restart mdd-gateway-1 mdd-datos-1 mdd-distributivo-1 mdd-salida-1
```

Todas las sesiones abiertas terminan, y cada usuario vuelve a iniciar sesión. Si todo funciona, se puede borrar `jwt.pem.anterior`.
