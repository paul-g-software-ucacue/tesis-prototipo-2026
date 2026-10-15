# Contrato: la SPA en el perfil consolidado (D-52)

## Servicio `frontend` en `compose.yaml`

| Clave | Valor |
|---|---|
| Imagen | `deploy/docker/frontend.Dockerfile`, contexto `.` (la raíz, porque necesita `contratos/`) |
| Puerto | `8081:8080` (el API Gateway sigue en `8080`) |
| Depende de | `gateway` sano |
| *Healthcheck* | `wget -q -O- http://127.0.0.1:8080/salud` |
| Memoria | 64 MiB |
| Variables | `MDD_URI_GATEWAY` (por defecto `http://gateway:8080`), para la plantilla de Nginx |

El perfil pasa a 7 contenedores, con 3 008 MiB de límites en total. `docker compose up -d --wait` sigue levantando todo con un solo comando.

## Nginx

| Ruta | Respuesta |
|---|---|
| `/api/…` | Se reenvía al API Gateway sin cambios, también la cabecera `Authorization` |
| `/salud` | 200, para el *healthcheck* |
| `/assets/…` | Archivo estático con `Cache-Control: public, max-age=31536000, immutable` |
| Cualquier otra | `index.html` (`try_files $uri /index.html`), con `Cache-Control: no-cache` |

Cabeceras en todas las respuestas: la `Content-Security-Policy` de research §10, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` y `Permissions-Policy: camera=(), microphone=(), geolocation=()`. Sin `Server` con la versión (`server_tokens off`).

## `deploy/probar_spa.sh`

**Uso:** `deploy/probar_spa.sh`, con el perfil levantado. Usa `MDD_SPA` (por defecto `http://localhost:8081`). Termina con 0 o 1 y no imprime el token.

| Paso | Petición | Comprueba |
|---|---|---|
| 1 | `GET /` | 200, HTML con `<html lang="es"`, y la `Content-Security-Policy` |
| 2 | `GET /una/ruta/de/la/spa` | 200 con el mismo `index.html` |
| 3 | `POST /api/usuarios/sesion` con `autor.prueba` | 200 con `token` (a través de la SPA y del API Gateway) |
| 4 | `GET /api/usuarios/sesion` con el token | 200 con `autor.prueba` y sus roles |
| 5 | `GET /api/usuarios/sesion` sin token | El 401 uniforme `SEGURIDAD_NO_AUTENTICADO` |

`perfil-consolidado` lo corre después de `probar_eventos.sh` y antes de la medición de R-19, que suma el contenedor `frontend`.
